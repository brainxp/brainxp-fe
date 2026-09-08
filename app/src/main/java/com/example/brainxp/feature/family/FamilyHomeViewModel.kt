package com.example.brainxp.feature.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.core.result.valueOrNull
import com.example.brainxp.data.prefs.AuthDataStore
import com.example.brainxp.data.repo.AlertRepository
import com.example.brainxp.data.repo.AuthRepository
import com.example.brainxp.data.repo.FamilyRepository
import com.example.brainxp.data.repo.RewardRepository
import com.example.brainxp.domain.model.AcademicLevel
import com.example.brainxp.domain.model.FamilyChild
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyHomeLoad(
    val home: FamilyHomeUiState = FamilyHomeUiState(),
    val loading: Boolean = true,
    val error: ApiError? = null,
    val signedOut: Boolean = false,
) {
    val empty: Boolean get() = !loading && error == null && home.children.isEmpty()
}

@HiltViewModel
class FamilyHomeViewModel
    @Inject
    constructor(
        private val family: FamilyRepository,
        private val rewards: RewardRepository,
        private val auth: AuthDataStore,
        private val account: AuthRepository,
        private val alerts: AlertRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(FamilyHomeLoad())
        val state: StateFlow<FamilyHomeLoad> = mutableState.asStateFlow()

        init {
            load()
        }

        fun retry() = load()

        fun signOut() {
            if (mutableState.value.signedOut) return
            viewModelScope.launch {
                account.signOut()
                mutableState.update { it.copy(signedOut = true) }
            }
        }

        fun claimOwnRules(level: AcademicLevel) {
            viewModelScope.launch {
                when (val created = family.createSelfSubject(level)) {
                    is AppResult.Success -> {
                        auth.saveSubject(created.value.childId)
                        load()
                    }

                    is AppResult.Failure -> {
                        mutableState.update { it.copy(error = created.error) }
                    }
                }
            }
        }

        private suspend fun ownSubjectId(): String? = auth.current().subjectId

        private fun load() {
            mutableState.update { it.copy(loading = true, error = null) }
            viewModelScope.launch {
                when (val listed = family.children()) {
                    is AppResult.Failure -> {
                        mutableState.update { it.copy(loading = false, error = listed.error) }
                    }

                    is AppResult.Success -> {
                        val own = listed.value.firstOrNull { child -> child.childId == ownSubjectId() }
                        val children = listed.value.filterNot { child -> child.childId == own?.childId }
                        val standings = withStandings(children)
                        val mine = own?.let { subject -> withStandings(listOf(subject)).firstOrNull() }
                        val raised = alerts.openAlerts().valueOrNull().orEmpty()
                        mutableState.update {
                            it.copy(
                                home =
                                    FamilyHomeUiState(
                                        children = standings,
                                        self = mine,
                                        alerts = raised,
                                    ),
                                loading = false,
                            )
                        }
                    }
                }
            }
        }

        private suspend fun withStandings(children: List<FamilyChild>): List<FamilyMember> =
            coroutineScope {
                children
                    .map { child ->
                        async {
                            val standing = rewards.standingOf(child.childId)
                            FamilyMember(
                                id = child.childId,
                                name = child.name,
                                level = child.level ?: AcademicLevel.SMP,
                                balanceSeconds = standing.valueOr { it.balanceSeconds },
                                streakDays = standing.valueOr { it.streakCurrent },
                                remainingCapSeconds =
                                    standing.valueOr {
                                        (it.dailyCapSeconds - it.spentTodaySeconds).coerceAtLeast(0)
                                    },
                            )
                        }
                    }.map { it.await() }
            }
    }

private inline fun <T> AppResult<T>.valueOr(pick: (T) -> Int): Int =
    when (this) {
        is AppResult.Success -> pick(value)
        is AppResult.Failure -> 0
    }

package com.example.brainxp.feature.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
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
    val refreshing: Boolean = false,
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

        fun refresh() = load(quietly = true)

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

        private fun load(quietly: Boolean = false) {
            mutableState.update { it.copy(loading = !quietly, refreshing = quietly, error = null) }
            viewModelScope.launch {
                when (val listed = family.children()) {
                    is AppResult.Failure -> finishLoad(AppResult.Failure(listed.error))
                    is AppResult.Success -> finishLoad(loadHome(listed.value))
                }
            }
        }

        private suspend fun loadHome(subjects: List<FamilyChild>): AppResult<FamilyHomeUiState> {
            val own = subjects.firstOrNull { child -> child.childId == ownSubjectId() }
            val children = subjects.filterNot { child -> child.childId == own?.childId }
            val selfResult = own?.let { withStandings(listOf(it)) } ?: AppResult.Success(emptyList())
            return when (val childMembers = withStandings(children)) {
                is AppResult.Failure -> {
                    AppResult.Failure(childMembers.error)
                }

                is AppResult.Success -> {
                    when (selfResult) {
                        is AppResult.Failure -> {
                            AppResult.Failure(selfResult.error)
                        }

                        is AppResult.Success -> {
                            when (val raised = alerts.openAlerts()) {
                                is AppResult.Failure -> {
                                    AppResult.Failure(raised.error)
                                }

                                is AppResult.Success -> {
                                    AppResult.Success(
                                        FamilyHomeUiState(
                                            children = childMembers.value,
                                            self = selfResult.value.firstOrNull(),
                                            alerts = raised.value,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        private fun finishLoad(result: AppResult<FamilyHomeUiState>) {
            mutableState.update { current ->
                when (result) {
                    is AppResult.Failure -> {
                        current.copy(loading = false, refreshing = false, error = result.error)
                    }

                    is AppResult.Success -> {
                        current.copy(home = result.value, loading = false, refreshing = false)
                    }
                }
            }
        }

        private suspend fun withStandings(children: List<FamilyChild>): AppResult<List<FamilyMember>> =
            coroutineScope {
                val loaded =
                    children
                        .map { child ->
                            async {
                                child to rewards.standingOf(child.childId)
                            }
                        }.map { it.await() }
                val failure = loaded.firstNotNullOfOrNull { (_, result) -> (result as? AppResult.Failure)?.error }
                if (failure != null) {
                    AppResult.Failure(failure)
                } else {
                    AppResult.Success(
                        loaded.mapNotNull { (child, result) ->
                            val standing = (result as? AppResult.Success)?.value ?: return@mapNotNull null
                            FamilyMember(
                                id = child.childId,
                                name = child.name,
                                level = child.level ?: AcademicLevel.SMP,
                                balanceSeconds = standing.balanceSeconds,
                                streakDays = standing.streakCurrent,
                                remainingCapSeconds =
                                    (standing.dailyCapSeconds - standing.spentTodaySeconds).coerceAtLeast(0),
                            )
                        },
                    )
                }
            }
    }

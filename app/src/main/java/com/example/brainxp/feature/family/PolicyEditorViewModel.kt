package com.example.brainxp.feature.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.repo.FamilyRepository
import com.example.brainxp.data.repo.PolicyRepository
import com.example.brainxp.data.repo.SubjectPolicy
import com.example.brainxp.domain.model.PolicyDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SECONDS_PER_MINUTE = 60

data class PolicyEditorLoad(
    val policy: PolicyUiState? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val notice: String? = null,
    val error: ApiError? = null,
)

@HiltViewModel
class PolicyEditorViewModel
    @Inject
    constructor(
        private val policies: PolicyRepository,
        private val family: FamilyRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(PolicyEditorLoad())
        val state: StateFlow<PolicyEditorLoad> = mutableState.asStateFlow()

        private var childId: String? = null

        fun load(
            subjectId: String,
            childName: String,
        ) {
            if (childId == subjectId) return
            childId = subjectId
            mutableState.update { it.copy(loading = true, error = null) }
            viewModelScope.launch { fetch(subjectId, childName) }
        }

        fun retry() {
            val subject = childId ?: return
            val name =
                mutableState.value.policy
                    ?.subjectName
                    .orEmpty()
            childId = null
            load(subject, name)
        }

        fun onEvent(event: PolicyEvent) {
            val current = mutableState.value.policy ?: return
            if (event == PolicyEvent.Save) {
                save(current)
                return
            }
            mutableState.update { it.copy(policy = current.stepped(event), saved = false) }
        }

        private suspend fun fetch(
            subjectId: String,
            childName: String,
        ) {
            val resolved =
                childName.ifBlank {
                    (family.children() as? AppResult.Success)
                        ?.value
                        ?.firstOrNull { child -> child.childId == subjectId }
                        ?.name
                        .orEmpty()
                }
            mutableState.update {
                when (val result = policies.policy(subjectId)) {
                    is AppResult.Success -> it.copy(policy = result.value.toUiState(resolved), loading = false)
                    is AppResult.Failure -> it.copy(loading = false, error = result.error)
                }
            }
        }

        private fun save(policy: PolicyUiState) {
            val subject = childId ?: return
            if (mutableState.value.saving) return
            mutableState.update { it.copy(saving = true, error = null, notice = null) }
            viewModelScope.launch {
                when (val result = policies.save(policy.toDraft(), subject)) {
                    is AppResult.Failure -> {
                        mutableState.update { it.copy(saving = false, error = result.error) }
                    }

                    is AppResult.Success -> {
                        val deferred = result.value.takeIf { !it.applied }
                        mutableState.update {
                            it.copy(saving = false, saved = true, notice = deferred?.message)
                        }
                        fetch(subject, policy.subjectName)
                    }
                }
            }
        }
    }

private fun PolicyUiState.toDraft(): PolicyDraft =
    PolicyDraft(
        questionsPerSession = questionsPerSession,
        essayCount = essayCount,
        baseRewardSeconds = baseRewardSeconds,
        dailyCapSeconds = dailyCapMinutes.map { it * SECONDS_PER_MINUTE },
        dailyGrantSeconds = dailyGrantMinutes.map { it * SECONDS_PER_MINUTE },
        idleDaysAllowed = idleDaysAllowed,
        dayResetHour = dayResetHour,
        uploadMethods = uploadMethods,
    )

private fun SubjectPolicy.toUiState(childName: String): PolicyUiState =
    PolicyUiState(
        subjectName = childName,
        questionsPerSession = questionsPerSession,
        essayCount = essayCount,
        baseRewardSeconds = baseRewardSeconds,
        uploadMethods = uploadMethods,
        dailyCapMinutes = dailyCapSeconds.map { it / SECONDS_PER_MINUTE },
        dailyGrantMinutes = dailyGrantSeconds.map { it / SECONDS_PER_MINUTE },
        idleDaysAllowed = idleDaysAllowed,
        dayResetHour = dayResetHour,
        apps = lockedApps.map { LockedAppEntry(packageName = it, label = it, locked = true) },
    )

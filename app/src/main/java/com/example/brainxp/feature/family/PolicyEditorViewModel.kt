package com.example.brainxp.feature.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.core.result.valueOrNull
import com.example.brainxp.data.repo.AppInventoryRepository
import com.example.brainxp.data.repo.FamilyRepository
import com.example.brainxp.data.repo.PolicyRepository
import com.example.brainxp.data.repo.SubjectPolicy
import com.example.brainxp.domain.model.DeviceApp
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
        private val inventory: AppInventoryRepository,
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
            when (event) {
                PolicyEvent.Save -> save(current)
                is PolicyEvent.ToggleApp -> toggleApp(current, event.packageName)
                else -> mutableState.update { it.copy(policy = current.stepped(event), saved = false) }
            }
        }

        private fun toggleApp(
            current: PolicyUiState,
            packageName: String,
        ) {
            val subject = childId ?: return
            val wanted =
                current.apps
                    .firstOrNull { it.packageName == packageName }
                    ?.locked
                    ?.not() ?: return
            mutableState.update { it.copy(policy = current.stepped(PolicyEvent.ToggleApp(packageName)), notice = null) }
            viewModelScope.launch {
                val result = policies.setAppLocked(packageName, wanted, subject)
                if (result is AppResult.Failure) {
                    mutableState.update { state ->
                        state.copy(
                            policy = state.policy?.stepped(PolicyEvent.ToggleApp(packageName)),
                            error = result.error,
                        )
                    }
                }
            }
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
            val result = policies.policy(subjectId)
            val onDevice = inventory.inventoryOf(subjectId).valueOrNull().orEmpty()
            mutableState.update {
                when (result) {
                    is AppResult.Success -> {
                        it.copy(policy = result.value.toUiState(resolved, onDevice), loading = false)
                    }

                    is AppResult.Failure -> {
                        it.copy(loading = false, error = result.error)
                    }
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

private fun SubjectPolicy.toUiState(
    childName: String,
    onDevice: List<DeviceApp>,
): PolicyUiState =
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
        apps = appEntriesOf(onDevice, lockedApps),
    )

internal fun appEntriesOf(
    onDevice: List<DeviceApp>,
    lockedPackages: List<String>,
): List<LockedAppEntry> {
    val locked = lockedPackages.toSet()
    val known = onDevice.map { app -> app.packageName }.toSet()
    val reported =
        onDevice.map { app ->
            LockedAppEntry(
                packageName = app.packageName,
                label = app.label,
                locked = app.locked || app.packageName in locked,
            )
        }
    val strays =
        locked
            .filterNot { it in known }
            .map { LockedAppEntry(packageName = it, label = it, locked = true) }

    return (reported + strays).sortedWith(compareByDescending<LockedAppEntry> { it.locked }.thenBy { it.label })
}

package com.example.brainxp.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.prefs.AuthDataStore
import com.example.brainxp.data.repo.AuthRepository
import com.example.brainxp.data.repo.PolicyChange
import com.example.brainxp.data.repo.PolicyRepository
import com.example.brainxp.data.repo.SubjectPolicy
import com.example.brainxp.data.repo.toDraft
import com.example.brainxp.domain.GuardedAction
import com.example.brainxp.domain.ParentLock
import com.example.brainxp.domain.ProtectionControl
import com.example.brainxp.domain.model.DeviceRole
import com.example.brainxp.domain.model.PolicyDraft
import com.example.brainxp.domain.protectionControlOf
import com.example.brainxp.feature.home.LockedApp
import com.example.brainxp.feature.home.LockedAppsSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val LANGUAGE_ID = "id"
const val LANGUAGE_EN = "en"

data class SettingsUiState(
    val policy: SubjectPolicy? = null,
    val draft: PolicyDraft? = null,
    val lockedApps: List<LockedApp> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val notice: String? = null,
    val error: ApiError? = null,
    val signedOut: Boolean = false,
    val role: DeviceRole = DeviceRole.PARENT,
    val ownRules: Boolean = false,
    val blocked: Boolean = false,
) {
    val rulesLocked: Boolean get() = role == DeviceRole.CHILD

    val protection: ProtectionControl get() = protectionControlOf(role, ownRules)

    val dirty: Boolean
        get() {
            val edited = draft ?: return false
            val saved = policy?.toDraft() ?: return false
            return edited != saved
        }

    val canSave: Boolean get() = dirty && !saving && !rulesLocked
}

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val policies: PolicyRepository,
        private val editor: PolicySettingsEditor,
        private val auth: AuthRepository,
        private val parentLock: ParentLock,
        private val identity: AuthDataStore,
        lockedApps: LockedAppsSource,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(SettingsUiState())
        val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

        init {
            retry()
            viewModelScope.launch {
                identity.auth.collect { snapshot ->
                    mutableState.update { it.copy(ownRules = !snapshot.subjectId.isNullOrBlank()) }
                }
            }
            viewModelScope.launch {
                parentLock.lock.collect { lock ->
                    mutableState.update { it.copy(role = lock.role) }
                }
            }
            viewModelScope.launch {
                lockedApps.observe().collect { locked ->
                    mutableState.update { it.copy(lockedApps = locked.apps) }
                }
            }
        }

        fun retry() {
            mutableState.update { it.copy(loading = true, error = null) }
            viewModelScope.launch { reload() }
        }

        fun edit(edit: SettingsPolicyEdit) {
            val now = mutableState.value
            if (now.rulesLocked) return

            when (edit) {
                is SettingsPolicyEdit.Level -> {
                    apply { editor.applyDirect(edit) }
                }

                is SettingsPolicyEdit.Language -> {
                    apply { editor.applyDirect(edit) }
                }

                is SettingsPolicyEdit.Draft -> {
                    val current = now.draft ?: return
                    mutableState.update { it.copy(draft = editor.applyToDraft(edit.change, current)) }
                }
            }
        }

        fun save() {
            val edited = mutableState.value.draft ?: return
            if (!mutableState.value.canSave) return
            apply { policies.save(edited) }
        }

        fun discard() {
            mutableState.update { it.copy(draft = it.policy?.toDraft(), notice = null, error = null) }
        }

        fun signOut() {
            viewModelScope.launch {
                if (!parentLock.allows(GuardedAction.SWITCH_MODE)) {
                    mutableState.update { it.copy(blocked = true) }
                    return@launch
                }
                auth.signOut()
                mutableState.update { it.copy(signedOut = true) }
            }
        }

        private fun apply(change: suspend () -> AppResult<PolicyChange>) {
            if (mutableState.value.saving) return
            mutableState.update { it.copy(saving = true, error = null, notice = null) }
            viewModelScope.launch {
                when (val result = change()) {
                    is AppResult.Success -> {
                        mutableState.update {
                            it.copy(saving = false, notice = result.value.message.takeIf { _ -> !result.value.applied })
                        }
                        reload()
                    }

                    is AppResult.Failure -> {
                        mutableState.update { it.copy(saving = false, error = result.error) }
                    }
                }
            }
        }

        private suspend fun reload() {
            val result = policies.policy()
            mutableState.update {
                when (result) {
                    is AppResult.Success -> {
                        it.copy(policy = result.value, draft = result.value.toDraft(), loading = false)
                    }

                    is AppResult.Failure -> {
                        it.copy(loading = false, error = result.error)
                    }
                }
            }
        }
    }

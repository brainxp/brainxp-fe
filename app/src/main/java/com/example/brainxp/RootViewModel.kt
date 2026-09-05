package com.example.brainxp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.data.prefs.SettingsDataStore
import com.example.brainxp.data.repo.RestrictionRepository
import com.example.brainxp.domain.GuardedAction
import com.example.brainxp.domain.ParentLock
import com.example.brainxp.domain.UnlockSessionManager
import com.example.brainxp.domain.model.DeviceRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RootUiState {
    data object Loading : RootUiState

    data object Onboarding : RootUiState

    data object Main : RootUiState
}

@HiltViewModel
class RootViewModel
    @Inject
    constructor(
        private val settings: SettingsDataStore,
        private val restrictions: RestrictionRepository,
        private val unlocks: UnlockSessionManager,
        private val parentLock: ParentLock,
    ) : ViewModel() {
        private val mutablePinRequired = MutableStateFlow<GuardedAction?>(null)
        val pinRequired: StateFlow<GuardedAction?> = mutablePinRequired.asStateFlow()

        private var pinVerified = false

        val role: StateFlow<DeviceRole?> =
            settings.settings
                .map { it.role }
                .stateIn(viewModelScope, SharingStarted.Eagerly, null)

        val state: StateFlow<RootUiState> =
            settings.settings
                .map { snapshot ->
                    if (snapshot.onboardingComplete) RootUiState.Main else RootUiState.Onboarding
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
                    initialValue = RootUiState.Loading,
                )

        fun markSetupComplete() {
            viewModelScope.launch { settings.setOnboardingComplete(true) }
        }

        fun resetSetup() {
            viewModelScope.launch {
                if (!parentLock.allows(GuardedAction.SWITCH_MODE, pinVerified = pinVerified)) {
                    mutablePinRequired.value = GuardedAction.SWITCH_MODE
                    return@launch
                }
                settings.setOnboardingComplete(false)
            }
        }

        fun toggleProtection() {
            viewModelScope.launch {
                val current = settings.settings.first().protectionEnabled
                if (current && !parentLock.allows(GuardedAction.DISABLE_PROTECTION, pinVerified = pinVerified)) {
                    mutablePinRequired.value = GuardedAction.DISABLE_PROTECTION
                    return@launch
                }
                settings.setProtectionEnabled(!current)
            }
        }

        fun submitPin(pin: String) {
            viewModelScope.launch {
                if (parentLock.verify(pin)) {
                    pinVerified = true
                    mutablePinRequired.value = null
                }
            }
        }

        fun dismissPin() {
            mutablePinRequired.value = null
        }

        fun grantDebugUnlock(durationSeconds: Int) {
            if (!BuildConfig.DEBUG) {
                return
            }
            viewModelScope.launch {
                val packages =
                    restrictions
                        .observeRestricted()
                        .first()
                        .filter { it.enabled }
                        .map { it.packageName }
                        .toSet()
                unlocks.start(durationSeconds.coerceAtLeast(1), packages)
            }
        }

        fun setWarningLead(seconds: Int) {
            if (!BuildConfig.DEBUG) {
                return
            }
            viewModelScope.launch { settings.setWarningLeadSeconds(seconds.coerceAtLeast(1)) }
        }

        fun endUnlock() {
            viewModelScope.launch { unlocks.endEarly() }
        }

        private companion object {
            const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
        }
    }

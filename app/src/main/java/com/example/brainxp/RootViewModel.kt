package com.example.brainxp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.data.prefs.AuthDataStore
import com.example.brainxp.data.prefs.SettingsDataStore
import com.example.brainxp.domain.model.DeviceRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
        private val auth: AuthDataStore,
    ) : ViewModel() {
        val role: StateFlow<DeviceRole?> =
            settings.settings
                .map { it.role }
                .stateIn(viewModelScope, SharingStarted.Eagerly, null)

        val state: StateFlow<RootUiState> =
            combine(settings.settings, auth.auth) { snapshot, session ->
                when {
                    !snapshot.onboardingComplete -> RootUiState.Onboarding
                    !session.isAuthenticated -> RootUiState.Onboarding
                    else -> RootUiState.Main
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
                initialValue = RootUiState.Loading,
            )

        fun markSetupComplete() {
            viewModelScope.launch { settings.setOnboardingComplete(true) }
        }

        private companion object {
            const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
        }
    }

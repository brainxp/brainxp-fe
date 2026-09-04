package com.example.brainxp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.data.prefs.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    ) : ViewModel() {
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
            viewModelScope.launch { settings.setOnboardingComplete(false) }
        }

        private companion object {
            const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
        }
    }

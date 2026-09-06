package com.example.brainxp.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.prefs.SettingsDataStore
import com.example.brainxp.data.repo.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignInUiState(
    val busy: Boolean = false,
    val error: ApiError? = null,
    val signedIn: Boolean = false,
)

@HiltViewModel
class SignInViewModel
    @Inject
    constructor(
        private val auth: AuthRepository,
        private val settings: SettingsDataStore,
    ) : ViewModel() {
        private val _state = MutableStateFlow(SignInUiState())
        val state: StateFlow<SignInUiState> = _state.asStateFlow()

        fun consumeSignIn() = _state.update { it.copy(signedIn = false) }

        fun submit(credentials: Credentials) {
            if (_state.value.busy) return
            _state.update { it.copy(busy = true, error = null) }
            viewModelScope.launch {
                val result =
                    if (credentials.register) {
                        auth.register(
                            email = credentials.email,
                            password = credentials.password,
                            displayName = credentials.displayName,
                            personal = !credentials.family,
                        )
                    } else {
                        auth.login(credentials.email, credentials.password)
                    }
                if (result is AppResult.Success) {
                    settings.setRole(result.value.role)
                }
                _state.update {
                    when (result) {
                        is AppResult.Success -> it.copy(busy = false, signedIn = true)
                        is AppResult.Failure -> it.copy(busy = false, error = result.error)
                    }
                }
            }
        }
    }

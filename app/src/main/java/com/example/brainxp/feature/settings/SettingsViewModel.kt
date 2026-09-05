package com.example.brainxp.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.repo.AuthRepository
import com.example.brainxp.data.repo.PolicyRepository
import com.example.brainxp.data.repo.SubjectPolicy
import com.example.brainxp.domain.model.AcademicLevel
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
    val loading: Boolean = true,
    val saving: Boolean = false,
    val notice: String? = null,
    val error: ApiError? = null,
    val signedOut: Boolean = false,
)

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val policies: PolicyRepository,
        private val auth: AuthRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(SettingsUiState())
        val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

        init {
            load()
        }

        fun retry() = load()

        fun dismissNotice() = mutableState.update { it.copy(notice = null) }

        fun chooseLevel(level: AcademicLevel) = apply { policies.setLevel(level) }

        fun chooseLanguage(language: String) = apply { policies.setLanguage(language) }

        fun chooseQuestionCount(count: Int) = apply { policies.setQuestionsPerSession(count) }

        fun signOut() {
            viewModelScope.launch {
                auth.signOut()
                mutableState.update { it.copy(signedOut = true) }
            }
        }

        private fun apply(change: suspend () -> AppResult<com.example.brainxp.data.repo.PolicyChange>) {
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

        private fun load() {
            mutableState.update { it.copy(loading = true, error = null) }
            viewModelScope.launch { reload() }
        }

        private suspend fun reload() {
            mutableState.update {
                when (val result = policies.policy()) {
                    is AppResult.Success -> it.copy(policy = result.value, loading = false)
                    is AppResult.Failure -> it.copy(loading = false, error = result.error)
                }
            }
        }
    }

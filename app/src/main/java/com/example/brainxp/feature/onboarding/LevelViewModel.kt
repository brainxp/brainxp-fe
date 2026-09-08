package com.example.brainxp.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.prefs.AuthDataStore
import com.example.brainxp.data.repo.FamilyRepository
import com.example.brainxp.data.repo.PolicyRepository
import com.example.brainxp.domain.model.AcademicLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LevelStep {
    CREATE_SUBJECT,
    UPDATE_LEVEL,
}

fun levelStepOf(subjectId: String?): LevelStep = if (subjectId.isNullOrBlank()) LevelStep.CREATE_SUBJECT else LevelStep.UPDATE_LEVEL

data class LevelUiState(
    val busy: Boolean = false,
    val error: ApiError? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class LevelViewModel
    @Inject
    constructor(
        private val policies: PolicyRepository,
        private val family: FamilyRepository,
        private val auth: AuthDataStore,
    ) : ViewModel() {
        private val _state = MutableStateFlow(LevelUiState())
        val state: StateFlow<LevelUiState> = _state.asStateFlow()

        fun consumeSaved() = _state.update { it.copy(saved = false) }

        fun submit(level: AcademicLevel) {
            if (_state.value.busy) return
            _state.update { it.copy(busy = true, error = null) }
            viewModelScope.launch {
                val outcome = runStep(level)
                _state.update {
                    when (outcome) {
                        is AppResult.Success<*> -> it.copy(busy = false, saved = true)
                        is AppResult.Failure -> it.copy(busy = false, error = outcome.error)
                    }
                }
            }
        }

        private suspend fun runStep(level: AcademicLevel): AppResult<*> =
            when (levelStepOf(auth.current().subjectId)) {
                LevelStep.CREATE_SUBJECT -> family.createSelfSubject(level)
                LevelStep.UPDATE_LEVEL -> policies.setLevel(level)
            }
    }

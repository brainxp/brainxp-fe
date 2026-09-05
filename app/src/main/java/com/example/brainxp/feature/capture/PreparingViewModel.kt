package com.example.brainxp.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.upload.MaterialPreparation
import com.example.brainxp.core.upload.PreparationState
import com.example.brainxp.domain.model.MaterialStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PreparingUiState(
    val materialName: String = "",
    val stage: PreparingStage = PreparingStage.READING,
    val readyQuestions: Int = 0,
    val materialId: String? = null,
    val rejected: Boolean = false,
    val error: ApiError? = null,
)

@HiltViewModel
class PreparingViewModel
    @Inject
    constructor(
        private val preparation: MaterialPreparation,
    ) : ViewModel() {
        val state: StateFlow<PreparingUiState> =
            preparation.state
                .map(::viewOf)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), PreparingUiState())

        fun done() = preparation.forget()

        private companion object {
            const val STOP_TIMEOUT = 5_000L
        }
    }

internal fun viewOf(state: PreparationState): PreparingUiState =
    when (state) {
        PreparationState.Idle -> {
            PreparingUiState()
        }

        is PreparationState.Working -> {
            PreparingUiState(
                stage = if (state.waitedMillis == 0L) PreparingStage.READING else PreparingStage.VALIDATING,
                materialId = state.materialId,
            )
        }

        is PreparationState.Settled -> {
            PreparingUiState(
                materialName = state.material.title,
                stage = if (state.material.status == MaterialStatus.READY) PreparingStage.READY else PreparingStage.READING,
                readyQuestions = state.material.questionCount,
                materialId = state.material.id,
                rejected = state.material.status == MaterialStatus.FAILED,
            )
        }

        is PreparationState.Stalled -> {
            PreparingUiState(
                stage = PreparingStage.VALIDATING,
                materialId = state.materialId,
                error = state.error,
            )
        }
    }

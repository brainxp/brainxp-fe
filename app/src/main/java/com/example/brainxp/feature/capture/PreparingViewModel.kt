package com.example.brainxp.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.upload.MaterialPreparation
import com.example.brainxp.core.upload.PreparationState
import com.example.brainxp.core.upload.PreparingStage
import com.example.brainxp.data.prefs.GuideStore
import com.example.brainxp.domain.model.MaterialStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PreparingUiState(
    val materialName: String = "",
    val stage: PreparingStage = PreparingStage.READING,
    val readyQuestions: Int = 0,
    val totalQuestions: Int = 0,
    val materialId: String? = null,
    val rejected: Boolean = false,
    val error: ApiError? = null,
    val guide: Boolean = false,
) {
    val ready: Boolean get() = readyQuestions > 0

    val done: Boolean get() = stage == PreparingStage.READY

    val progress: Float
        get() =
            when {
                done -> 1f
                totalQuestions > 0 -> (readyQuestions.toFloat() / totalQuestions).coerceIn(0f, 1f)
                else -> 0f
            }
}

@HiltViewModel
class PreparingViewModel
    @Inject
    constructor(
        private val preparation: MaterialPreparation,
        private val guides: GuideStore,
    ) : ViewModel() {
        private val guideOpen = MutableStateFlow(false)

        val state: StateFlow<PreparingUiState> =
            combine(preparation.state.map(::viewOf), guideOpen) { view, guide -> view.copy(guide = guide) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), PreparingUiState())

        init {
            viewModelScope.launch { guideOpen.value = !guides.quizGuideSeen() }
        }

        fun done() = preparation.forget()

        fun showGuide(visible: Boolean) {
            guideOpen.value = visible
            if (!visible) viewModelScope.launch { guides.rememberQuizGuide() }
        }

        fun retry() {
            state.value.materialId?.let { preparation.watch(it, state.value.materialName) }
        }

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
                materialName = state.name,
                stage = state.stage,
                readyQuestions = state.ready,
                totalQuestions = state.total,
                materialId = state.materialId,
            )
        }

        is PreparationState.Settled -> {
            PreparingUiState(
                materialName = state.material.title,
                stage = if (state.material.status == MaterialStatus.READY) PreparingStage.READY else PreparingStage.REJECTED,
                readyQuestions = state.material.questionCount,
                totalQuestions = state.material.questionCount,
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

package com.example.brainxp.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.repo.RewardRepository
import com.example.brainxp.domain.model.DayPoint
import com.example.brainxp.domain.model.Progress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

const val PROGRESS_WINDOW_DAYS = 7
const val MIN_ACTIVE_DAYS_FOR_CHART = 3

data class ProgressUiState(
    val phase: Phase = Phase.Loading,
    val progress: Progress? = null,
    val days: List<DayPoint> = emptyList(),
    val refreshing: Boolean = false,
) {
    val activeDays: Int get() = days.count { it.earnedSeconds > 0 || it.consumedSeconds > 0 }

    val chartWorthShowing: Boolean get() = activeDays >= MIN_ACTIVE_DAYS_FOR_CHART

    sealed interface Phase {
        data object Loading : Phase

        data object Ready : Phase

        data class Error(
            val error: ApiError,
            val retryable: Boolean,
        ) : Phase
    }
}

@HiltViewModel
class ProgressViewModel
    @Inject
    constructor(
        private val rewards: RewardRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(ProgressUiState())
        val state: StateFlow<ProgressUiState> = mutableState.asStateFlow()

        init {
            load()
        }

        fun retry() {
            load()
        }

        fun refresh() {
            mutableState.value = mutableState.value.copy(refreshing = true)
            load(quietly = true)
        }

        private fun load(quietly: Boolean = false) {
            if (!quietly) mutableState.value = ProgressUiState(phase = ProgressUiState.Phase.Loading)
            viewModelScope.launch {
                val progress = rewards.progress()
                val report = rewards.report(PROGRESS_WINDOW_DAYS)
                mutableState.value =
                    when (progress) {
                        is AppResult.Success -> {
                            ProgressUiState(
                                phase = ProgressUiState.Phase.Ready,
                                progress = progress.value,
                                days = (report as? AppResult.Success)?.value?.days.orEmpty(),
                            )
                        }

                        is AppResult.Failure -> {
                            ProgressUiState(
                                ProgressUiState.Phase.Error(progress.error, progress.error.retryable),
                            )
                        }
                    }
            }
        }
    }

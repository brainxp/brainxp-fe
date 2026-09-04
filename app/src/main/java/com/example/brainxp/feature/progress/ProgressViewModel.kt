package com.example.brainxp.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.repo.RewardRepository
import com.example.brainxp.domain.model.Progress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgressUiState(
    val phase: Phase = Phase.Loading,
    val progress: Progress? = null,
) {
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

        private fun load() {
            mutableState.value = ProgressUiState(phase = ProgressUiState.Phase.Loading)
            viewModelScope.launch {
                mutableState.value =
                    when (val result = rewards.progress()) {
                        is AppResult.Success -> {
                            ProgressUiState(ProgressUiState.Phase.Ready, result.value)
                        }

                        is AppResult.Failure -> {
                            ProgressUiState(
                                ProgressUiState.Phase.Error(result.error, result.error.retryable),
                            )
                        }
                    }
            }
        }
    }

package com.example.brainxp.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.repo.RewardRepository
import com.example.brainxp.domain.model.LedgerEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val entries: List<LedgerEntry> = emptyList(),
    val loading: Boolean = true,
    val error: ApiError? = null,
) {
    val empty: Boolean get() = !loading && error == null && entries.isEmpty()
}

@HiltViewModel
class HistoryViewModel
    @Inject
    constructor(
        private val rewards: RewardRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(HistoryUiState())
        val state: StateFlow<HistoryUiState> = mutableState.asStateFlow()

        init {
            load()
        }

        fun retry() = load()

        private fun load() {
            mutableState.update { it.copy(loading = true, error = null) }
            viewModelScope.launch {
                val result = rewards.history()
                mutableState.update {
                    when (result) {
                        is AppResult.Success -> it.copy(entries = result.value, loading = false)
                        is AppResult.Failure -> it.copy(loading = false, error = result.error)
                    }
                }
            }
        }
    }

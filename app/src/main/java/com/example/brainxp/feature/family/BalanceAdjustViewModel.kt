package com.example.brainxp.feature.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.repo.RewardRepository
import com.example.brainxp.domain.model.LedgerDirection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BalanceAdjustLoad(
    val applying: Boolean = false,
    val applied: Boolean = false,
    val error: ApiError? = null,
)

@HiltViewModel
class BalanceAdjustViewModel
    @Inject
    constructor(
        private val rewards: RewardRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(BalanceAdjustLoad())
        val state: StateFlow<BalanceAdjustLoad> = mutableState.asStateFlow()

        fun apply(
            childId: String,
            direction: AdjustDirection,
            seconds: Int,
            note: String,
        ) {
            if (mutableState.value.applying) return
            mutableState.update { it.copy(applying = true, error = null) }
            viewModelScope.launch {
                val result =
                    rewards.adjust(
                        direction = direction.toLedgerDirection(),
                        seconds = seconds,
                        note = note,
                        subjectId = childId,
                    )
                mutableState.update {
                    when (result) {
                        is AppResult.Success -> it.copy(applying = false, applied = true)
                        is AppResult.Failure -> it.copy(applying = false, error = result.error)
                    }
                }
            }
        }
    }

private fun AdjustDirection.toLedgerDirection(): LedgerDirection =
    when (this) {
        AdjustDirection.GRANT -> LedgerDirection.GRANT
        AdjustDirection.REDEEM -> LedgerDirection.REDEEM
    }

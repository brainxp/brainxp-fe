package com.example.brainxp.feature.home

import com.example.brainxp.blocking.ProtectionStatus
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.domain.model.UnlockState
import kotlin.time.Duration

data class HomeUiState(
    val phase: Phase = Phase.Loading,
    val rewardMinutes: Int = 0,
    val balanceStale: Boolean = false,
    val unlock: UnlockState = UnlockState.Locked,
    val remaining: Duration = Duration.ZERO,
    val protection: ProtectionStatus = ProtectionStatus.OFF,
) {
    sealed interface Phase {
        data object Loading : Phase

        data object Ready : Phase

        data class Error(
            val error: ApiError,
            val retryable: Boolean,
        ) : Phase
    }

    val unlockRunning: Boolean get() = unlock is UnlockState.Active && remaining > Duration.ZERO

    val degraded: Boolean get() = protection == ProtectionStatus.DEGRADED
}

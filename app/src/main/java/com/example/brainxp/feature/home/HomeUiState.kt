package com.example.brainxp.feature.home

import androidx.compose.ui.graphics.ImageBitmap
import com.example.brainxp.blocking.ProtectionStatus
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.domain.model.BlockReason
import com.example.brainxp.domain.model.UnlockState
import kotlin.time.Duration

data class LockedApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap? = null,
)

data class LockedApps(
    val apps: List<LockedApp> = emptyList(),
    val managed: Boolean = false,
)

data class PendingSession(
    val materialId: String,
    val title: String,
    val answered: Int,
    val total: Int,
)

data class HomeUiState(
    val phase: Phase = Phase.Loading,
    val balanceSeconds: Int = 0,
    val spentTodaySeconds: Int = 0,
    val dailyCapSeconds: Int = 0,
    val secondsUntilReset: Int = 0,
    val streakDays: Int = 0,
    val unreadNotifications: Int = 0,
    val blockReason: BlockReason = BlockReason.NONE,
    val balanceStale: Boolean = false,
    val unlock: UnlockState = UnlockState.Locked,
    val remaining: Duration = Duration.ZERO,
    val protection: ProtectionStatus = ProtectionStatus.OFF,
    val lockedApps: List<LockedApp> = emptyList(),
    val managed: Boolean = false,
    val displayName: String = "",
    val consumedSeconds: Int = 0,
    val idleDays: Int = 0,
    val idleDaysAllowed: Int = 0,
    val pending: PendingSession? = null,
    val preparing: PreparingRow? = null,
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

    val protectionOff: Boolean get() = protection == ProtectionStatus.OFF

    val capReached: Boolean get() = blockReason == BlockReason.DAILY_CAP

    val idleLocked: Boolean get() = blockReason == BlockReason.IDLE

    val guardianStale: Boolean get() = blockReason == BlockReason.GUARDIAN_STALE

    val appsOpen: Boolean get() = unlockRunning

    val spentFraction: Float
        get() = if (dailyCapSeconds <= 0) 0f else (spentTodaySeconds.toFloat() / dailyCapSeconds)
}

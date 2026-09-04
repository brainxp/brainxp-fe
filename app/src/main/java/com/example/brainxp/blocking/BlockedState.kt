package com.example.brainxp.blocking

import com.example.brainxp.domain.model.BlockReason
import com.example.brainxp.domain.model.Standing

enum class BlockedState {
    NO_BALANCE,
    NOT_STARTED,
    DAILY_CAP,
    IDLE_HOLD,
    GUARDIAN_STALE,
}

data class BlockedInfo(
    val state: BlockedState = BlockedState.NO_BALANCE,
    val balanceSeconds: Int = 0,
    val secondsUntilReset: Int = 0,
    val idleDays: Int = 0,
    val idleDaysAllowed: Int = 0,
)

fun blockedInfoOf(
    standing: Standing?,
    balanceSeconds: Int,
): BlockedInfo =
    BlockedInfo(
        state = blockedStateOf(standing?.blockReason ?: BlockReason.NONE, balanceSeconds),
        balanceSeconds = balanceSeconds,
        secondsUntilReset = standing?.secondsUntilReset ?: 0,
        idleDays = standing?.idleDays ?: 0,
        idleDaysAllowed = standing?.idleDaysAllowed ?: 0,
    )

fun blockedStateOf(
    reason: BlockReason,
    balanceSeconds: Int,
): BlockedState =
    when {
        reason == BlockReason.GUARDIAN_STALE -> BlockedState.GUARDIAN_STALE
        reason == BlockReason.IDLE -> BlockedState.IDLE_HOLD
        reason == BlockReason.DAILY_CAP -> BlockedState.DAILY_CAP
        balanceSeconds > 0 -> BlockedState.NOT_STARTED
        else -> BlockedState.NO_BALANCE
    }

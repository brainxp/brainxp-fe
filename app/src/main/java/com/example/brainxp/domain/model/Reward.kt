package com.example.brainxp.domain.model

enum class BlockReason {
    NONE,
    NO_BALANCE,
    DAILY_CAP,
    GUARDIAN_STALE,
}

data class Standing(
    val balanceSeconds: Int,
    val playableSeconds: Int,
    val ceilingSeconds: Int,
    val dailyCapSeconds: Int,
    val spentTodaySeconds: Int,
    val secondsUntilReset: Int,
    val blockReason: BlockReason,
    val streakCurrent: Int,
    val points: Int,
    val freezeTokens: Int,
) {
    val playable: Boolean get() = blockReason == BlockReason.NONE && playableSeconds > 0
}

data class ConsumptionEntry(
    val clientEventId: String,
    val appLabel: String,
    val seconds: Int,
    val occurredAtWallClock: Long?,
)

enum class LedgerDirection {
    GRANT,
    REDEEM,
}

data class LedgerEntry(
    val entryType: String,
    val deltaSeconds: Int,
    val occurredAt: String,
    val note: String?,
)

data class RestrictedApp(
    val packageName: String,
    val label: String,
    val enabled: Boolean,
)

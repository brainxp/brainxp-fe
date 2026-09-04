package com.example.brainxp.data.repo.fake

import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.repo.RewardRepository
import com.example.brainxp.domain.model.BlockReason
import com.example.brainxp.domain.model.ConsumptionEntry
import com.example.brainxp.domain.model.LedgerDirection
import com.example.brainxp.domain.model.LedgerEntry
import com.example.brainxp.domain.model.Progress
import com.example.brainxp.domain.model.Standing
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeRewardRepository
    @Inject
    constructor(
        private val backend: FakeBackend,
    ) : RewardRepository {
        private var balanceSeconds = STARTING_BALANCE_SECONDS
        private var spentTodaySeconds = 0
        private var streak = STARTING_STREAK
        private var points = 0
        private val entries = mutableListOf<LedgerEntry>()
        private val seenEventIds = ConcurrentHashMap.newKeySet<String>()

        var guardianStale: Boolean = false

        override suspend fun standing(): AppResult<Standing> = backend.respond(FakeBackend.REWARD_STANDING) { snapshot() }

        override suspend fun progress(): AppResult<Progress> = backend.respond(FakeBackend.REWARD_PROGRESS) { FakeData.progress(streak) }

        override suspend fun reportConsumption(entries: List<ConsumptionEntry>): AppResult<Standing> {
            val invalid = entries.firstOrNull { it.seconds < 0 }
            if (invalid != null) {
                return AppResult.Failure(ApiError.Validation("seconds", "tidak boleh negatif"))
            }

            return backend.respond(FakeBackend.LEDGER_SYNC) {
                entries.forEach { entry ->
                    if (seenEventIds.add(entry.clientEventId)) {
                        val applied = entry.seconds.coerceAtMost(balanceSeconds)
                        balanceSeconds -= applied
                        spentTodaySeconds += applied
                        this.entries +=
                            LedgerEntry(
                                entryType = "consumption",
                                deltaSeconds = -applied,
                                occurredAt = (entry.occurredAtWallClock ?: FakeData.now).toString(),
                                note = entry.appLabel,
                            )
                    }
                }
                snapshot()
            }
        }

        override suspend fun history(): AppResult<List<LedgerEntry>> = backend.respond(FakeBackend.LEDGER_HISTORY) { entries.toList() }

        override suspend fun adjust(
            direction: LedgerDirection,
            seconds: Int,
            note: String,
        ): AppResult<Standing> {
            if (seconds <= 0) {
                return AppResult.Failure(ApiError.Validation("seconds", "harus lebih dari nol"))
            }
            if (direction == LedgerDirection.REDEEM && seconds > balanceSeconds) {
                return AppResult.Failure(ApiError.Validation("seconds", "melebihi saldo tersedia"))
            }

            return backend.respond(FakeBackend.LEDGER_ADJUST) {
                val delta = if (direction == LedgerDirection.GRANT) seconds else -seconds
                balanceSeconds = (balanceSeconds + delta).coerceIn(0, CEILING_SECONDS)
                if (direction == LedgerDirection.GRANT) points += seconds / POINTS_DIVISOR
                entries +=
                    LedgerEntry(
                        entryType = direction.name.lowercase(),
                        deltaSeconds = delta,
                        occurredAt = FakeData.now.toString(),
                        note = note,
                    )
                snapshot()
            }
        }

        private fun snapshot(): Standing {
            val remainingCap = (DAILY_CAP_SECONDS - spentTodaySeconds).coerceAtLeast(0)
            val playable = minOf(balanceSeconds, remainingCap)
            return Standing(
                balanceSeconds = balanceSeconds,
                playableSeconds = playable,
                ceilingSeconds = CEILING_SECONDS,
                dailyCapSeconds = DAILY_CAP_SECONDS,
                spentTodaySeconds = spentTodaySeconds,
                secondsUntilReset = SECONDS_UNTIL_RESET,
                blockReason = blockReason(playable, remainingCap),
                streakCurrent = streak,
                points = points,
                freezeTokens = FREEZE_TOKENS,
                idleDays = IDLE_DAYS,
                idleDaysAllowed = IDLE_DAYS_ALLOWED,
            )
        }

        private fun blockReason(
            playable: Int,
            remainingCap: Int,
        ): BlockReason =
            when {
                guardianStale -> BlockReason.GUARDIAN_STALE
                remainingCap == 0 -> BlockReason.DAILY_CAP
                playable == 0 -> BlockReason.NO_BALANCE
                else -> BlockReason.NONE
            }

        private companion object {
            const val STARTING_BALANCE_SECONDS = 1_500
            const val CEILING_SECONDS = 7_200
            const val DAILY_CAP_SECONDS = 5_400
            const val SECONDS_UNTIL_RESET = 18_000
            const val STARTING_STREAK = 3
            const val FREEZE_TOKENS = 1
            const val IDLE_DAYS = 0
            const val IDLE_DAYS_ALLOWED = 2
            const val POINTS_DIVISOR = 60
        }
    }

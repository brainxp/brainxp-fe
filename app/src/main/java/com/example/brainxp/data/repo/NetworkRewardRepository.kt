package com.example.brainxp.data.repo

import com.example.brainxp.core.network.AdjustDto
import com.example.brainxp.core.network.BadgeDto
import com.example.brainxp.core.network.ConsumptionDto
import com.example.brainxp.core.network.DayPointDto
import com.example.brainxp.core.network.ErrorMapper
import com.example.brainxp.core.network.LedgerEntryDto
import com.example.brainxp.core.network.ProgressDto
import com.example.brainxp.core.network.ReportDto
import com.example.brainxp.core.network.RewardApi
import com.example.brainxp.core.network.StandingDto
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.core.result.map
import com.example.brainxp.data.prefs.AuthDataStore
import com.example.brainxp.domain.model.Badge
import com.example.brainxp.domain.model.BlockReason
import com.example.brainxp.domain.model.ConsumptionEntry
import com.example.brainxp.domain.model.DayPoint
import com.example.brainxp.domain.model.LedgerDirection
import com.example.brainxp.domain.model.LedgerEntry
import com.example.brainxp.domain.model.Progress
import com.example.brainxp.domain.model.Report
import com.example.brainxp.domain.model.Standing
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkRewardRepository
    @Inject
    constructor(
        private val api: RewardApi,
        private val auth: AuthDataStore,
        private val errors: ErrorMapper,
    ) : RewardRepository {
        override suspend fun standing(): AppResult<Standing> = withSubject { api.standing(it).toStanding() }

        override suspend fun progress(): AppResult<Progress> = withSubject { api.progress(it).toProgress() }

        override suspend fun history(): AppResult<List<LedgerEntry>> = withSubject { api.ledger(it).map(LedgerEntryDto::toEntry) }

        override suspend fun report(
            days: Int,
            subjectId: String?,
        ): AppResult<Report> = withSubject(subjectId) { api.report(it, days).toReport() }

        override suspend fun standingOf(subjectId: String): AppResult<Standing> = withSubject(subjectId) { api.standing(it).toStanding() }

        override suspend fun reportConsumption(entries: List<ConsumptionEntry>): AppResult<Standing> =
            withSubject { subject ->
                api.sync(subject, entries.map(ConsumptionEntry::toDto)).toStanding()
            }

        override suspend fun adjust(
            direction: LedgerDirection,
            seconds: Int,
            note: String,
            subjectId: String?,
        ): AppResult<Standing> =
            withSubject(subjectId) { subject ->
                api
                    .adjust(subject, AdjustDto(direction = direction.name.lowercase(), seconds = seconds, note = note))
                    .toStanding()
            }

        private suspend fun <T> withSubject(
            explicit: String? = null,
            block: suspend (String) -> T,
        ): AppResult<T> {
            val subject = explicit ?: auth.current().subjectId ?: return AppResult.Failure(ApiError.Unauthorized)
            return runCatching { block(subject) }
                .fold(
                    onSuccess = { AppResult.Success(it) },
                    onFailure = { AppResult.Failure(errors.map(it)) },
                )
        }
    }

private fun StandingDto.toStanding(): Standing =
    Standing(
        balanceSeconds = balanceSeconds,
        playableSeconds = playableSeconds,
        dailyCapSeconds = dailyCapSeconds,
        spentTodaySeconds = spentTodaySeconds,
        secondsUntilReset = secondsUntilReset,
        blockReason = blockReasonOf(blockReason),
        streakCurrent = streakCurrent,
        freezeTokens = freezeTokens,
        idleDays = idleDays,
        idleDaysAllowed = idleDaysAllowed,
    )

private fun blockReasonOf(raw: String): BlockReason =
    BlockReason.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: BlockReason.NONE

private fun ProgressDto.toProgress(): Progress =
    Progress(
        streakCurrent = streakCurrent,
        streakLongest = streakLongest,
        sessions = sessions,
        correctTotal = correctTotal,
        essayPassed = essayPassed,
        freezeTokens = freezeTokens,
        badges = badges.map(BadgeDto::toBadge),
    )

private fun BadgeDto.toBadge(): Badge =
    Badge(
        code = code,
        name = name,
        hint = hint,
        earned = earned,
    )

private fun LedgerEntryDto.toEntry(): LedgerEntry =
    LedgerEntry(
        entryType = entryType,
        deltaSeconds = deltaSeconds,
        occurredAt = occurredAt,
        note = note,
    )

private fun DayPointDto.toPoint(): DayPoint =
    DayPoint(
        day = day,
        earnedSeconds = earnedSeconds,
        consumedSeconds = consumedSeconds,
    )

private fun ReportDto.toReport(): Report =
    Report(
        standing = standing.toStanding(),
        days = days.map(DayPointDto::toPoint),
        materialsStudied = materialsStudied,
        correctTotal = correctTotal,
        essayPassed = essayPassed,
        recent = recent.map(LedgerEntryDto::toEntry),
        guardianAlerts = guardianAlerts,
    )

private fun ConsumptionEntry.toDto(): ConsumptionDto =
    ConsumptionDto(
        clientEventId = clientEventId,
        appLabel = appLabel,
        seconds = seconds,
        occurredAt = occurredAtWallClock?.let { Instant.ofEpochMilli(it).toString() },
    )

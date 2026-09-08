package com.example.brainxp.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class StandingDto(
    @SerialName("balance_seconds") val balanceSeconds: Int,
    @SerialName("playable_seconds") val playableSeconds: Int,
    @SerialName("daily_cap_seconds") val dailyCapSeconds: Int,
    @SerialName("spent_today_seconds") val spentTodaySeconds: Int,
    @SerialName("seconds_until_reset") val secondsUntilReset: Int,
    @SerialName("block_reason") val blockReason: String,
    @SerialName("streak_current") val streakCurrent: Int,
    @SerialName("freeze_tokens") val freezeTokens: Int,
    @SerialName("idle_days") val idleDays: Int = 0,
    @SerialName("idle_days_allowed") val idleDaysAllowed: Int = 0,
)

@Serializable
data class BadgeDto(
    val code: String,
    val name: String,
    val hint: String,
    val earned: Boolean,
    @SerialName("earned_at") val earnedAt: String? = null,
)

@Serializable
data class ProgressDto(
    @SerialName("streak_current") val streakCurrent: Int,
    @SerialName("streak_longest") val streakLongest: Int,
    val sessions: Int,
    @SerialName("correct_total") val correctTotal: Int,
    @SerialName("essay_passed") val essayPassed: Int,
    @SerialName("freeze_tokens") val freezeTokens: Int,
    val badges: List<BadgeDto> = emptyList(),
)

@Serializable
data class LedgerEntryDto(
    @SerialName("entry_type") val entryType: String,
    @SerialName("delta_seconds") val deltaSeconds: Int,
    @SerialName("occurred_at") val occurredAt: String,
    val note: String? = null,
)

@Serializable
data class DayPointDto(
    val day: String,
    @SerialName("earned_seconds") val earnedSeconds: Int,
    @SerialName("consumed_seconds") val consumedSeconds: Int,
)

@Serializable
data class ReportDto(
    val standing: StandingDto,
    val days: List<DayPointDto> = emptyList(),
    @SerialName("materials_studied") val materialsStudied: Int,
    @SerialName("correct_total") val correctTotal: Int,
    @SerialName("essay_passed") val essayPassed: Int,
    val recent: List<LedgerEntryDto> = emptyList(),
    @SerialName("guardian_alerts") val guardianAlerts: List<String> = emptyList(),
)

@Serializable
data class ConsumptionDto(
    @SerialName("client_event_id") val clientEventId: String,
    @SerialName("app_label") val appLabel: String,
    val seconds: Int,
    @SerialName("occurred_at") val occurredAt: String? = null,
)

@Serializable
data class AdjustDto(
    val direction: String,
    val seconds: Int,
    val note: String,
)

interface RewardApi {
    @GET("subjects/{subjectId}/standing")
    suspend fun standing(
        @Path("subjectId") subjectId: String,
    ): StandingDto

    @GET("subjects/{subjectId}/progress")
    suspend fun progress(
        @Path("subjectId") subjectId: String,
    ): ProgressDto

    @GET("subjects/{subjectId}/ledger")
    suspend fun ledger(
        @Path("subjectId") subjectId: String,
    ): List<LedgerEntryDto>

    @GET("subjects/{subjectId}/report")
    suspend fun report(
        @Path("subjectId") subjectId: String,
        @Query("days") days: Int,
    ): ReportDto

    @POST("subjects/{subjectId}/ledger/sync")
    suspend fun sync(
        @Path("subjectId") subjectId: String,
        @Body body: List<ConsumptionDto>,
    ): StandingDto

    @POST("subjects/{subjectId}/ledger/adjust")
    suspend fun adjust(
        @Path("subjectId") subjectId: String,
        @Body body: AdjustDto,
    ): StandingDto
}

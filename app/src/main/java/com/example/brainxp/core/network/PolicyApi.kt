package com.example.brainxp.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

@Serializable
data class PolicyPatchDto(
    @SerialName("academic_level") val academicLevel: String? = null,
    @SerialName("question_language") val questionLanguage: String? = null,
    @SerialName("questions_per_session") val questionsPerSession: Int? = null,
    @SerialName("daily_caps") val dailyCaps: List<Int>? = null,
)

@Serializable
data class PolicyChangeDto(
    val applied: Boolean = false,
    @SerialName("pending_until") val pendingUntil: String? = null,
    val message: String? = null,
)

@Serializable
data class PolicyDto(
    @SerialName("subject_id") val subjectId: String,
    @SerialName("academic_level") val academicLevel: String? = null,
    @SerialName("question_language") val questionLanguage: String? = null,
    @SerialName("questions_per_session") val questionsPerSession: Int = 0,
    @SerialName("day_reset_hour") val dayResetHour: Int = 0,
    @SerialName("idle_days_allowed") val idleDaysAllowed: Int = 0,
    @SerialName("pending_weaken_at") val pendingWeakenAt: String? = null,
    @SerialName("essay_ratio") val essayRatio: Double = 0.0,
    @SerialName("daily_caps") val dailyCaps: List<Int> = emptyList(),
    @SerialName("daily_grants") val dailyGrants: List<Int> = emptyList(),
    @SerialName("locked_apps") val lockedApps: List<String> = emptyList(),
)

interface PolicyApi {
    @GET("policies/{subjectId}")
    suspend fun policy(
        @Path("subjectId") subjectId: String,
    ): PolicyDto

    @PUT("policies/{subjectId}")
    suspend fun update(
        @Path("subjectId") subjectId: String,
        @Body body: PolicyPatchDto,
    ): PolicyChangeDto
}

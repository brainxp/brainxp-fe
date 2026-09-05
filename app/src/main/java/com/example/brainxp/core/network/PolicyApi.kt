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

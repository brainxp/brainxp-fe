package com.example.brainxp.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Streaming

@Serializable
data class MaterialAcceptedDto(
    @SerialName("material_id") val materialId: String,
    val status: String,
    @SerialName("duplicate_of") val duplicateOf: String? = null,
)

@Serializable
data class MaterialDto(
    val id: String,
    val status: String,
    @SerialName("original_name") val originalName: String? = null,
    @SerialName("source_type") val sourceType: String? = null,
    @SerialName("page_count") val pageCount: Int? = null,
    @SerialName("assessed_level") val assessedLevel: String? = null,
    @SerialName("declared_level") val declaredLevel: String? = null,
    @SerialName("gate_verdict") val gateVerdict: String? = null,
    @SerialName("gate_reason") val gateReason: String? = null,
    @SerialName("topic_summary") val topicSummary: String? = null,
    @SerialName("times_studied") val timesStudied: Int = 0,
    @SerialName("question_count") val questionCount: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    val unfinished: UnfinishedDto? = null,
)

@Serializable
data class UnfinishedDto(
    @SerialName("session_id") val sessionId: String,
    val answered: Int,
    val total: Int,
)

@Serializable
data class StageEventDto(
    val stage: String,
    val ready: Int? = null,
    val total: Int? = null,
    val status: String? = null,
    val reason: String? = null,
)

interface MaterialApi {
    @Multipart
    @POST("subjects/{subjectId}/materials")
    suspend fun upload(
        @Path("subjectId") subjectId: String,
        @Part file: MultipartBody.Part,
        @Part("method") method: RequestBody,
    ): MaterialAcceptedDto

    @GET("subjects/{subjectId}/library")
    suspend fun library(
        @Path("subjectId") subjectId: String,
    ): List<MaterialDto>

    @DELETE("materials/{materialId}")
    suspend fun delete(
        @Path("materialId") materialId: String,
    )

    @GET("materials/{materialId}")
    suspend fun material(
        @Path("materialId") materialId: String,
    ): MaterialDto

    @Streaming
    @GET("materials/{materialId}/stream")
    suspend fun stream(
        @Path("materialId") materialId: String,
    ): ResponseBody
}

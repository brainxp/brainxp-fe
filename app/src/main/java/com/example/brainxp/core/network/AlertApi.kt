package com.example.brainxp.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class AlertDto(
    val id: Int,
    @SerialName("subject_id") val subjectId: String,
    @SerialName("subject_name") val subjectName: String,
    val kind: String,
    @SerialName("created_at") val createdAt: String,
    val detail: String? = null,
    @SerialName("acknowledged_at") val acknowledgedAt: String? = null,
    @SerialName("resolved_at") val resolvedAt: String? = null,
)

interface AlertApi {
    @GET("alerts")
    suspend fun alerts(
        @Query("status") status: String,
    ): List<AlertDto>
}

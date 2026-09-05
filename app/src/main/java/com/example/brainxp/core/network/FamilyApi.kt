package com.example.brainxp.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

const val PLATFORM_ANDROID = "android"

@Serializable
data class ChildRequestDto(
    @SerialName("display_name") val displayName: String,
    @SerialName("academic_level") val academicLevel: String,
    @SerialName("question_language") val questionLanguage: String,
)

@Serializable
data class SubjectDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("academic_level") val academicLevel: String,
    val kind: String,
)

@Serializable
data class PairingCodeDto(
    val code: String,
    @SerialName("subject_id") val subjectId: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("attempts_allowed") val attemptsAllowed: Int,
)

@Serializable
data class PairRequestDto(
    val code: String,
    @SerialName("install_binding") val installBinding: String,
    @SerialName("model_name") val modelName: String? = null,
    val platform: String = PLATFORM_ANDROID,
)

@Serializable
data class BindingCheckRequestDto(
    @SerialName("install_binding") val installBinding: String,
)

@Serializable
data class BindingCheckDto(
    val bound: Boolean,
    @SerialName("family_mode") val familyMode: Boolean = false,
    @SerialName("subject_name") val subjectName: String? = null,
)

@Serializable
data class HeartbeatRequestDto(
    @SerialName("guardian_status") val guardianStatus: String,
)

interface FamilyApi {
    @GET("subjects")
    suspend fun children(): List<SubjectDto>

    @POST("subjects")
    suspend fun createChild(
        @Body body: ChildRequestDto,
    ): SubjectDto

    @POST("subjects/{subjectId}/pairing-code")
    suspend fun pairingCode(
        @Path("subjectId") subjectId: String,
    ): PairingCodeDto

    @POST("devices/pair")
    suspend fun pair(
        @Body body: PairRequestDto,
    ): TokenDto

    @POST("devices/check-binding")
    suspend fun checkBinding(
        @Body body: BindingCheckRequestDto,
    ): BindingCheckDto

    @POST("devices/heartbeat")
    suspend fun heartbeat(
        @Body body: HeartbeatRequestDto,
    )
}

package com.example.brainxp.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
    @SerialName("display_name") val displayName: String,
    val mode: String,
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

@Serializable
data class RefreshRequestDto(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class TokenDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    val role: String,
    @SerialName("subject_id") val subjectId: String? = null,
    @SerialName("family_id") val familyId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("device_secret") val deviceSecret: String? = null,
)

interface AuthApi {
    @POST("auth/register")
    suspend fun register(
        @Body body: RegisterRequestDto,
    ): TokenDto

    @POST("auth/refresh")
    suspend fun refresh(
        @Body body: RefreshRequestDto,
    ): TokenDto

    @POST("auth/login")
    suspend fun login(
        @Body body: LoginRequestDto,
    ): TokenDto
}

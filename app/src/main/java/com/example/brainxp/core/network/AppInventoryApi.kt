package com.example.brainxp.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

@Serializable
data class InstalledAppDto(
    val `package`: String,
    val label: String,
    @SerialName("is_system") val isSystem: Boolean = false,
    @SerialName("version_name") val versionName: String? = null,
)

@Serializable
data class AppInventoryRequestDto(
    val apps: List<InstalledAppDto>,
)

@Serializable
data class AppSyncDto(
    @SerialName("first_sync") val firstSync: Boolean = false,
    val changed: Boolean = false,
    val present: Int = 0,
)

@Serializable
data class InventoryAppDto(
    val `package`: String,
    val label: String,
    val locked: Boolean = false,
    @SerialName("is_system") val isSystem: Boolean = false,
    @SerialName("is_new") val isNew: Boolean = false,
)

@Serializable
data class AppInventoryDto(
    val apps: List<InventoryAppDto> = emptyList(),
    val total: Int = 0,
    @SerialName("locked_count") val lockedCount: Int = 0,
    @SerialName("synced_at") val syncedAt: String? = null,
)

interface AppInventoryApi {
    @PUT("devices/apps")
    suspend fun sync(
        @Body body: AppInventoryRequestDto,
    ): AppSyncDto

    @GET("subjects/{subjectId}/apps")
    suspend fun inventory(
        @Path("subjectId") subjectId: String,
    ): AppInventoryDto
}

package com.example.brainxp.data.repo

import com.example.brainxp.core.network.AppInventoryApi
import com.example.brainxp.core.network.AppInventoryRequestDto
import com.example.brainxp.core.network.ErrorMapper
import com.example.brainxp.core.network.InstalledAppDto
import com.example.brainxp.core.network.InventoryAppDto
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.core.result.map
import com.example.brainxp.domain.model.DeviceApp
import javax.inject.Inject
import javax.inject.Singleton

interface AppInventoryRepository {
    suspend fun publish(apps: List<DeviceApp>): AppResult<Unit>

    suspend fun inventoryOf(subjectId: String): AppResult<List<DeviceApp>>
}

@Singleton
class NetworkAppInventoryRepository
    @Inject
    constructor(
        private val api: AppInventoryApi,
        private val errors: ErrorMapper,
    ) : AppInventoryRepository {
        override suspend fun publish(apps: List<DeviceApp>): AppResult<Unit> {
            if (apps.isEmpty()) return AppResult.Success(Unit)
            return call {
                api.sync(AppInventoryRequestDto(apps = apps.take(SYNC_LIMIT).map(DeviceApp::toDto)))
            }.map { }
        }

        override suspend fun inventoryOf(subjectId: String): AppResult<List<DeviceApp>> =
            call { api.inventory(subjectId) }.map { page -> page.apps.map(InventoryAppDto::toApp) }

        private suspend fun <T> call(block: suspend () -> T): AppResult<T> =
            runCatching { block() }
                .fold(
                    onSuccess = { AppResult.Success(it) },
                    onFailure = { AppResult.Failure(errors.map(it)) },
                )

        private companion object {
            const val SYNC_LIMIT = 1_000
        }
    }

private fun DeviceApp.toDto(): InstalledAppDto =
    InstalledAppDto(
        `package` = packageName,
        label = label,
        isSystem = system,
    )

private fun InventoryAppDto.toApp(): DeviceApp =
    DeviceApp(
        packageName = `package`,
        label = label,
        locked = locked,
        system = isSystem,
        fresh = isNew,
    )

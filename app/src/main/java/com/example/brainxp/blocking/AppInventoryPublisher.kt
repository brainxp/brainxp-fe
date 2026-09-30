package com.example.brainxp.blocking

import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.repo.AppInventoryRepository
import com.example.brainxp.domain.model.DeviceApp
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInventoryPublisher
    @Inject
    constructor(
        private val source: InstalledAppsSource,
        private val inventory: AppInventoryRepository,
    ) {
        private val mutex = Mutex()
        private var publishedSignature: List<Pair<String, String>>? = null

        suspend fun publish(): AppResult<Unit> =
            mutex.withLock {
                val selectable = SystemCriticalFilter.selectable(source.launchableApps(), source.protectedPackages())
                val signature = selectable.map { it.packageName to it.label }
                if (signature == publishedSignature) return@withLock AppResult.Success(Unit)
                val result = inventory.publish(selectable.map(InstalledApp::asDeviceApp))
                if (result is AppResult.Success) publishedSignature = signature
                result
            }
    }

private fun InstalledApp.asDeviceApp(): DeviceApp =
    DeviceApp(
        packageName = packageName,
        label = label,
    )

package com.example.brainxp.blocking

import com.example.brainxp.data.repo.AppInventoryRepository
import com.example.brainxp.domain.model.DeviceApp
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInventoryPublisher
    @Inject
    constructor(
        private val source: InstalledAppsSource,
        private val inventory: AppInventoryRepository,
    ) {
        suspend fun publish() {
            val selectable = SystemCriticalFilter.selectable(source.launchableApps(), source.protectedPackages())
            inventory.publish(selectable.map(InstalledApp::asDeviceApp))
        }
    }

private fun InstalledApp.asDeviceApp(): DeviceApp =
    DeviceApp(
        packageName = packageName,
        label = label,
    )

package com.example.brainxp.feature.home

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.brainxp.blocking.InstalledAppsSource
import com.example.brainxp.data.repo.RestrictionRepository
import com.example.brainxp.domain.ParentLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockedAppsSource
    @Inject
    constructor(
        private val restrictions: RestrictionRepository,
        private val installed: InstalledAppsSource,
        private val parentLock: ParentLock,
    ) {
        fun observe(): Flow<LockedApps> =
            flow {
                val labels = installed.launchableApps().associate { it.packageName to it.label }
                val icons = mutableMapOf<String, ImageBitmap?>()
                val named =
                    combine(restrictions.observeRestricted(), parentLock.lock) { apps, lock ->
                        val enabled = apps.filter { it.enabled }
                        enabled.forEach { app ->
                            icons.getOrPut(app.packageName) {
                                installed.icon(app.packageName)?.asImageBitmap()
                            }
                        }
                        LockedApps(
                            apps =
                                enabled.map { app ->
                                    LockedApp(
                                        packageName = app.packageName,
                                        label = labels[app.packageName] ?: app.packageName,
                                        icon = icons[app.packageName],
                                    )
                                },
                            managed = lock.locked,
                        )
                    }
                emitAll(named)
            }
    }

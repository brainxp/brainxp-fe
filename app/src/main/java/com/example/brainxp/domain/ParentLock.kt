package com.example.brainxp.domain

import com.example.brainxp.data.prefs.SettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParentLock
    @Inject
    constructor(
        private val settings: SettingsDataStore,
    ) {
        val lock: Flow<ChildDeviceLock> =
            settings.settings.map { snapshot -> ChildDeviceLock(role = snapshot.role) }

        suspend fun allows(action: GuardedAction): Boolean = ParentGate.allows(lock.first(), action)
    }

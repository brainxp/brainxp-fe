package com.example.brainxp.domain

import com.example.brainxp.data.prefs.ParentPinStore
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
        private val pins: ParentPinStore,
    ) {
        val lock: Flow<ChildDeviceLock> =
            settings.settings.map { snapshot ->
                ChildDeviceLock(role = snapshot.role, pinSet = snapshot.parentPinHash != null)
            }

        suspend fun requiresPin(action: GuardedAction): Boolean = ParentGate.requiresPin(lock.first(), action)

        suspend fun allows(
            action: GuardedAction,
            pinVerified: Boolean,
        ): Boolean = ParentGate.allows(lock.first(), action, pinVerified)

        suspend fun verify(pin: String): Boolean = pins.matches(pin)
    }

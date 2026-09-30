package com.example.brainxp.domain

import com.example.brainxp.data.prefs.AuthDataStore
import com.example.brainxp.data.prefs.SettingsDataStore
import com.example.brainxp.domain.model.familyParent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParentLock
    @Inject
    constructor(
        private val settings: SettingsDataStore,
        private val auth: AuthDataStore,
    ) {
        val lock: Flow<ChildDeviceLock> =
            settings.settings.map { snapshot -> ChildDeviceLock(role = snapshot.role) }

        val familyParent: Flow<Boolean> =
            combine(settings.settings, auth.auth) { snapshot, session ->
                familyParent(snapshot.role, session.familyId, session.role)
            }

        suspend fun allows(action: GuardedAction): Boolean = ParentGate.allows(lock.first(), action)
    }

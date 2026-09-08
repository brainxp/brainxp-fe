package com.example.brainxp.data.repo

import com.example.brainxp.data.prefs.AuthDataStore
import com.example.brainxp.data.prefs.SettingsDataStore
import com.example.brainxp.domain.model.familyParent
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParentDeviceCheck
    @Inject
    constructor(
        private val settings: SettingsDataStore,
        private val auth: AuthDataStore,
    ) {
        suspend fun watchesAFamily(): Boolean {
            val session = auth.current()
            if (!session.isAuthenticated) return false
            return familyParent(settings.settings.first().role, session.familyId, session.role)
        }
    }

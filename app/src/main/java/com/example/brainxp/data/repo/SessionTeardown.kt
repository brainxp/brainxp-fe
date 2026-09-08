package com.example.brainxp.data.repo

import com.example.brainxp.data.prefs.AppMode
import com.example.brainxp.data.prefs.AuthDataStore
import com.example.brainxp.data.prefs.SettingsDataStore
import com.example.brainxp.domain.model.DeviceRole
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionTeardown
    @Inject
    constructor(
        private val auth: AuthDataStore,
        private val settings: SettingsDataStore,
        private val local: LocalData,
    ) {
        suspend fun run() {
            withContext(NonCancellable) {
                local.wipe()
                settings.setRole(DeviceRole.PARENT)
                settings.setMode(AppMode.UNSET)
                settings.setOnboardingComplete(false)
                auth.clear()
            }
        }
    }

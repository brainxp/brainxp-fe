package com.example.brainxp.domain

import com.example.brainxp.data.prefs.SettingsDataStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProtectionSwitch
    @Inject
    constructor(
        private val settings: SettingsDataStore,
        private val parentLock: ParentLock,
    ) {
        suspend fun toggle(): Boolean {
            val enabled = settings.settings.first().protectionEnabled
            if (enabled && !parentLock.allows(GuardedAction.DISABLE_PROTECTION)) return false
            settings.setProtectionEnabled(!enabled)
            return true
        }
    }

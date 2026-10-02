package com.example.brainxp.data.repo

import com.example.brainxp.core.network.AuthTokenStore
import com.example.brainxp.core.network.AuthTokens
import com.example.brainxp.data.prefs.AuthDataStore
import com.example.brainxp.data.prefs.SettingsDataStore
import com.example.brainxp.di.AppScope
import com.example.brainxp.domain.model.DeviceRole
import com.example.brainxp.domain.model.deviceRoleOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRestorer
    @Inject
    constructor(
        private val store: AuthDataStore,
        private val tokens: AuthTokenStore,
        private val settings: SettingsDataStore,
        @AppScope private val scope: CoroutineScope,
    ) {
        fun restore() {
            scope.launch {
                val saved = store.current()
                val access = saved.accessToken ?: return@launch
                tokens.update(AuthTokens(access, saved.refreshToken.orEmpty()))
                val stored = settings.settings.first().role
                val wanted = roleForSession(stored, saved.role)
                if (wanted != stored) settings.setRole(wanted)
            }
        }
    }

fun roleForSession(
    stored: DeviceRole,
    sessionRole: String?,
): DeviceRole = if (sessionRole == null) stored else deviceRoleOf(sessionRole)

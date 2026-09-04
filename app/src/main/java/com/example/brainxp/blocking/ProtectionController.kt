package com.example.brainxp.blocking

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.brainxp.data.prefs.SettingsDataStore
import com.example.brainxp.di.AppScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProtectionController
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val settings: SettingsDataStore,
        @AppScope private val scope: CoroutineScope,
    ) {
        fun start() {
            scope.launch {
                settings.settings
                    .map { it.protectionEnabled }
                    .distinctUntilChanged()
                    .collect { enabled -> if (enabled) startService() else stopService() }
            }
        }

        private fun startService() {
            ContextCompat.startForegroundService(context, intent())
        }

        private fun stopService() {
            context.stopService(intent())
        }

        private fun intent() = Intent(context, BlockingService::class.java)
    }

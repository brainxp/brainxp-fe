package com.example.brainxp.blocking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.brainxp.data.prefs.SettingsDataStore
import com.example.brainxp.di.AppScope
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun settings(): SettingsDataStore

        @AppScope
        fun scope(): CoroutineScope
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val appContext = context.applicationContext
        val dependencies =
            runCatching {
                EntryPointAccessors.fromApplication(appContext, Dependencies::class.java)
            }.getOrNull() ?: return

        val pending = goAsync()
        dependencies.scope().launch {
            try {
                val enabled =
                    dependencies
                        .settings()
                        .settings
                        .first()
                        .protectionEnabled
                if (enabled) {
                    ContextCompat.startForegroundService(
                        appContext,
                        Intent(appContext, BlockingService::class.java),
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }
}

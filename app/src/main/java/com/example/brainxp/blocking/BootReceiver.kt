package com.example.brainxp.blocking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.brainxp.data.prefs.SettingsDataStore
import com.example.brainxp.di.AppScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject
    lateinit var settings: SettingsDataStore

    @Inject
    @AppScope
    lateinit var scope: CoroutineScope

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }
        val pending = goAsync()
        val appContext = context.applicationContext
        scope.launch {
            try {
                if (settings.settings.first().protectionEnabled) {
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

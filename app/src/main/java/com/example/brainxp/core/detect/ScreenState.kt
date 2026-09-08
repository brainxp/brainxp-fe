package com.example.brainxp.core.detect

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenState
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        val isScreenOn: Flow<Boolean> =
            callbackFlow {
                val receiver =
                    object : BroadcastReceiver() {
                        override fun onReceive(
                            received: Context?,
                            intent: Intent?,
                        ) {
                            when (intent?.action) {
                                Intent.ACTION_SCREEN_ON -> trySend(true)
                                Intent.ACTION_SCREEN_OFF -> trySend(false)
                            }
                        }
                    }

                trySend(interactive())

                val filter =
                    IntentFilter().apply {
                        addAction(Intent.ACTION_SCREEN_ON)
                        addAction(Intent.ACTION_SCREEN_OFF)
                    }
                ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

                awaitClose { context.unregisterReceiver(receiver) }
            }.distinctUntilChanged()

        fun interactive(): Boolean = context.getSystemService(PowerManager::class.java)?.isInteractive == true
    }

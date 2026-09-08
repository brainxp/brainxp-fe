package com.example.brainxp.core.permission

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessibilityWatch
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val permissions: PermissionStateProvider,
    ) {
        private val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) = permissions.refresh()

                override fun onChange(
                    selfChange: Boolean,
                    uri: Uri?,
                ) = permissions.refresh()
            }

        private var watching = false

        fun start() {
            if (watching) return
            watching = true
            WATCHED.forEach { setting ->
                context.contentResolver.registerContentObserver(
                    Settings.Secure.getUriFor(setting),
                    false,
                    observer,
                )
            }
        }

        fun stop() {
            if (!watching) return
            watching = false
            context.contentResolver.unregisterContentObserver(observer)
        }

        private companion object {
            val WATCHED =
                listOf(
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    Settings.Secure.ACCESSIBILITY_ENABLED,
                )
        }
    }

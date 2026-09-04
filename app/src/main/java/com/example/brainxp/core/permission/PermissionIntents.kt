package com.example.brainxp.core.permission

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface PermissionIntents {
    fun settingsIntents(permission: SpecialPermission): List<Intent>
}

@Singleton
class AndroidPermissionIntents
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : PermissionIntents {
        override fun settingsIntents(permission: SpecialPermission): List<Intent> = listOf(primary(permission), appDetails())

        private fun primary(permission: SpecialPermission): Intent =
            when (permission) {
                SpecialPermission.USAGE_ACCESS -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                SpecialPermission.OVERLAY -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri())
                SpecialPermission.NOTIFICATIONS -> appNotifications()
                SpecialPermission.BATTERY_EXEMPTION -> Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                SpecialPermission.ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            }

        private fun appNotifications(): Intent =
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

        private fun appDetails(): Intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri())

        private fun packageUri(): Uri = Uri.fromParts("package", context.packageName, null)
    }

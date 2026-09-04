package com.example.brainxp.core.permission

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface PermissionReader {
    fun isGranted(permission: SpecialPermission): Boolean
}

@Singleton
class AndroidPermissionReader
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : PermissionReader {
        override fun isGranted(permission: SpecialPermission): Boolean =
            when (permission) {
                SpecialPermission.USAGE_ACCESS -> hasUsageAccess()
                SpecialPermission.OVERLAY -> Settings.canDrawOverlays(context)
                SpecialPermission.NOTIFICATIONS -> NotificationManagerCompat.from(context).areNotificationsEnabled()
                SpecialPermission.BATTERY_EXEMPTION -> isIgnoringBatteryOptimizations()
                SpecialPermission.ACCESSIBILITY -> isOwnAccessibilityServiceEnabled()
            }

        private fun hasUsageAccess(): Boolean {
            val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
            val mode =
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName,
                )
            return if (mode == AppOpsManager.MODE_DEFAULT) {
                context.checkSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
            } else {
                mode == AppOpsManager.MODE_ALLOWED
            }
        }

        private fun isIgnoringBatteryOptimizations(): Boolean {
            val power = context.getSystemService(PowerManager::class.java) ?: return false
            return power.isIgnoringBatteryOptimizations(context.packageName)
        }

        private fun isOwnAccessibilityServiceEnabled(): Boolean {
            val resolver = context.contentResolver
            if (Settings.Secure.getInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) != 1) {
                return false
            }
            val enabled = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
            return enabled.split(':').any { it.substringBefore('/') == context.packageName }
        }
    }

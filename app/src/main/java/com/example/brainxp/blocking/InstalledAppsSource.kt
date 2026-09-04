package com.example.brainxp.blocking

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Settings
import android.telecom.TelecomManager
import com.example.brainxp.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstalledAppsSource
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @IoDispatcher private val io: CoroutineDispatcher,
    ) {
        suspend fun launchableApps(): List<InstalledApp> =
            withContext(io) {
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                context.packageManager
                    .queryIntentActivities(intent, 0)
                    .mapNotNull { resolved ->
                        val info = resolved.activityInfo?.applicationInfo ?: return@mapNotNull null
                        InstalledApp(
                            packageName = info.packageName,
                            label = context.packageManager.getApplicationLabel(info).toString(),
                            isGame = info.category == ApplicationInfo.CATEGORY_GAME,
                        )
                    }.distinctBy { it.packageName }
            }

        suspend fun protectedPackages(): ProtectedPackages =
            withContext(io) {
                ProtectedPackages(
                    own = context.packageName,
                    launcher = resolve(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)),
                    settings = resolve(Intent(Settings.ACTION_SETTINGS)),
                    dialer = context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage,
                    emergency = emergencyPackages(),
                )
            }

        private fun resolve(intent: Intent): String? =
            context.packageManager
                .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo
                ?.packageName
                ?.takeIf { it != ANDROID_RESOLVER }

        private fun emergencyPackages(): Set<String> =
            setOfNotNull(
                resolve(Intent(ACTION_EMERGENCY_ASSISTANCE)),
                resolve(Intent(Intent.ACTION_DIAL)),
            )

        private companion object {
            const val ANDROID_RESOLVER = "android"
            const val ACTION_EMERGENCY_ASSISTANCE = "android.telephony.action.EMERGENCY_ASSISTANCE"
        }
    }

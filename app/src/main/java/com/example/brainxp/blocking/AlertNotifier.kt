package com.example.brainxp.blocking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.brainxp.MainActivity
import com.example.brainxp.R
import com.example.brainxp.domain.model.AlertKind
import com.example.brainxp.domain.model.GuardianAlert
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertNotifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun announce(alert: GuardianAlert) {
            createChannel()
            manager().notify(idFor(alert), build(alert))
        }

        private fun build(alert: GuardianAlert): Notification {
            val body = wordingOf(alert)
            return NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(context.getColor(R.color.brand_navy))
                .setContentTitle(context.getString(R.string.alert_title_named, alert.subjectName))
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(openIntent(alert.id))
                .build()
        }

        private fun wordingOf(alert: GuardianAlert): String =
            alert.detail?.takeIf { it.isNotBlank() }
                ?: context.getString(bodyOf(alert.kind), alert.subjectName)

        private fun openIntent(alertId: Int): PendingIntent =
            PendingIntent.getActivity(
                context,
                REQUEST_BASE + alertId,
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private fun createChannel() {
            manager().createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.alert_channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.alert_channel_description)
                },
            )
        }

        private fun idFor(alert: GuardianAlert) = ID_BASE + alert.id

        private fun manager() = context.getSystemService(NotificationManager::class.java)

        private companion object {
            const val CHANNEL_ID = "brainxp_guardian_alerts"
            const val ID_BASE = 2000
            const val REQUEST_BASE = 70
        }
    }

internal fun bodyOf(kind: AlertKind): Int =
    when (kind) {
        AlertKind.ACCESSIBILITY_OFF -> R.string.alert_accessibility_off
        AlertKind.USAGE_ACCESS_OFF -> R.string.alert_usage_access_off
        AlertKind.OVERLAY_OFF -> R.string.alert_overlay_off
        AlertKind.PROTECTION_DISABLED -> R.string.alert_protection_disabled
        AlertKind.DEVICE_SILENT -> R.string.alert_device_silent
        AlertKind.UNKNOWN -> R.string.alert_unknown
    }

package com.example.brainxp.blocking

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.brainxp.MainActivity
import com.example.brainxp.R
import com.example.brainxp.core.time.clock
import com.example.brainxp.domain.PlaytimeCue
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpiryWarning
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun createChannel() {
            channel(
                id = WARNING_CHANNEL_ID,
                name = R.string.warning_channel_name,
                description = R.string.warning_channel_description,
                importance = NotificationManager.IMPORTANCE_HIGH,
            )
            channel(
                id = OFFER_CHANNEL_ID,
                name = R.string.offer_channel_name,
                description = R.string.offer_channel_description,
                importance = NotificationManager.IMPORTANCE_LOW,
            )
        }

        fun cancel() {
            notificationManager().cancel(WARNING_ID)
            notificationManager().cancel(OFFER_ID)
        }

        fun post(
            cue: PlaytimeCue,
            thresholdSeconds: Int,
        ) {
            val offer = cue == PlaytimeCue.OFFER
            val body = context.getString(bodyOf(cue))
            val notification =
                NotificationCompat
                    .Builder(context, if (offer) OFFER_CHANNEL_ID else WARNING_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(context.getColor(R.color.brand_navy))
                    .setContentTitle(titleOf(cue, thresholdSeconds))
                    .setContentText(body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(false)
                    .setPriority(if (offer) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(
                        PendingIntent.getActivity(
                            context,
                            REQUEST_OPEN,
                            Intent(context, MainActivity::class.java),
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                        ),
                    ).build()
            if (!offer) {
                notificationManager().cancel(OFFER_ID)
            }
            notificationManager().notify(if (offer) OFFER_ID else WARNING_ID, notification)
        }

        private fun titleOf(
            cue: PlaytimeCue,
            thresholdSeconds: Int,
        ): String =
            when (cue) {
                PlaytimeCue.LAST_CALL -> context.getString(R.string.expiry_warning_title, clock(thresholdSeconds))
                else -> context.getString(R.string.expiry_minutes_title, thresholdSeconds / SECONDS_PER_MINUTE)
            }

        private fun bodyOf(cue: PlaytimeCue): Int =
            when (cue) {
                PlaytimeCue.OFFER -> R.string.expiry_offer_body
                PlaytimeCue.WRAP_UP -> R.string.expiry_wrapup_body
                PlaytimeCue.LAST_CALL -> R.string.expiry_warning_body
            }

        private fun channel(
            id: String,
            name: Int,
            description: Int,
            importance: Int,
        ) {
            notificationManager().createNotificationChannel(
                NotificationChannel(id, context.getString(name), importance).apply {
                    this.description = context.getString(description)
                    setShowBadge(false)
                },
            )
        }

        private fun notificationManager() = context.getSystemService(NotificationManager::class.java)

        companion object {
            private const val WARNING_CHANNEL_ID = "brainxp_expiry_warning"
            private const val OFFER_CHANNEL_ID = "brainxp_playtime_offer"
            private const val WARNING_ID = 1002
            private const val OFFER_ID = 1003
            private const val REQUEST_OPEN = 21
            private const val SECONDS_PER_MINUTE = 60
        }
    }

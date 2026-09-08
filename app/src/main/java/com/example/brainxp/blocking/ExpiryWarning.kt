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
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.warning_channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.warning_channel_description)
                    setShowBadge(false)
                },
            )
        }

        fun cancel() {
            notificationManager().cancel(ID)
        }

        fun post(remainingSeconds: Int) {
            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(context.getString(R.string.expiry_warning_title, clock(remainingSeconds)))
                    .setContentText(context.getString(R.string.expiry_warning_body))
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(
                        PendingIntent.getActivity(
                            context,
                            REQUEST_OPEN,
                            Intent(context, MainActivity::class.java),
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                        ),
                    ).build()
            notificationManager().notify(ID, notification)
        }

        private fun notificationManager() = context.getSystemService(NotificationManager::class.java)

        companion object {
            private const val CHANNEL_ID = "brainxp_expiry_warning"
            private const val ID = 1002
            private const val REQUEST_OPEN = 21
        }
    }

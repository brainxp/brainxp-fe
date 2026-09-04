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
import com.example.brainxp.domain.model.UnlockState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class ProtectionNotification
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun createChannel() {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.blocking_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = context.getString(R.string.blocking_channel_description)
                    setShowBadge(false)
                },
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    DEGRADED_CHANNEL_ID,
                    context.getString(R.string.degraded_channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.degraded_channel_description)
                },
            )
        }

        fun build(
            snapshot: ProtectionSnapshot,
            blocked: Boolean,
            now: Long,
        ): Notification {
            val unlock = snapshot.restriction.unlock
            val running = unlock is UnlockState.Active && now < unlock.endAtElapsed

            return NotificationCompat
                .Builder(context, if (snapshot.degraded) DEGRADED_CHANNEL_ID else CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title(snapshot, blocked, running))
                .setContentText(body(snapshot, unlock, running, now))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setPriority(if (snapshot.degraded) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
                .setContentIntent(openApp())
                .build()
        }

        private fun title(
            snapshot: ProtectionSnapshot,
            blocked: Boolean,
            running: Boolean,
        ): String =
            when {
                snapshot.degraded -> context.getString(R.string.degraded_notification_title)
                blocked -> context.getString(R.string.blocking_notification_blocked)
                running -> context.getString(R.string.blocking_notification_unlocked)
                else -> context.getString(R.string.blocking_notification_active)
            }

        private fun body(
            snapshot: ProtectionSnapshot,
            unlock: UnlockState,
            running: Boolean,
            now: Long,
        ): String =
            when {
                snapshot.degraded -> {
                    context.getString(R.string.degraded_notification_body)
                }

                running && unlock is UnlockState.Active -> {
                    context.getString(R.string.blocking_notification_remaining, remaining(unlock.endAtElapsed - now))
                }

                else -> {
                    context.resources.getQuantityString(
                        R.plurals.blocking_notification_restricted_count,
                        snapshot.restriction.restrictedPackages.size,
                        snapshot.restriction.restrictedPackages.size,
                    )
                }
            }

        private fun remaining(millis: Long): String {
            val total = millis.milliseconds.inWholeSeconds
            return "%d:%02d".format(total / SECONDS_PER_MINUTE, total % SECONDS_PER_MINUTE)
        }

        private fun openApp(): PendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        companion object {
            const val ID = 1001
            private const val CHANNEL_ID = "brainxp_protection"
            private const val DEGRADED_CHANNEL_ID = "brainxp_protection_degraded"
            private const val SECONDS_PER_MINUTE = 60
        }
    }

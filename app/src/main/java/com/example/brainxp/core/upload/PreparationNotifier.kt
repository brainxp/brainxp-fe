package com.example.brainxp.core.upload

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.brainxp.MainActivity
import com.example.brainxp.R
import com.example.brainxp.data.repo.NotificationRepository
import com.example.brainxp.domain.model.AppNotification
import com.example.brainxp.domain.model.Material
import com.example.brainxp.domain.model.MaterialStatus
import com.example.brainxp.domain.model.NotificationKind
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreparationNotifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val notifications: NotificationRepository,
    ) {
        suspend fun announce(material: Material) {
            val ready = material.status == MaterialStatus.READY
            val recorded =
                notifications.record(
                    kind = if (ready) NotificationKind.QUESTIONS_READY else NotificationKind.MATERIAL_REJECTED,
                    materialId = material.id,
                    materialTitle = material.title,
                    questionCount = material.questionCount,
                ) ?: return
            createChannel()
            post(material, recorded)
        }

        fun cancel() {
            manager().cancel(ID)
        }

        private fun post(
            material: Material,
            record: AppNotification,
        ) {
            val ready = material.status == MaterialStatus.READY
            val builder =
                NotificationCompat
                    .Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(
                        context.getString(
                            if (ready) {
                                R.string.ready_notification_title
                            } else {
                                R.string.ready_notification_rejected_title
                            },
                        ),
                    ).setContentText(
                        if (ready) {
                            context.getString(
                                R.string.ready_notification_body,
                                material.title,
                                material.questionCount,
                            )
                        } else {
                            context.getString(R.string.ready_notification_rejected_body, material.title)
                        },
                    ).setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(openIntent(material.id, record.id, ready))

            if (ready) {
                builder.addAction(
                    R.mipmap.ic_launcher,
                    context.getString(R.string.ready_notification_action),
                    openIntent(material.id, record.id, ready = true),
                )
            }

            manager().notify(ID, builder.build())
        }

        private fun openIntent(
            materialId: String,
            notificationId: String,
            ready: Boolean,
        ): PendingIntent =
            PendingIntent.getActivity(
                context,
                REQUEST_OPEN,
                Intent(context, MainActivity::class.java)
                    .putExtra(if (ready) EXTRA_READY_MATERIAL else EXTRA_REJECTED_MATERIAL, materialId)
                    .putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private fun createChannel() {
            manager().createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.ready_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = context.getString(R.string.ready_channel_description)
                },
            )
        }

        private fun manager() = context.getSystemService(NotificationManager::class.java)

        companion object {
            const val EXTRA_READY_MATERIAL = "ready_material_id"
            const val EXTRA_REJECTED_MATERIAL = "rejected_material_id"
            const val EXTRA_NOTIFICATION_ID = "ready_notification_id"
            private const val CHANNEL_ID = "brainxp_questions_ready"
            private const val ID = 1003
            private const val REQUEST_OPEN = 31
        }
    }

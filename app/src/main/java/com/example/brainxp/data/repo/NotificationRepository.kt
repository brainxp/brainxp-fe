package com.example.brainxp.data.repo

import com.example.brainxp.core.time.AppClock
import com.example.brainxp.data.db.NotificationDao
import com.example.brainxp.data.db.NotificationEntity
import com.example.brainxp.domain.model.AppNotification
import com.example.brainxp.domain.model.NotificationKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

const val NOTIFICATION_HISTORY = 100

@Singleton
class NotificationRepository
    @Inject
    constructor(
        private val notifications: NotificationDao,
        private val clock: AppClock,
    ) {
        fun observe(): Flow<List<AppNotification>> =
            notifications.observeRecent(NOTIFICATION_HISTORY).map { rows -> rows.mapNotNull(::domainOf) }

        fun unreadCount(): Flow<Int> = notifications.observeUnreadCount()

        suspend fun record(
            kind: NotificationKind,
            materialId: String?,
            materialTitle: String,
            questionCount: Int,
        ): AppNotification? {
            if (materialId != null && notifications.findFor(materialId, kind.name) != null) {
                return null
            }
            val row =
                NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    kind = kind.name,
                    materialId = materialId,
                    materialTitle = materialTitle,
                    questionCount = questionCount,
                    createdAt = clock.wallClock(),
                )
            notifications.upsert(row)
            return domainOf(row)
        }

        suspend fun markRead(id: String) = notifications.markRead(id, clock.wallClock())

        suspend fun clear() = notifications.clearAll()

        private fun domainOf(row: NotificationEntity): AppNotification? =
            NotificationKind.fromName(row.kind)?.let { kind ->
                AppNotification(
                    id = row.id,
                    kind = kind,
                    materialId = row.materialId,
                    materialTitle = row.materialTitle,
                    questionCount = row.questionCount,
                    createdAt = row.createdAt,
                    readAt = row.readAt,
                )
            }
    }

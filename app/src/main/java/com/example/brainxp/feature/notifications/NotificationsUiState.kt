package com.example.brainxp.feature.notifications

import com.example.brainxp.domain.model.AppNotification
import com.example.brainxp.domain.model.NotificationKind

data class NotificationsUiState(
    val notifications: List<AppNotification> = emptyList(),
    val loading: Boolean = true,
) {
    val unreadCount: Int get() = notifications.count { it.unread }
}

sealed interface NotificationsEvent {
    data class Open(
        val id: String,
    ) : NotificationsEvent
}

internal fun destinationOf(record: AppNotification): NotificationsEffect? {
    val material = record.materialId ?: return null
    return when (record.kind) {
        NotificationKind.QUESTIONS_READY -> NotificationsEffect.OpenQuestions(material)
        NotificationKind.MATERIAL_REJECTED -> NotificationsEffect.OpenRejection(material)
    }
}

sealed interface NotificationsEffect {
    data class OpenQuestions(
        val materialId: String,
    ) : NotificationsEffect

    data class OpenRejection(
        val materialId: String,
    ) : NotificationsEffect
}

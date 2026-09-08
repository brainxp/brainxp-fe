package com.example.brainxp.feature.notifications

import com.example.brainxp.domain.model.AppNotification

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

sealed interface NotificationsEffect {
    data class OpenQuestions(
        val materialId: String,
    ) : NotificationsEffect
}

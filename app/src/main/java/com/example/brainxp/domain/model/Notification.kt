package com.example.brainxp.domain.model

enum class NotificationKind {
    QUESTIONS_READY,
    MATERIAL_REJECTED,
    ;

    companion object {
        fun fromName(value: String): NotificationKind? = entries.firstOrNull { it.name == value }
    }
}

data class AppNotification(
    val id: String,
    val kind: NotificationKind,
    val materialId: String?,
    val materialTitle: String,
    val questionCount: Int,
    val createdAt: Long,
    val readAt: Long? = null,
) {
    val unread: Boolean get() = readAt == null
}

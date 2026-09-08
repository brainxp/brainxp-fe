package com.example.brainxp.domain.model

enum class AlertKind {
    ACCESSIBILITY_OFF,
    USAGE_ACCESS_OFF,
    OVERLAY_OFF,
    PROTECTION_DISABLED,
    DEVICE_SILENT,
    UNKNOWN,
}

data class GuardianAlert(
    val id: Int,
    val subjectId: String,
    val subjectName: String,
    val kind: AlertKind,
    val detail: String? = null,
    val acknowledged: Boolean = false,
)

data class GuardianEvent(
    val type: String,
    val permission: String? = null,
    val required: Boolean = false,
) {
    companion object {
        const val REVOKED = "permission_revoked"
        const val RESTORED = "permission_restored"
    }
}

fun alertKindOf(wire: String): AlertKind =
    when (wire) {
        "accessibility_off" -> AlertKind.ACCESSIBILITY_OFF
        "usage_access_off" -> AlertKind.USAGE_ACCESS_OFF
        "overlay_off" -> AlertKind.OVERLAY_OFF
        "protection_disabled" -> AlertKind.PROTECTION_DISABLED
        "device_silent" -> AlertKind.DEVICE_SILENT
        else -> AlertKind.UNKNOWN
    }

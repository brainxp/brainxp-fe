package com.example.brainxp.domain.model

enum class ActivityKind {
    MATERIAL_ADDED,
    SESSION_COMPLETED,
    REWARD_EARNED,
    UNLOCK_STARTED,
    UNLOCK_ENDED,
    PROTECTION_DISABLED,
    PROTECTION_DEGRADED,
    PROTECTION_ANOMALY,
}

data class ActivityEvent(
    val kind: ActivityKind,
    val timestamp: Long,
    val payload: Map<String, String> = emptyMap(),
)

enum class GuardianStatus {
    OK,
    DEGRADED,
    DISABLED,
    UNKNOWN,
}

data class FamilyChild(
    val childId: String,
    val name: String,
    val level: AcademicLevel?,
)

data class PairingCode(
    val code: String,
    val childId: String,
    val expiresAt: String,
    val attemptsAllowed: Int,
)

data class DeviceBinding(
    val bound: Boolean,
    val familyMode: Boolean,
    val subjectName: String?,
)

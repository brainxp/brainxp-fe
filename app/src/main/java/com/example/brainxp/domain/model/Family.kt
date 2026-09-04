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
    val guardianStatus: GuardianStatus,
    val availableMinutes: Int,
    val sessionsThisWeek: Int,
    val accuracy: Double?,
)

data class ChildConfig(
    val restrictedPackages: List<String>,
    val dailyCapMinutes: Int,
    val rewardPerSessionMinutes: Int,
)

data class PairingResult(
    val childId: String,
    val childName: String,
)

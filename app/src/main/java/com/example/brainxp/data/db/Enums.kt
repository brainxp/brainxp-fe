package com.example.brainxp.data.db

enum class SyncState {
    PENDING,
    SYNCED,
    FAILED,
}

enum class UnlockStatus {
    ACTIVE,
    EXPIRED,
    ENDED,
}

enum class ActivityEventType {
    MATERIAL_ADDED,
    SESSION_COMPLETED,
    REWARD_EARNED,
    UNLOCK_STARTED,
    UNLOCK_ENDED,
    PROTECTION_DISABLED,
    PROTECTION_DEGRADED,
    PROTECTION_ANOMALY,
}

enum class PendingOperationType {
    UPLOAD_MATERIAL,
    SUBMIT_ANSWER,
    COMPLETE_SESSION,
    SYNC_LEDGER,
    SEND_EVENTS,
}

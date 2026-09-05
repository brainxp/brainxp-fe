package com.example.brainxp.domain

import com.example.brainxp.domain.model.DeviceRole

enum class GuardedAction {
    CHANGE_RESTRICTIONS,
    SWITCH_MODE,
    DISABLE_PROTECTION,
}

data class ChildDeviceLock(
    val role: DeviceRole,
    val pinSet: Boolean,
) {
    val locked: Boolean get() = role == DeviceRole.CHILD && pinSet
}

object ParentGate {
    fun requiresPin(
        lock: ChildDeviceLock,
        action: GuardedAction,
    ): Boolean = lock.locked && action in GUARDED

    fun allows(
        lock: ChildDeviceLock,
        action: GuardedAction,
        pinVerified: Boolean,
    ): Boolean = !requiresPin(lock, action) || pinVerified

    private val GUARDED =
        setOf(
            GuardedAction.CHANGE_RESTRICTIONS,
            GuardedAction.SWITCH_MODE,
            GuardedAction.DISABLE_PROTECTION,
        )
}

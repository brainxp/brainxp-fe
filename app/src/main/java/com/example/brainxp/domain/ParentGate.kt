package com.example.brainxp.domain

import com.example.brainxp.domain.model.DeviceRole

enum class GuardedAction {
    CHANGE_RESTRICTIONS,
    SWITCH_MODE,
}

fun protectionHeld(lockedAppCount: Int): Boolean = lockedAppCount > 0

data class ChildDeviceLock(
    val role: DeviceRole,
) {
    val locked: Boolean get() = role == DeviceRole.CHILD
}

object ParentGate {
    fun allows(
        lock: ChildDeviceLock,
        action: GuardedAction,
    ): Boolean = !lock.locked || action !in GUARDED

    private val GUARDED =
        setOf(
            GuardedAction.CHANGE_RESTRICTIONS,
            GuardedAction.SWITCH_MODE,
        )
}

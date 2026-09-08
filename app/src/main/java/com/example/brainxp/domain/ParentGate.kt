package com.example.brainxp.domain

import com.example.brainxp.data.prefs.SettingsSnapshot
import com.example.brainxp.domain.model.DeviceRole

enum class GuardedAction {
    CHANGE_RESTRICTIONS,
    SWITCH_MODE,
    DISABLE_PROTECTION,
}

enum class ProtectionControl {
    HELD,
    OWNED,
    ABSENT,
}

fun protectionControlOf(
    role: DeviceRole,
    ownRules: Boolean,
): ProtectionControl =
    when {
        role == DeviceRole.CHILD -> ProtectionControl.HELD
        ownRules -> ProtectionControl.OWNED
        else -> ProtectionControl.ABSENT
    }

val SettingsSnapshot.protectionHeld: Boolean
    get() = protectionEnabled || role == DeviceRole.CHILD

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
            GuardedAction.DISABLE_PROTECTION,
        )
}

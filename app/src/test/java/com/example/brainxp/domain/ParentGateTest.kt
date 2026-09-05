package com.example.brainxp.domain

import com.example.brainxp.domain.model.DeviceRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val CHILD_WITH_PIN = ChildDeviceLock(role = DeviceRole.CHILD, pinSet = true)
private val CHILD_WITHOUT_PIN = ChildDeviceLock(role = DeviceRole.CHILD, pinSet = false)
private val PARENT = ChildDeviceLock(role = DeviceRole.PARENT, pinSet = true)

class ParentGateTest {
    @Test
    fun `restriction settings are locked on a child device without the pin`() {
        assertTrue(ParentGate.requiresPin(CHILD_WITH_PIN, GuardedAction.CHANGE_RESTRICTIONS))
        assertFalse(
            "restrictions must not be reachable without the pin",
            ParentGate.allows(CHILD_WITH_PIN, GuardedAction.CHANGE_RESTRICTIONS, pinVerified = false),
        )
    }

    @Test
    fun `mode switching is locked on a child device without the pin`() {
        assertTrue(ParentGate.requiresPin(CHILD_WITH_PIN, GuardedAction.SWITCH_MODE))
        assertFalse(
            "mode switching must not be reachable without the pin",
            ParentGate.allows(CHILD_WITH_PIN, GuardedAction.SWITCH_MODE, pinVerified = false),
        )
    }

    @Test
    fun `disabling protection is locked on a child device without the pin`() {
        assertTrue(ParentGate.requiresPin(CHILD_WITH_PIN, GuardedAction.DISABLE_PROTECTION))
        assertFalse(
            "protection must not be disablable without the pin",
            ParentGate.allows(CHILD_WITH_PIN, GuardedAction.DISABLE_PROTECTION, pinVerified = false),
        )
    }

    @Test
    fun `every guarded action opens once the pin is verified`() {
        GuardedAction.entries.forEach { action ->
            assertTrue(
                "$action should open with a verified pin",
                ParentGate.allows(CHILD_WITH_PIN, action, pinVerified = true),
            )
        }
    }

    @Test
    fun `no action is gated on a parent device`() {
        GuardedAction.entries.forEach { action ->
            assertFalse(
                "$action must never ask a parent for a pin",
                ParentGate.requiresPin(PARENT, action),
            )
        }
    }

    @Test
    fun `a child device with no pin set is not locked out of its own settings`() {
        GuardedAction.entries.forEach { action ->
            assertTrue(
                "$action must stay reachable until a parent sets a pin",
                ParentGate.allows(CHILD_WITHOUT_PIN, action, pinVerified = false),
            )
        }
    }

    @Test
    fun `every guarded action is actually covered by the gate`() {
        GuardedAction.entries.forEach { action ->
            assertTrue(
                "$action was added to the enum but left unguarded",
                ParentGate.requiresPin(CHILD_WITH_PIN, action),
            )
        }
    }
}

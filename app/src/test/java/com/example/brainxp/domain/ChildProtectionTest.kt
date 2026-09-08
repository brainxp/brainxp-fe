package com.example.brainxp.domain

import com.example.brainxp.data.prefs.SettingsSnapshot
import com.example.brainxp.domain.model.DeviceRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChildProtectionTest {
    @Test
    fun `a child device is protected even though nobody switched it on`() {
        val snapshot = SettingsSnapshot(role = DeviceRole.CHILD, protectionEnabled = false)

        assertTrue(snapshot.protectionHeld)
    }

    @Test
    fun `a child device stays protected when it is also switched on`() {
        val snapshot = SettingsSnapshot(role = DeviceRole.CHILD, protectionEnabled = true)

        assertTrue(snapshot.protectionHeld)
    }

    @Test
    fun `a parent device is only protected once someone switches it on`() {
        val off = SettingsSnapshot(role = DeviceRole.PARENT, protectionEnabled = false)
        val on = SettingsSnapshot(role = DeviceRole.PARENT, protectionEnabled = true)

        assertFalse(off.protectionHeld)
        assertTrue(on.protectionHeld)
    }

    @Test
    fun `the child device holds protection on with no switch to touch`() {
        assertEquals(
            ProtectionControl.HELD,
            protectionControlOf(role = DeviceRole.CHILD, ownRules = false),
        )
        assertEquals(
            ProtectionControl.HELD,
            protectionControlOf(role = DeviceRole.CHILD, ownRules = true),
        )
    }

    @Test
    fun `a phone that carries its own rules owns the switch`() {
        assertEquals(
            ProtectionControl.OWNED,
            protectionControlOf(role = DeviceRole.PARENT, ownRules = true),
        )
    }

    @Test
    fun `a parent who never joined the rules is offered no switch to guard nothing`() {
        assertEquals(
            ProtectionControl.ABSENT,
            protectionControlOf(role = DeviceRole.PARENT, ownRules = false),
        )
    }

    @Test
    fun `the child device cannot be talked out of protection`() {
        assertTrue(ChildDeviceLock(DeviceRole.CHILD).locked)
        assertFalse(ParentGate.allows(ChildDeviceLock(DeviceRole.CHILD), GuardedAction.DISABLE_PROTECTION))
    }
}

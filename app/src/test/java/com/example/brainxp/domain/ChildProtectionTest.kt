package com.example.brainxp.domain

import com.example.brainxp.domain.model.DeviceRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChildProtectionTest {
    @Test
    fun `a paired child device is locked out of its own rules`() {
        assertTrue(ChildDeviceLock(DeviceRole.CHILD).locked)
    }

    @Test
    fun `a parent device is free to change its own rules`() {
        assertFalse(ChildDeviceLock(DeviceRole.PARENT).locked)
    }

    @Test
    fun `a child cannot uncheck the apps that hold protection up`() {
        assertFalse(
            ParentGate.allows(ChildDeviceLock(DeviceRole.CHILD), GuardedAction.CHANGE_RESTRICTIONS),
        )
    }

    @Test
    fun `a child cannot switch modes to shake protection off`() {
        assertFalse(
            ParentGate.allows(ChildDeviceLock(DeviceRole.CHILD), GuardedAction.SWITCH_MODE),
        )
    }

    @Test
    fun `a parent keeps both of those doors open`() {
        val unlocked = ChildDeviceLock(DeviceRole.PARENT)

        assertTrue(ParentGate.allows(unlocked, GuardedAction.CHANGE_RESTRICTIONS))
        assertTrue(ParentGate.allows(unlocked, GuardedAction.SWITCH_MODE))
    }
}

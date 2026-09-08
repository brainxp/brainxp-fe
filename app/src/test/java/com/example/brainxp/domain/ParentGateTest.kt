package com.example.brainxp.domain

import com.example.brainxp.domain.model.DeviceRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val CHILD = ChildDeviceLock(role = DeviceRole.CHILD)
private val PARENT = ChildDeviceLock(role = DeviceRole.PARENT)

class ParentGateTest {
    @Test
    fun `a paired child device refuses every guarded action`() {
        GuardedAction.entries.forEach { action ->
            assertFalse(
                "$action must never be reachable from a paired child device",
                ParentGate.allows(CHILD, action),
            )
        }
    }

    @Test
    fun `the lock needs nothing but the role`() {
        assertTrue("a paired child device is locked on its role alone", CHILD.locked)
        assertFalse("an unpaired device is never locked", PARENT.locked)
    }

    @Test
    fun `no action is gated on a device that is not a paired child`() {
        GuardedAction.entries.forEach { action ->
            assertTrue(
                "$action must stay reachable when nobody manages this phone",
                ParentGate.allows(PARENT, action),
            )
        }
    }

    @Test
    fun `every guarded action is actually covered by the gate`() {
        GuardedAction.entries.forEach { action ->
            assertFalse(
                "$action was added to the enum but left unguarded",
                ParentGate.allows(CHILD, action),
            )
        }
    }
}

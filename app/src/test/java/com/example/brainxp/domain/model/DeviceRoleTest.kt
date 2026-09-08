package com.example.brainxp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceRoleTest {
    @Test
    fun `a personal account keeps control of its own rules`() {
        assertEquals(DeviceRole.PARENT, deviceRoleOf("personal"))
    }

    @Test
    fun `a family parent keeps control of its own rules`() {
        assertEquals(DeviceRole.PARENT, deviceRoleOf("parent"))
    }

    @Test
    fun `only a child hands control to someone else`() {
        assertEquals(DeviceRole.CHILD, deviceRoleOf("child"))
        assertEquals(DeviceRole.CHILD, deviceRoleOf("CHILD"))
    }

    @Test
    fun `an unfamiliar role never locks the device down`() {
        assertEquals(DeviceRole.PARENT, deviceRoleOf(null))
        assertEquals(DeviceRole.PARENT, deviceRoleOf(""))
        assertEquals(DeviceRole.PARENT, deviceRoleOf("guardian"))
    }

    @Test
    fun `a personal account never lands on the family dashboard`() {
        assertFalse(familyParent(DeviceRole.PARENT, familyId = null, serverRole = "personal"))
    }

    @Test
    fun `a parent reaches the family dashboard on either piece of evidence`() {
        assertTrue(familyParent(DeviceRole.PARENT, familyId = "5f1e", serverRole = "personal"))
        assertTrue(familyParent(DeviceRole.PARENT, familyId = null, serverRole = "parent"))
    }

    @Test
    fun `a paired child device stays on its own home`() {
        assertFalse(familyParent(DeviceRole.CHILD, familyId = "5f1e", serverRole = "parent"))
    }

    @Test
    fun `a device with nothing signed in stays on its own home`() {
        assertFalse(familyParent(role = null, familyId = null, serverRole = null))
        assertFalse(familyParent(DeviceRole.PARENT, familyId = null, serverRole = null))
    }
}

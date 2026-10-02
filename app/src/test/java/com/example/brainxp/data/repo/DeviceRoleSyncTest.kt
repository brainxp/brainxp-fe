package com.example.brainxp.data.repo

import com.example.brainxp.domain.model.DeviceRole
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceRoleSyncTest {
    @Test
    fun `a personal session on a phone still marked as a child is put back to personal`() {
        assertEquals(DeviceRole.PARENT, roleForSession(stored = DeviceRole.CHILD, sessionRole = "personal"))
    }

    @Test
    fun `a parent session never inherits a leftover child role`() {
        assertEquals(DeviceRole.PARENT, roleForSession(stored = DeviceRole.CHILD, sessionRole = "owner"))
    }

    @Test
    fun `a paired child session keeps the child role`() {
        assertEquals(DeviceRole.CHILD, roleForSession(stored = DeviceRole.CHILD, sessionRole = "child"))
    }

    @Test
    fun `a child session corrects a phone wrongly marked as a parent`() {
        assertEquals(DeviceRole.CHILD, roleForSession(stored = DeviceRole.PARENT, sessionRole = "child"))
    }

    @Test
    fun `without a session the stored role is left alone`() {
        assertEquals(DeviceRole.CHILD, roleForSession(stored = DeviceRole.CHILD, sessionRole = null))
    }
}

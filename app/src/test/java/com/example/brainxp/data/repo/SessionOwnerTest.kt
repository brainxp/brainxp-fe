package com.example.brainxp.data.repo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionOwnerTest {
    @Test
    fun `the same owner signing in again is not a change`() {
        assertFalse(ownerChanged(lastOwnerId = "subject-1", incomingOwnerId = "subject-1"))
    }

    @Test
    fun `a different owner signing in is a change`() {
        assertTrue(ownerChanged(lastOwnerId = "subject-1", incomingOwnerId = "subject-2"))
    }

    @Test
    fun `an unknown previous owner counts as a change`() {
        assertTrue(ownerChanged(lastOwnerId = null, incomingOwnerId = "subject-1"))
    }

    @Test
    fun `an owner we cannot identify counts as a change`() {
        assertTrue(ownerChanged(lastOwnerId = "subject-1", incomingOwnerId = null))
    }

    @Test
    fun `two unknowns still count as a change`() {
        assertTrue(ownerChanged(lastOwnerId = null, incomingOwnerId = null))
    }
}

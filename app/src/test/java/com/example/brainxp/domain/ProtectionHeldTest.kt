package com.example.brainxp.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionHeldTest {
    @Test
    fun `a phone with nothing locked has nothing to protect`() {
        assertFalse(protectionHeld(lockedAppCount = 0))
    }

    @Test
    fun `locking a single app is enough to put protection to work`() {
        assertTrue(protectionHeld(lockedAppCount = 1))
    }

    @Test
    fun `locking several apps keeps protection at work`() {
        assertTrue(protectionHeld(lockedAppCount = 7))
    }
}

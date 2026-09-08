package com.example.brainxp.blocking

import com.example.brainxp.domain.model.DeviceBinding
import com.example.brainxp.domain.model.DeviceRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BindingCheckTest {
    @Test
    fun `a child device with no earlier check is due`() {
        assertTrue(dueForBindingCheck(DeviceRole.CHILD, now = 0L, lastCheckAt = null, windowMs = WINDOW))
    }

    @Test
    fun `a parent device is never due`() {
        assertFalse(dueForBindingCheck(DeviceRole.PARENT, now = 0L, lastCheckAt = null, windowMs = WINDOW))
    }

    @Test
    fun `a check inside the throttle window is skipped`() {
        assertFalse(dueForBindingCheck(DeviceRole.CHILD, now = WINDOW - 1, lastCheckAt = 0L, windowMs = WINDOW))
    }

    @Test
    fun `a check on the throttle boundary is allowed`() {
        assertTrue(dueForBindingCheck(DeviceRole.CHILD, now = WINDOW, lastCheckAt = 0L, windowMs = WINDOW))
    }

    @Test
    fun `a check past the throttle window is allowed`() {
        assertTrue(dueForBindingCheck(DeviceRole.CHILD, now = WINDOW * 2, lastCheckAt = 0L, windowMs = WINDOW))
    }

    @Test
    fun `an unbound device is released`() {
        assertTrue(releasesDevice(DeviceBinding(bound = false, familyMode = true, subjectName = "Rina")))
    }

    @Test
    fun `a bound device is kept`() {
        assertFalse(releasesDevice(DeviceBinding(bound = true, familyMode = true, subjectName = "Rina")))
    }

    @Test
    fun `a failed call never releases the device`() {
        assertFalse(releasesDevice(null))
    }

    @Test
    fun `leaving family mode alone does not release a still-bound device`() {
        assertFalse(releasesDevice(DeviceBinding(bound = true, familyMode = false, subjectName = null)))
    }

    private companion object {
        const val WINDOW = 10_000L
    }
}

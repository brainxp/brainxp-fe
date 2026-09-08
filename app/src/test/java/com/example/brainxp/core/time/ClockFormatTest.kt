package com.example.brainxp.core.time

import org.junit.Assert.assertEquals
import org.junit.Test

class ClockFormatTest {
    @Test
    fun `an empty balance still shows every field`() {
        assertEquals("00:00:00", fullClock(0))
    }

    @Test
    fun `a balance under an hour keeps the hour field visible`() {
        assertEquals("00:25:00", fullClock(1_500))
    }

    @Test
    fun `seconds alone do not collapse the rest`() {
        assertEquals("00:00:59", fullClock(59))
    }

    @Test
    fun `an hour is padded to two digits`() {
        assertEquals("01:03:20", fullClock(3_800))
    }

    @Test
    fun `a negative balance reads as empty rather than counting backwards`() {
        assertEquals("00:00:00", fullClock(-500))
    }

    @Test
    fun `a balance beyond ninety nine hours is not truncated`() {
        assertEquals("100:00:00", fullClock(360_000))
    }

    @Test
    fun `the compact clock the receipts and notifications use is unchanged`() {
        assertEquals("25:00", clock(1_500))
        assertEquals("1:03:20", clock(3_800))
        assertEquals("00:45", clock(45))
    }
}

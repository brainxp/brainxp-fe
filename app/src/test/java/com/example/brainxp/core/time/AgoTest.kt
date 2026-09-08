package com.example.brainxp.core.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

private val JAKARTA = ZoneId.of("Asia/Jakarta")

private fun at(
    day: Int,
    hour: Int,
    minute: Int = 0,
): Long =
    LocalDateTime
        .of(2026, 9, day, hour, minute)
        .atZone(JAKARTA)
        .toInstant()
        .toEpochMilli()

class AgoTest {
    @Test
    fun `something that just landed reads as now rather than zero minutes`() {
        val ago = agoOf(then = at(day = 8, hour = 12), now = at(day = 8, hour = 12) + 30_000, zone = JAKARTA)

        assertEquals(AgoScale.NOW, ago.scale)
    }

    @Test
    fun `minutes are counted until the hour turns`() {
        val ago = agoOf(then = at(day = 8, hour = 12), now = at(day = 8, hour = 12, minute = 13), zone = JAKARTA)

        assertEquals(AgoScale.MINUTES, ago.scale)
        assertEquals(13, ago.amount)
    }

    @Test
    fun `the last minute before the hour is still minutes`() {
        val ago = agoOf(then = at(day = 8, hour = 12), now = at(day = 8, hour = 12, minute = 59), zone = JAKARTA)

        assertEquals(AgoScale.MINUTES, ago.scale)
        assertEquals(59, ago.amount)
    }

    @Test
    fun `an hour old within the same day counts hours`() {
        val ago = agoOf(then = at(day = 8, hour = 9), now = at(day = 8, hour = 14), zone = JAKARTA)

        assertEquals(AgoScale.HOURS, ago.scale)
        assertEquals(5, ago.amount)
    }

    @Test
    fun `late last night reads as yesterday even though only ten hours passed`() {
        val ago = agoOf(then = at(day = 7, hour = 22), now = at(day = 8, hour = 8), zone = JAKARTA)

        assertEquals(AgoScale.YESTERDAY, ago.scale)
    }

    @Test
    fun `early this morning is still hours rather than yesterday`() {
        val ago = agoOf(then = at(day = 8, hour = 1), now = at(day = 8, hour = 11), zone = JAKARTA)

        assertEquals(AgoScale.HOURS, ago.scale)
        assertEquals(10, ago.amount)
    }

    @Test
    fun `anything older than yesterday falls back to a date`() {
        val ago = agoOf(then = at(day = 5, hour = 12), now = at(day = 8, hour = 12), zone = JAKARTA)

        assertEquals(AgoScale.DATE, ago.scale)
    }

    @Test
    fun `a timestamp from the future never shows a negative age`() {
        val ago = agoOf(then = at(day = 8, hour = 13), now = at(day = 8, hour = 12), zone = JAKARTA)

        assertEquals(AgoScale.NOW, ago.scale)
    }
}

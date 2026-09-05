package com.example.brainxp.core.upload

import com.example.brainxp.domain.model.MaterialStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaterialPreparationBackoffTest {
    @Test
    fun `the wait grows from one second and stops at five`() {
        val waits = mutableListOf(FIRST_DELAY_MILLIS)
        repeat(8) { waits += nextDelay(waits.last()) }

        assertEquals(
            listOf(1_000L, 1_500L, 2_250L, 3_375L, 5_000L, 5_000L, 5_000L, 5_000L, 5_000L),
            waits,
        )
    }

    @Test
    fun `the wait never exceeds the ceiling`() {
        var wait = FIRST_DELAY_MILLIS
        repeat(50) { wait = nextDelay(wait) }

        assertEquals(MAX_DELAY_MILLIS, wait)
    }

    @Test
    fun `the budget survives a slow gate plus a spell in the background`() {
        assertTrue(
            "a 45s gate plus minutes backgrounded must still fit",
            PREPARATION_BUDGET_MILLIS > 5 * 60_000L,
        )
    }

    @Test
    fun `the budget is reached in a bounded number of polls`() {
        var waited = 0L
        var wait = FIRST_DELAY_MILLIS
        var polls = 0
        while (waited < PREPARATION_BUDGET_MILLIS) {
            waited += wait
            wait = nextDelay(wait)
            polls++
        }

        assertEquals(123, polls)
    }

    @Test
    fun `ready and failed settle, everything else keeps waiting`() {
        assertTrue(settled(MaterialStatus.READY))
        assertTrue(settled(MaterialStatus.FAILED))
        assertFalse(settled(MaterialStatus.PROCESSING))
        assertFalse(settled(MaterialStatus.UPLOADING))
    }
}

package com.example.brainxp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val NOW_WALL = 1_700_000_000_000L
private const val MINUTE = 60_000L

private fun active(endAtWallClock: Long) =
    UnlockState.Active(
        unlockId = "unlock-1",
        endAtElapsed = 999_999L,
        endAtWallClock = endAtWallClock,
        allowedPackages = setOf("com.mobile.legends"),
    )

class UnlockRebaseTest {
    @Test
    fun `an unlock whose wall clock end has passed is expired`() {
        val rebased = active(NOW_WALL - MINUTE).rebasedAfterBoot(NOW_WALL, nowElapsed = 0L)

        assertEquals(UnlockState.Expired, rebased)
    }

    @Test
    fun `an unlock ending exactly now is expired`() {
        val rebased = active(NOW_WALL).rebasedAfterBoot(NOW_WALL, nowElapsed = 0L)

        assertEquals(UnlockState.Expired, rebased)
    }

    @Test
    fun `remaining time is rebased onto the new elapsed clock`() {
        val rebased = active(NOW_WALL + 5 * MINUTE).rebasedAfterBoot(NOW_WALL, nowElapsed = 0L)

        assertTrue(rebased is UnlockState.Active)
        assertEquals(5 * MINUTE, (rebased as UnlockState.Active).endAtElapsed)
    }

    @Test
    fun `rebasing accounts for elapsed time already on the clock`() {
        val rebased = active(NOW_WALL + 5 * MINUTE).rebasedAfterBoot(NOW_WALL, nowElapsed = 30_000L)

        assertEquals(30_000L + 5 * MINUTE, (rebased as UnlockState.Active).endAtElapsed)
    }

    @Test
    fun `the stale elapsed value from before the reboot is discarded`() {
        val stale = active(NOW_WALL + MINUTE)

        val rebased = stale.rebasedAfterBoot(NOW_WALL, nowElapsed = 0L) as UnlockState.Active

        assertEquals(MINUTE, rebased.endAtElapsed)
        assertEquals(999_999L, stale.endAtElapsed)
    }

    @Test
    fun `identity fields survive the rebase`() {
        val rebased = active(NOW_WALL + MINUTE).rebasedAfterBoot(NOW_WALL, nowElapsed = 0L) as UnlockState.Active

        assertEquals("unlock-1", rebased.unlockId)
        assertEquals(setOf("com.mobile.legends"), rebased.allowedPackages)
        assertEquals(NOW_WALL + MINUTE, rebased.endAtWallClock)
    }

    @Test
    fun `a clock moved backwards extends rather than expires`() {
        val rebased = active(NOW_WALL + MINUTE).rebasedAfterBoot(NOW_WALL - MINUTE, nowElapsed = 0L)

        assertEquals(2 * MINUTE, (rebased as UnlockState.Active).endAtElapsed)
    }
}

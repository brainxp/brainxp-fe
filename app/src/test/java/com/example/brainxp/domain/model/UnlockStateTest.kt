package com.example.brainxp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val GAME = "com.game.one"
private const val SOCIAL = "com.social.two"

private fun session(
    budgetMillis: Long = 60_000L,
    consumed: Map<String, Long> = emptyMap(),
) = UnlockState.Active(
    unlockId = "unlock-1",
    budgetMillis = budgetMillis,
    consumedByPackage = consumed,
    allowedPackages = setOf(GAME, SOCIAL),
)

class UnlockStateTest {
    @Test
    fun `a new session has its whole budget remaining`() {
        assertEquals(60_000L, session().remainingMillis)
        assertFalse(session().exhausted)
    }

    @Test
    fun `advancing accumulates against one package`() {
        val advanced = session().advanced(GAME, 1_000L).advanced(GAME, 500L)

        assertEquals(mapOf(GAME to 1_500L), advanced.consumedByPackage)
        assertEquals(58_500L, advanced.remainingMillis)
    }

    @Test
    fun `advancing keeps packages apart but shares the budget`() {
        val advanced = session().advanced(GAME, 1_000L).advanced(SOCIAL, 2_000L)

        assertEquals(mapOf(GAME to 1_000L, SOCIAL to 2_000L), advanced.consumedByPackage)
        assertEquals(57_000L, advanced.remainingMillis)
    }

    @Test
    fun `advancing past the budget stops at the budget`() {
        val advanced = session(budgetMillis = 1_000L).advanced(GAME, 5_000L)

        assertEquals(mapOf(GAME to 1_000L), advanced.consumedByPackage)
        assertEquals(0L, advanced.remainingMillis)
        assertTrue(advanced.exhausted)
    }

    @Test
    fun `advancing an exhausted session changes nothing`() {
        val spent = session(budgetMillis = 1_000L).advanced(GAME, 1_000L)

        assertEquals(spent, spent.advanced(GAME, 1_000L))
    }

    @Test
    fun `advancing by zero or backwards changes nothing`() {
        val start = session()

        assertEquals(start, start.advanced(GAME, 0L))
        assertEquals(start, start.advanced(GAME, -5_000L))
    }

    @Test
    fun `a zero budget session is exhausted from the start`() {
        assertTrue(session(budgetMillis = 0L).exhausted)
    }

    @Test
    fun `only allowed packages are metered`() {
        assertTrue(session().meters(GAME))
        assertFalse(session().meters("com.notes.three"))
    }
}

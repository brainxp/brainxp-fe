package com.example.brainxp.domain

import com.example.brainxp.domain.model.RestrictionState
import com.example.brainxp.domain.model.UnlockState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val GAME = "com.game.one"
private const val SOCIAL = "com.social.two"
private const val NOTES = "com.notes.three"
private const val BUDGET = 10_000L

private fun active(
    allowed: Set<String>,
    consumed: Map<String, Long> = emptyMap(),
) = UnlockState.Active(
    unlockId = "unlock-1",
    budgetMillis = BUDGET,
    consumedByPackage = consumed,
    allowedPackages = allowed,
)

class RestrictionPolicyTest {
    private fun blocked(
        packageName: String,
        state: RestrictionState,
    ) = RestrictionPolicy.isBlocked(packageName, state)

    @Test
    fun `an unrestricted package is never blocked while locked`() {
        val state = RestrictionState(restrictedPackages = setOf(GAME), unlock = UnlockState.Locked)

        assertFalse(blocked(NOTES, state))
    }

    @Test
    fun `an unrestricted package is never blocked even mid unlock`() {
        val state = RestrictionState(restrictedPackages = setOf(GAME), unlock = active(setOf(GAME)))

        assertFalse(blocked(NOTES, state))
    }

    @Test
    fun `an unrestricted package is never blocked after expiry`() {
        val state = RestrictionState(restrictedPackages = setOf(GAME), unlock = UnlockState.Expired)

        assertFalse(blocked(NOTES, state))
    }

    @Test
    fun `nothing is blocked when the restricted set is empty`() {
        val state = RestrictionState(restrictedPackages = emptySet(), unlock = UnlockState.Locked)

        assertFalse(blocked(GAME, state))
    }

    @Test
    fun `a restricted package is blocked while locked`() {
        val state = RestrictionState(restrictedPackages = setOf(GAME), unlock = UnlockState.Locked)

        assertTrue(blocked(GAME, state))
    }

    @Test
    fun `a restricted package is blocked while expired`() {
        val state = RestrictionState(restrictedPackages = setOf(GAME), unlock = UnlockState.Expired)

        assertTrue(blocked(GAME, state))
    }

    @Test
    fun `an allowed package is not blocked during an active unlock`() {
        val state = RestrictionState(restrictedPackages = setOf(GAME), unlock = active(setOf(GAME)))

        assertFalse(blocked(GAME, state))
    }

    @Test
    fun `a session with budget left still covers the package`() {
        val state =
            RestrictionState(
                restrictedPackages = setOf(GAME),
                unlock = active(setOf(GAME), consumed = mapOf(GAME to BUDGET - 1)),
            )

        assertFalse(blocked(GAME, state))
    }

    @Test
    fun `a fully spent session blocks again`() {
        val state =
            RestrictionState(
                restrictedPackages = setOf(GAME),
                unlock = active(setOf(GAME), consumed = mapOf(GAME to BUDGET)),
            )

        assertTrue(blocked(GAME, state))
    }

    @Test
    fun `consumption spread over apps counts against the same budget`() {
        val state =
            RestrictionState(
                restrictedPackages = setOf(GAME, SOCIAL),
                unlock =
                    active(
                        setOf(GAME, SOCIAL),
                        consumed = mapOf(GAME to BUDGET / 2, SOCIAL to BUDGET / 2),
                    ),
            )

        assertTrue(blocked(GAME, state))
        assertTrue(blocked(SOCIAL, state))
    }

    @Test
    fun `a zero length session blocks from its first tick`() {
        val state =
            RestrictionState(
                restrictedPackages = setOf(GAME),
                unlock =
                    UnlockState.Active(
                        unlockId = "unlock-1",
                        budgetMillis = 0L,
                        allowedPackages = setOf(GAME),
                    ),
            )

        assertTrue(blocked(GAME, state))
    }

    @Test
    fun `an active unlock does not cover a package outside its allowed set`() {
        val state =
            RestrictionState(
                restrictedPackages = setOf(GAME, SOCIAL),
                unlock = active(setOf(GAME)),
            )

        assertFalse(blocked(GAME, state))
        assertTrue(blocked(SOCIAL, state))
    }

    @Test
    fun `an active unlock with no allowed packages blocks everything restricted`() {
        val state = RestrictionState(restrictedPackages = setOf(GAME), unlock = active(emptySet()))

        assertTrue(blocked(GAME, state))
    }

    @Test
    fun `an unlock that allows a package never restricted changes nothing`() {
        val state = RestrictionState(restrictedPackages = setOf(GAME), unlock = active(setOf(NOTES)))

        assertTrue(blocked(GAME, state))
        assertFalse(blocked(NOTES, state))
    }

    @Test
    fun `every restricted package is judged independently`() {
        val state =
            RestrictionState(
                restrictedPackages = setOf(GAME, SOCIAL, NOTES),
                unlock = active(setOf(GAME, NOTES)),
            )

        assertFalse(blocked(GAME, state))
        assertTrue(blocked(SOCIAL, state))
        assertFalse(blocked(NOTES, state))
    }

    @Test
    fun `running out flips every allowed package at once`() {
        val spent =
            RestrictionState(
                restrictedPackages = setOf(GAME, NOTES),
                unlock = active(setOf(GAME, NOTES), consumed = mapOf(GAME to BUDGET)),
            )
        val left =
            RestrictionState(
                restrictedPackages = setOf(GAME, NOTES),
                unlock = active(setOf(GAME, NOTES)),
            )

        assertFalse(blocked(GAME, left))
        assertFalse(blocked(NOTES, left))
        assertTrue(blocked(GAME, spent))
        assertTrue(blocked(NOTES, spent))
    }
}

package com.example.brainxp.domain

import com.example.brainxp.domain.model.RestrictionState
import com.example.brainxp.domain.model.UnlockState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val GAME = "com.game.one"
private const val SOCIAL = "com.social.two"
private const val NOTES = "com.notes.three"
private const val END = 10_000L

private fun active(
    allowed: Set<String>,
    endAtElapsed: Long = END,
) = UnlockState.Active(
    unlockId = "unlock-1",
    endAtElapsed = endAtElapsed,
    endAtWallClock = 1_700_000_000_000L,
    allowedPackages = allowed,
)

class RestrictionPolicyTest {
    private fun blocked(
        packageName: String,
        state: RestrictionState,
        now: Long,
    ) = RestrictionPolicy.isBlocked(packageName, state, now)

    @Test
    fun `an unrestricted package is never blocked while locked`() {
        val state = RestrictionState(restrictedPackages = setOf(GAME), unlock = UnlockState.Locked)

        assertFalse(blocked(NOTES, state, now = 0L))
    }

    @Test
    fun `an unrestricted package is never blocked even mid unlock`() {
        val state = RestrictionState(restrictedPackages = setOf(GAME), unlock = active(setOf(GAME)))

        assertFalse(blocked(NOTES, state, now = END - 1))
    }

    @Test
    fun `an unrestricted package is never blocked after expiry`() {
        val state = RestrictionState(restrictedPackages = setOf(GAME), unlock = UnlockState.Expired)

        assertFalse(blocked(NOTES, state, now = END + 5_000))
    }

    @Test
    fun `nothing is blocked when the restricted set is empty`() {
        val state = RestrictionState(restrictedPackages = emptySet(), unlock = UnlockState.Locked)

        assertFalse(blocked(GAME, state, now = 0L))
    }

    @Test
    fun `a restricted package is blocked while locked`() {
        val state = RestrictionState(restrictedPackages = setOf(GAME), unlock = UnlockState.Locked)

        assertTrue(blocked(GAME, state, now = 0L))
    }

    @Test
    fun `a restricted package is blocked while expired`() {
        val state = RestrictionState(restrictedPackages = setOf(GAME), unlock = UnlockState.Expired)

        assertTrue(blocked(GAME, state, now = 0L))
    }

    @Test
    fun `an allowed package is not blocked during an active unlock`() {
        val state = RestrictionState(restrictedPackages = setOf(GAME), unlock = active(setOf(GAME)))

        assertFalse(blocked(GAME, state, now = END - 1))
    }

    @Test
    fun `just before expiry the package is still allowed`() {
        val state = RestrictionState(restrictedPackages = setOf(GAME), unlock = active(setOf(GAME)))

        assertFalse(blocked(GAME, state, now = END - 1))
    }

    @Test
    fun `at exact expiry the package is blocked`() {
        val state = RestrictionState(restrictedPackages = setOf(GAME), unlock = active(setOf(GAME)))

        assertTrue(blocked(GAME, state, now = END))
    }

    @Test
    fun `just after expiry the package is blocked`() {
        val state = RestrictionState(restrictedPackages = setOf(GAME), unlock = active(setOf(GAME)))

        assertTrue(blocked(GAME, state, now = END + 1))
    }

    @Test
    fun `long after expiry the package is blocked`() {
        val state = RestrictionState(restrictedPackages = setOf(GAME), unlock = active(setOf(GAME)))

        assertTrue(blocked(GAME, state, now = END + 3_600_000))
    }

    @Test
    fun `an active unlock does not cover a package outside its allowed set`() {
        val state =
            RestrictionState(
                restrictedPackages = setOf(GAME, SOCIAL),
                unlock = active(setOf(GAME)),
            )

        assertFalse(blocked(GAME, state, now = END - 1))
        assertTrue(blocked(SOCIAL, state, now = END - 1))
    }

    @Test
    fun `an active unlock with no allowed packages blocks everything restricted`() {
        val state = RestrictionState(restrictedPackages = setOf(GAME), unlock = active(emptySet()))

        assertTrue(blocked(GAME, state, now = END - 1))
    }

    @Test
    fun `an unlock that allows a package never restricted changes nothing`() {
        val state = RestrictionState(restrictedPackages = setOf(GAME), unlock = active(setOf(NOTES)))

        assertTrue(blocked(GAME, state, now = END - 1))
        assertFalse(blocked(NOTES, state, now = END - 1))
    }

    @Test
    fun `a zero length unlock blocks from its first tick`() {
        val state =
            RestrictionState(
                restrictedPackages = setOf(GAME),
                unlock = active(setOf(GAME), endAtElapsed = 0L),
            )

        assertTrue(blocked(GAME, state, now = 0L))
    }

    @Test
    fun `wall clock is not consulted when deciding`() {
        val skewed =
            UnlockState.Active(
                unlockId = "unlock-1",
                endAtElapsed = END,
                endAtWallClock = 0L,
                allowedPackages = setOf(GAME),
            )
        val state = RestrictionState(restrictedPackages = setOf(GAME), unlock = skewed)

        assertFalse(blocked(GAME, state, now = END - 1))
        assertTrue(blocked(GAME, state, now = END))
    }

    @Test
    fun `every restricted package is judged independently`() {
        val state =
            RestrictionState(
                restrictedPackages = setOf(GAME, SOCIAL, NOTES),
                unlock = active(setOf(GAME, NOTES)),
            )

        assertFalse(blocked(GAME, state, now = END - 1))
        assertTrue(blocked(SOCIAL, state, now = END - 1))
        assertFalse(blocked(NOTES, state, now = END - 1))
    }

    @Test
    fun `expiry flips every allowed package at the same instant`() {
        val state =
            RestrictionState(
                restrictedPackages = setOf(GAME, NOTES),
                unlock = active(setOf(GAME, NOTES)),
            )

        assertFalse(blocked(GAME, state, now = END - 1))
        assertFalse(blocked(NOTES, state, now = END - 1))
        assertTrue(blocked(GAME, state, now = END))
        assertTrue(blocked(NOTES, state, now = END))
    }
}

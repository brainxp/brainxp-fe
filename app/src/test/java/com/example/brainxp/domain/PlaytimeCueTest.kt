package com.example.brainxp.domain

import com.example.brainxp.domain.model.UnlockState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val LEAD = 60
private const val OFFER_AT = 30 * 60
private const val WRAP_UP_AT = 5 * 60

private fun minutes(count: Int) = count * 60

class PlaytimeCueTest {
    @Test
    fun `the offer fires when the balance drops past half an hour`() {
        val cue = cueCrossed(budgetSeconds = minutes(60), before = OFFER_AT + 1, after = OFFER_AT, lastCallSeconds = LEAD)

        assertEquals(PlaytimeCue.OFFER, cue)
    }

    @Test
    fun `a balance that starts at exactly half an hour never gets the offer`() {
        val cue = cueCrossed(budgetSeconds = OFFER_AT, before = OFFER_AT, after = OFFER_AT - 1, lastCallSeconds = LEAD)

        assertNull(cue)
    }

    @Test
    fun `a thirty five minute balance still gets the offer even though it lands early`() {
        val cue = cueCrossed(budgetSeconds = minutes(35), before = OFFER_AT + 2, after = OFFER_AT - 1, lastCallSeconds = LEAD)

        assertEquals(PlaytimeCue.OFFER, cue)
    }

    @Test
    fun `the wrap up warning fires on a long session`() {
        val cue = cueCrossed(budgetSeconds = minutes(30), before = WRAP_UP_AT + 1, after = WRAP_UP_AT, lastCallSeconds = LEAD)

        assertEquals(PlaytimeCue.WRAP_UP, cue)
    }

    @Test
    fun `a session too short to be worth warning about skips the wrap up`() {
        val cue = cueCrossed(budgetSeconds = minutes(9), before = WRAP_UP_AT + 1, after = WRAP_UP_AT, lastCallSeconds = LEAD)

        assertNull(cue)
    }

    @Test
    fun `ten minutes is enough to arm the wrap up warning`() {
        val cue = cueCrossed(budgetSeconds = minutes(10), before = WRAP_UP_AT + 1, after = WRAP_UP_AT, lastCallSeconds = LEAD)

        assertEquals(PlaytimeCue.WRAP_UP, cue)
    }

    @Test
    fun `a threshold that was already behind us is not crossed again`() {
        val cue = cueCrossed(budgetSeconds = minutes(30), before = 250, after = 240, lastCallSeconds = LEAD)

        assertNull(cue)
    }

    @Test
    fun `the last call is armed even on a short session`() {
        val cue = cueCrossed(budgetSeconds = minutes(5), before = LEAD + 1, after = LEAD, lastCallSeconds = LEAD)

        assertEquals(PlaytimeCue.LAST_CALL, cue)
    }

    @Test
    fun `nothing is announced once the balance is spent`() {
        val cue = cueCrossed(budgetSeconds = minutes(30), before = 1, after = 0, lastCallSeconds = LEAD)

        assertNull(cue)
    }

    @Test
    fun `the most urgent cue wins when one tick clears two thresholds`() {
        val cue = cueCrossed(budgetSeconds = minutes(60), before = OFFER_AT + 1, after = LEAD, lastCallSeconds = LEAD)

        assertEquals(PlaytimeCue.LAST_CALL, cue)
    }
}

private fun active(
    budgetSeconds: Int,
    remainingSeconds: Int,
    id: String = "unlock-1",
) = UnlockState.Active(
    unlockId = id,
    budgetMillis = budgetSeconds * 1000L,
    consumedByPackage = mapOf("com.example.game" to (budgetSeconds - remainingSeconds) * 1000L),
)

class PlaytimeCueTrackerTest {
    @Test
    fun `the first reading only arms the tracker and announces nothing`() {
        val tracker = PlaytimeCueTracker()

        val cue = tracker.cueFor(active(budgetSeconds = minutes(60), remainingSeconds = minutes(60)), LEAD)

        assertNull(cue)
    }

    @Test
    fun `a cue is announced once and not repeated for the same session`() {
        val tracker = PlaytimeCueTracker()
        tracker.cueFor(active(budgetSeconds = minutes(60), remainingSeconds = OFFER_AT + 2), LEAD)

        val first = tracker.cueFor(active(budgetSeconds = minutes(60), remainingSeconds = OFFER_AT), LEAD)
        val second = tracker.cueFor(active(budgetSeconds = minutes(60), remainingSeconds = OFFER_AT - 1), LEAD)

        assertEquals(PlaytimeCue.OFFER, first)
        assertNull(second)
    }

    @Test
    fun `a fresh session arms the cues again`() {
        val tracker = PlaytimeCueTracker()
        tracker.cueFor(active(budgetSeconds = minutes(60), remainingSeconds = OFFER_AT + 2), LEAD)
        tracker.cueFor(active(budgetSeconds = minutes(60), remainingSeconds = OFFER_AT), LEAD)

        tracker.cueFor(active(budgetSeconds = minutes(60), remainingSeconds = OFFER_AT + 2, id = "unlock-2"), LEAD)
        val cue = tracker.cueFor(active(budgetSeconds = minutes(60), remainingSeconds = OFFER_AT, id = "unlock-2"), LEAD)

        assertEquals(PlaytimeCue.OFFER, cue)
    }

    @Test
    fun `a service restarted below a threshold does not announce a stale cue`() {
        val tracker = PlaytimeCueTracker()

        tracker.cueFor(active(budgetSeconds = minutes(60), remainingSeconds = 200), LEAD)
        val cue = tracker.cueFor(active(budgetSeconds = minutes(60), remainingSeconds = 190), LEAD)

        assertNull(cue)
    }

    @Test
    fun `resetting forgets the session so the next one starts clean`() {
        val tracker = PlaytimeCueTracker()
        tracker.cueFor(active(budgetSeconds = minutes(60), remainingSeconds = OFFER_AT + 2), LEAD)
        tracker.cueFor(active(budgetSeconds = minutes(60), remainingSeconds = OFFER_AT), LEAD)
        tracker.reset()

        tracker.cueFor(active(budgetSeconds = minutes(60), remainingSeconds = OFFER_AT + 2), LEAD)
        val cue = tracker.cueFor(active(budgetSeconds = minutes(60), remainingSeconds = OFFER_AT), LEAD)

        assertEquals(PlaytimeCue.OFFER, cue)
    }
}

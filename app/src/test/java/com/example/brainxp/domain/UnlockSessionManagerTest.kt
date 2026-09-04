package com.example.brainxp.domain

import com.example.brainxp.core.result.AppResult
import com.example.brainxp.core.time.FakeAppClock
import com.example.brainxp.data.db.UnlockStatus
import com.example.brainxp.data.repo.ActivityLogRepository
import com.example.brainxp.data.repo.StoredUnlock
import com.example.brainxp.data.repo.UnlockRepository
import com.example.brainxp.domain.model.ActivityEvent
import com.example.brainxp.domain.model.ActivityKind
import com.example.brainxp.domain.model.UnlockState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

private const val GAME = "com.mobile.legends"
private const val MINUTE = 60_000L

private class FakeUnlockRepository : UnlockRepository {
    var stored: StoredUnlock? = null
    val ended = mutableListOf<Pair<String, UnlockStatus>>()

    override suspend fun activeUnlock(
        nowWallClock: Long,
        nowElapsed: Long,
    ): UnlockState = stored?.state ?: UnlockState.Locked

    override suspend fun loadActive(): StoredUnlock? = stored

    override suspend fun save(
        state: UnlockState.Active,
        bootWallClock: Long,
    ): AppResult<Unit> {
        stored = StoredUnlock(state, bootWallClock)
        return AppResult.Success(Unit)
    }

    override suspend fun markEnded(
        unlockId: String,
        status: UnlockStatus,
    ): AppResult<Unit> {
        ended += unlockId to status
        stored = null
        return AppResult.Success(Unit)
    }
}

private class RecordingActivityLog : ActivityLogRepository {
    val events = mutableListOf<ActivityEvent>()

    override fun observeRecent(limit: Int): Flow<List<ActivityEvent>> = flowOf(events.toList())

    override suspend fun record(event: ActivityEvent): AppResult<Unit> {
        events += event
        return AppResult.Success(Unit)
    }

    override suspend fun flushPending(): AppResult<Int> = AppResult.Success(0)
}

class UnlockSessionManagerTest {
    private val clock = FakeAppClock()
    private val repository = FakeUnlockRepository()
    private val log = RecordingActivityLog()
    private val manager = UnlockSessionManager(clock, repository, log)

    private fun kinds() = log.events.map { it.kind }

    @Test
    fun `starting a session stores both timestamps`() =
        runTest {
            val state = manager.start(durationSeconds = 300, allowedPackages = setOf(GAME)) as UnlockState.Active

            assertEquals(clock.elapsed + 5 * MINUTE, state.endAtElapsed)
            assertEquals(clock.wall + 5 * MINUTE, state.endAtWallClock)
            assertEquals(setOf(GAME), state.allowedPackages)
        }

    @Test
    fun `starting a session logs unlock started`() =
        runTest {
            manager.start(300, setOf(GAME))

            assertEquals(listOf(ActivityKind.UNLOCK_STARTED), kinds())
        }

    @Test
    fun `remaining is recomputed from the stored end and never accumulated`() =
        runTest {
            manager.start(300, setOf(GAME))

            assertEquals(5.minutes, manager.remaining())
            clock.advance(2 * MINUTE)
            assertEquals(3.minutes, manager.remaining())
            clock.advance(3 * MINUTE)
            assertEquals(kotlin.time.Duration.ZERO, manager.remaining())
        }

    @Test
    fun `an allowed package is permitted while the session runs`() =
        runTest {
            manager.start(300, setOf(GAME))

            assertTrue(manager.allows(GAME))
            assertFalse(manager.allows("com.other.app"))
        }

    @Test
    fun `an allowed package stops being permitted at expiry`() =
        runTest {
            manager.start(300, setOf(GAME))
            clock.advance(5 * MINUTE)

            assertFalse(manager.allows(GAME))
        }

    @Test
    fun `the session expires on the tick after its end`() =
        runTest {
            manager.start(300, setOf(GAME))
            clock.advance(4 * MINUTE)
            assertTrue(manager.evaluate() is UnlockState.Active)

            clock.advance(MINUTE)

            assertEquals(UnlockState.Expired, manager.evaluate())
            assertTrue(kinds().contains(ActivityKind.UNLOCK_ENDED))
        }

    @Test
    fun `expiry marks the stored session expired`() =
        runTest {
            manager.start(300, setOf(GAME))
            clock.advance(6 * MINUTE)

            manager.evaluate()

            assertEquals(UnlockStatus.EXPIRED, repository.ended.single().second)
        }

    @Test
    fun `ending early marks the session ended and logs it`() =
        runTest {
            manager.start(300, setOf(GAME))

            assertEquals(UnlockState.Expired, manager.endEarly())

            assertEquals(UnlockStatus.ENDED, repository.ended.single().second)
            assertTrue(kinds().contains(ActivityKind.UNLOCK_ENDED))
        }

    @Test
    fun `a reboot rebases remaining time from the wall clock`() =
        runTest {
            manager.start(600, setOf(GAME))
            clock.advance(4 * MINUTE)
            clock.reboot(downtimeMillis = MINUTE)

            val recovered = manager.refresh() as UnlockState.Active

            assertEquals(5 * MINUTE, recovered.endAtElapsed - clock.elapsed)
            assertTrue(manager.allows(GAME))
        }

    @Test
    fun `a reboot after the session would have ended expires it`() =
        runTest {
            manager.start(300, setOf(GAME))
            clock.advance(MINUTE)
            clock.reboot(downtimeMillis = 10 * MINUTE)

            assertEquals(UnlockState.Expired, manager.refresh())
            assertFalse(manager.allows(GAME))
        }

    @Test
    fun `the stale elapsed end from before the reboot is not trusted`() =
        runTest {
            manager.start(600, setOf(GAME))
            clock.advance(9 * MINUTE)
            val staleEnd = (repository.stored!!.state).endAtElapsed
            clock.reboot(downtimeMillis = 0L)

            val recovered = manager.refresh() as UnlockState.Active

            assertTrue(recovered.endAtElapsed < staleEnd)
            assertEquals(MINUTE, recovered.endAtElapsed - clock.elapsed)
        }

    @Test
    fun `moving the wall clock forward past tolerance is treated as tampering`() =
        runTest {
            manager.start(600, setOf(GAME))
            manager.evaluate()

            clock.moveWallClock(5 * MINUTE)

            assertEquals(UnlockState.Expired, manager.evaluate())
            assertTrue(kinds().contains(ActivityKind.PROTECTION_ANOMALY))
        }

    @Test
    fun `moving the wall clock backward past tolerance is treated as tampering`() =
        runTest {
            manager.start(600, setOf(GAME))
            manager.evaluate()

            clock.moveWallClock(-5 * MINUTE)

            assertEquals(UnlockState.Expired, manager.evaluate())
            assertTrue(kinds().contains(ActivityKind.PROTECTION_ANOMALY))
        }

    @Test
    fun `a clock nudge inside tolerance is not tampering`() =
        runTest {
            manager.start(600, setOf(GAME))
            manager.evaluate()

            clock.moveWallClock(30_000L)

            assertTrue(manager.evaluate() is UnlockState.Active)
            assertFalse(kinds().contains(ActivityKind.PROTECTION_ANOMALY))
        }

    @Test
    fun `tampering marks the session ended rather than expired`() =
        runTest {
            manager.start(600, setOf(GAME))
            manager.evaluate()
            clock.moveWallClock(5 * MINUTE)

            manager.evaluate()

            assertEquals(UnlockStatus.ENDED, repository.ended.single().second)
        }

    @Test
    fun `a backward clock move while the process was dead is tampering`() =
        runTest {
            manager.start(600, setOf(GAME))
            clock.moveWallClock(-5 * MINUTE)

            assertEquals(UnlockState.Expired, manager.refresh())
            assertTrue(kinds().contains(ActivityKind.PROTECTION_ANOMALY))
        }

    @Test
    fun `refresh with no stored session reports locked`() =
        runTest {
            assertEquals(UnlockState.Locked, manager.refresh())
            assertEquals(kotlin.time.Duration.ZERO, manager.remaining())
        }

    @Test
    fun `nothing is allowed while locked`() =
        runTest {
            assertFalse(manager.allows(GAME))
        }
}

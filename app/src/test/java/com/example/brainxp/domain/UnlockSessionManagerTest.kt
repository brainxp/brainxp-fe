package com.example.brainxp.domain

import com.example.brainxp.core.result.AppResult
import com.example.brainxp.core.time.FakeAppClock
import com.example.brainxp.data.db.UnlockStatus
import com.example.brainxp.data.repo.ActivityLogRepository
import com.example.brainxp.data.repo.ConsumptionReporter
import com.example.brainxp.data.repo.ReconciledBalance
import com.example.brainxp.data.repo.UnlockRepository
import com.example.brainxp.domain.model.ActivityEvent
import com.example.brainxp.domain.model.ActivityKind
import com.example.brainxp.domain.model.UnlockState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val GAME = "com.mobile.legends"
private const val CHAT = "com.chat.app"
private const val NOTES = "com.notes.app"
private const val TICK = 600L

private class FakeUnlockRepository : UnlockRepository {
    var stored: UnlockState.Active? = null
    val saves = mutableListOf<Pair<UnlockState.Active, UnlockStatus>>()

    override suspend fun loadActive(): UnlockState.Active? = stored

    override suspend fun save(
        state: UnlockState.Active,
        status: UnlockStatus,
    ): AppResult<Unit> {
        saves += state to status
        stored = if (status == UnlockStatus.ACTIVE) state else null
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

private class RecordingReporter : ConsumptionReporter {
    val reports = mutableListOf<Map<String, Int>>()

    override suspend fun report(secondsByPackage: Map<String, Int>): AppResult<ReconciledBalance> {
        reports += secondsByPackage
        return AppResult.Success(ReconciledBalance())
    }
}

class UnlockSessionManagerTest {
    private val clock = FakeAppClock()
    private val repository = FakeUnlockRepository()
    private val log = RecordingActivityLog()
    private val reporter = RecordingReporter()
    private val manager = UnlockSessionManager(clock, repository, log, reporter)

    private suspend fun tick(
        foreground: String?,
        times: Int = 1,
    ) {
        repeat(times) {
            clock.advance(TICK)
            manager.meter(foreground)
        }
    }

    @Test
    fun `a fresh session has its whole budget and nothing consumed`() =
        runTest {
            val state = manager.start(300, setOf(GAME)) as UnlockState.Active

            assertEquals(300_000L, state.budgetMillis)
            assertEquals(0L, state.consumedMillis)
            assertEquals(5.minutes, manager.remaining())
        }

    @Test
    fun `time does not drain while another app is in the foreground`() =
        runTest {
            manager.start(60, setOf(GAME))

            tick(CHAT, times = 50)

            assertEquals(1.minutes, manager.remaining())
            assertTrue(manager.state.value is UnlockState.Active)
        }

    @Test
    fun `time does not drain while nothing is in the foreground`() =
        runTest {
            manager.start(60, setOf(GAME))

            tick(null, times = 50)

            assertEquals(1.minutes, manager.remaining())
        }

    @Test
    fun `time drains while the unlocked app is in the foreground`() =
        runTest {
            manager.start(60, setOf(GAME))

            tick(GAME, times = 11)

            assertEquals(54.seconds, manager.remaining())
        }

    @Test
    fun `the first tick after switching in only arms the meter`() =
        runTest {
            manager.start(60, setOf(GAME))

            tick(GAME)

            assertEquals(1.minutes, manager.remaining())
        }

    @Test
    fun `leaving the app and coming back does not count the time away`() =
        runTest {
            manager.start(60, setOf(GAME))

            tick(GAME, times = 6)
            val afterPlaying = manager.remaining()
            clock.advance(30 * 60 * 1_000L)
            manager.meter(CHAT)
            tick(GAME)

            assertEquals(afterPlaying, manager.remaining())
        }

    @Test
    fun `the session expires once the budget is spent`() =
        runTest {
            manager.start(3, setOf(GAME))

            tick(GAME, times = 20)

            assertEquals(UnlockState.Expired, manager.state.value)
            assertEquals(UnlockStatus.EXPIRED, repository.saves.last().second)
        }

    @Test
    fun `consumption never exceeds the budget`() =
        runTest {
            manager.start(3, setOf(GAME))

            tick(GAME, times = 20)

            assertEquals(
                3_000L,
                repository.saves
                    .last()
                    .first.consumedMillis,
            )
        }

    @Test
    fun `consumption is attributed to the app that was open`() =
        runTest {
            manager.start(600, setOf(GAME, CHAT))

            tick(GAME, times = 6)
            tick(CHAT, times = 6)

            val active = manager.state.value as UnlockState.Active
            assertEquals(3_000L, active.consumedByPackage[GAME])
            assertEquals(3_600L, active.consumedByPackage[CHAT])
            assertEquals(6_600L, active.consumedMillis)
        }

    @Test
    fun `an app outside the session is never metered`() =
        runTest {
            manager.start(600, setOf(GAME))

            tick(NOTES, times = 6)

            assertEquals(emptyMap<String, Long>(), (manager.state.value as UnlockState.Active).consumedByPackage)
        }

    @Test
    fun `expiring reports what was actually consumed`() =
        runTest {
            manager.start(3, setOf(GAME))

            tick(GAME, times = 20)

            assertEquals(listOf(mapOf(GAME to 3)), reporter.reports)
        }

    @Test
    fun `ending early reports only the consumed part`() =
        runTest {
            manager.start(600, setOf(GAME))

            tick(GAME, times = 11)
            manager.endEarly()

            assertEquals(listOf(mapOf(GAME to 6)), reporter.reports)
            assertEquals(UnlockStatus.ENDED, repository.saves.last().second)
        }

    @Test
    fun `ending a session that was never used reports nothing`() =
        runTest {
            manager.start(600, setOf(GAME))

            manager.endEarly()

            assertEquals(emptyList<Map<String, Int>>(), reporter.reports)
        }

    @Test
    fun `moving the system clock forward does not consume the budget`() =
        runTest {
            manager.start(60, setOf(GAME))

            tick(GAME, times = 6)
            clock.moveWallClock(2 * 60 * 60 * 1_000L)
            manager.meter(GAME)

            assertEquals(57.seconds, manager.remaining())
            assertTrue(manager.state.value is UnlockState.Active)
        }

    @Test
    fun `moving the system clock backward does not extend the budget`() =
        runTest {
            manager.start(60, setOf(GAME))

            tick(GAME, times = 6)
            clock.moveWallClock(-(60 * 60 * 1_000L))
            tick(GAME, times = 6)

            assertEquals(53_400.milliseconds, manager.remaining())
        }

    @Test
    fun `a reboot in the middle of a session does not count the downtime`() =
        runTest {
            manager.start(60, setOf(GAME))
            tick(GAME, times = 11)
            repository.stored = manager.state.value as UnlockState.Active

            clock.reboot(downtimeMillis = 10 * 60 * 1_000L)
            manager.refresh()
            tick(GAME)

            assertEquals(54.seconds, manager.remaining())
        }

    @Test
    fun `a stalled ticker counts no more than the cap`() =
        runTest {
            manager.start(600, setOf(GAME))

            tick(GAME)
            clock.advance(10 * 60 * 1_000L)
            manager.meter(GAME)

            assertEquals(595.seconds, manager.remaining())
        }

    @Test
    fun `refresh restores progress already made`() =
        runTest {
            manager.start(600, setOf(GAME))
            tick(GAME, times = 11)
            val saved = manager.state.value as UnlockState.Active
            repository.stored = saved

            val restored = manager.refresh() as UnlockState.Active

            assertEquals(saved.consumedMillis, restored.consumedMillis)
            assertEquals(594.seconds, manager.remaining())
        }

    @Test
    fun `refresh with no stored session reports locked`() =
        runTest {
            assertEquals(UnlockState.Locked, manager.refresh())
        }

    @Test
    fun `a stored session already spent is closed on refresh`() =
        runTest {
            repository.stored =
                UnlockState.Active(
                    unlockId = "spent",
                    budgetMillis = 1_000L,
                    consumedByPackage = mapOf(GAME to 1_000L),
                    allowedPackages = setOf(GAME),
                )

            assertEquals(UnlockState.Expired, manager.refresh())
        }

    @Test
    fun `metering an ended session changes nothing`() =
        runTest {
            manager.start(600, setOf(GAME))
            manager.endEarly()

            tick(GAME, times = 6)

            assertEquals(UnlockState.Expired, manager.state.value)
        }

    @Test
    fun `the activity log records the start and the end once each`() =
        runTest {
            manager.start(3, setOf(GAME))
            tick(GAME, times = 20)

            assertEquals(
                listOf(ActivityKind.UNLOCK_STARTED, ActivityKind.UNLOCK_ENDED),
                log.events.map { it.kind },
            )
        }
}

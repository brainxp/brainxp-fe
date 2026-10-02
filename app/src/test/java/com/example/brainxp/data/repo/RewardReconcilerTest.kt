package com.example.brainxp.data.repo

import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.core.time.FakeAppClock
import com.example.brainxp.data.db.PendingOperationDao
import com.example.brainxp.data.db.PendingOperationEntity
import com.example.brainxp.data.db.PendingOperationType
import com.example.brainxp.data.prefs.CachedBalance
import com.example.brainxp.data.prefs.RewardCache
import com.example.brainxp.domain.model.BlockReason
import com.example.brainxp.domain.model.ConsumptionEntry
import com.example.brainxp.domain.model.LedgerDirection
import com.example.brainxp.domain.model.LedgerEntry
import com.example.brainxp.domain.model.Progress
import com.example.brainxp.domain.model.Report
import com.example.brainxp.domain.model.Standing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.inject.Provider

private fun standing(balanceSeconds: Int) =
    Standing(
        balanceSeconds = balanceSeconds,
        playableSeconds = balanceSeconds,
        dailyCapSeconds = 5_400,
        spentTodaySeconds = 0,
        secondsUntilReset = 18_000,
        blockReason = BlockReason.NONE,
        streakCurrent = 0,
        freezeTokens = 0,
    )

private class StubAppLabels : AppLabels {
    override suspend fun label(packageName: String): String = packageName
}

private class StubRewardRepository : RewardRepository {
    var result: AppResult<Standing> = AppResult.Success(standing(0))
    val reported = mutableListOf<List<ConsumptionEntry>>()

    override suspend fun standing(): AppResult<Standing> = result

    override suspend fun progress(): AppResult<Progress> =
        AppResult.Success(
            Progress(
                streakCurrent = 0,
                streakLongest = 0,
                sessions = 0,
                correctTotal = 0,
                essayPassed = 0,
                freezeTokens = 0,
                badges = emptyList(),
            ),
        )

    override suspend fun reportConsumption(entries: List<ConsumptionEntry>): AppResult<Standing> {
        reported += entries
        return result
    }

    override suspend fun history(): AppResult<List<LedgerEntry>> = AppResult.Success(emptyList())

    override suspend fun standingOf(subjectId: String): AppResult<Standing> = result

    override suspend fun report(
        days: Int,
        subjectId: String?,
    ): AppResult<Report> =
        AppResult.Success(
            Report(
                standing = standing(0),
                days = emptyList(),
                materialsStudied = 0,
                correctTotal = 0,
                essayPassed = 0,
                recent = emptyList(),
                guardianAlerts = emptyList(),
            ),
        )

    override suspend fun adjust(
        direction: LedgerDirection,
        seconds: Int,
        note: String,
        subjectId: String?,
    ): AppResult<Standing> = result
}

private class InMemoryPendingOperationDao : PendingOperationDao {
    private var nextId = 1L
    private val rows = linkedMapOf<Long, PendingOperationEntity>()

    override suspend fun insert(operation: PendingOperationEntity): Long {
        val id = nextId++
        rows[id] = operation.copy(id = id)
        return id
    }

    override suspend fun findReadyForRetry(
        now: Long,
        limit: Int,
    ): List<PendingOperationEntity> = rows.values.filter { it.nextAttemptAt <= now }.take(limit)

    override suspend fun findReadyForRetryByType(
        type: PendingOperationType,
        now: Long,
    ): List<PendingOperationEntity> = rows.values.filter { it.type == type && it.nextAttemptAt <= now }

    override suspend fun recordAttempt(
        id: Long,
        nextAttemptAt: Long,
    ) {
        rows[id]?.let { rows[id] = it.copy(attempts = it.attempts + 1, nextAttemptAt = nextAttemptAt) }
    }

    override suspend fun deleteById(id: Long) {
        rows.remove(id)
    }

    override suspend fun count(): Int = rows.size

    override suspend fun deleteExhausted(
        type: PendingOperationType,
        maxAttempts: Int,
    ) {
        rows.entries.removeAll { it.value.type == type && it.value.attempts >= maxAttempts }
    }
}

private class InMemoryRewardCache : RewardCache {
    private val flow = MutableStateFlow<CachedBalance?>(null)

    override val cached: Flow<CachedBalance?> = flow

    override suspend fun write(balance: CachedBalance) {
        flow.value = balance
    }

    fun seed(balance: CachedBalance?) {
        flow.value = balance
    }

    override suspend fun clear() {
        flow.value = null
    }
}

class RewardReconcilerTest {
    private val repository = StubRewardRepository()
    private val cache = InMemoryRewardCache()
    private val clock = FakeAppClock()
    private val pendingDao = InMemoryPendingOperationDao()
    private val pendingSync =
        PendingSyncQueue(
            rewards = Provider { repository },
            family = Provider { error("not used") },
            pending = pendingDao,
            clock = clock,
        )
    private val reconciler = RewardReconciler(repository, pendingSync, cache, clock, StubAppLabels())

    @Test
    fun `the server balance wins when it disagrees with the cache`() =
        runTest {
            cache.seed(CachedBalance(balanceSeconds = 9_000, updatedAtWallClock = 1L))
            repository.result = AppResult.Success(standing(600))

            val reconciled = reconciler.reconcile()

            assertEquals(600, reconciled.balanceSeconds)
            assertEquals(BalanceSource.SERVER, reconciled.source)
        }

    @Test
    fun `a smaller server balance also wins`() =
        runTest {
            cache.seed(CachedBalance(60, 1L))
            repository.result = AppResult.Success(standing(3_600))

            assertEquals(3_600, reconciler.reconcile().balanceSeconds)
        }

    @Test
    fun `a server balance of zero overrides a positive cache`() =
        runTest {
            cache.seed(CachedBalance(1_800, 1L))
            repository.result = AppResult.Success(standing(0))

            val reconciled = reconciler.reconcile()

            assertEquals(0, reconciled.balanceSeconds)
            assertFalse(reconciled.spendable)
        }

    @Test
    fun `the cache is overwritten with the server balance`() =
        runTest {
            cache.seed(CachedBalance(9_000, 1L))
            clock.wall = 5_000L
            repository.result = AppResult.Success(standing(600))

            reconciler.reconcile()

            assertEquals(CachedBalance(600, 5_000L), flowValue(cache.cached))
        }

    @Test
    fun `an offline start falls back to the cached balance and marks it stale`() =
        runTest {
            cache.seed(CachedBalance(1_500, 1L))
            repository.result = AppResult.Failure(ApiError.Network)

            val reconciled = reconciler.reconcile()

            assertEquals(1_500, reconciled.balanceSeconds)
            assertEquals(BalanceSource.CACHE, reconciled.source)
            assertTrue(reconciled.stale)
            assertEquals(ApiError.Network, reconciled.error)
        }

    @Test
    fun `a stale cached balance is still spendable`() =
        runTest {
            cache.seed(CachedBalance(1_500, 1L))
            repository.result = AppResult.Failure(ApiError.Network)

            assertTrue(reconciler.reconcile().spendable)
        }

    @Test
    fun `an offline start with no cache reports nothing rather than zero balance`() =
        runTest {
            repository.result = AppResult.Failure(ApiError.Network)

            val reconciled = reconciler.reconcile()

            assertEquals(BalanceSource.NONE, reconciled.source)
            assertEquals(0, reconciled.balanceSeconds)
            assertFalse(reconciled.spendable)
        }

    @Test
    fun `an offline start never invents a larger balance than the cache held`() =
        runTest {
            cache.seed(CachedBalance(300, 1L))
            repository.result = AppResult.Failure(ApiError.ServerBusy)

            assertEquals(300, reconciler.reconcile().balanceSeconds)
        }

    @Test
    fun `a failed reconcile leaves the cache untouched`() =
        runTest {
            cache.seed(CachedBalance(300, 1L))
            repository.result = AppResult.Failure(ApiError.Network)

            reconciler.reconcile()

            assertEquals(CachedBalance(300, 1L), flowValue(cache.cached))
        }

    @Test
    fun `the full standing is only exposed when it came from the server`() =
        runTest {
            repository.result = AppResult.Success(standing(600))
            assertEquals(600, reconciler.reconcile().standing?.balanceSeconds)

            repository.result = AppResult.Failure(ApiError.Network)
            assertNull(reconciler.reconcile().standing)
        }

    @Test
    fun `state mirrors the last reconcile`() =
        runTest {
            repository.result = AppResult.Success(standing(900))

            reconciler.reconcile()

            assertEquals(900, reconciler.state.value.balanceSeconds)
        }

    @Test
    fun `failed consumption stays deducted until its ledger row is accepted`() =
        runTest {
            repository.result = AppResult.Success(standing(600))
            reconciler.reconcile()
            repository.result = AppResult.Failure(ApiError.Network)

            reconciler.report(mapOf("com.game" to 60))
            val held = reconciler.reconcile()

            assertEquals(540, held.balanceSeconds)
            assertEquals(BalanceSource.CACHE, held.source)
            assertTrue(pendingSync.hasPendingConsumption())
        }

    @Test
    fun `a rejected consumption response cannot restore spent balance`() =
        runTest {
            repository.result = AppResult.Success(standing(600))
            reconciler.reconcile()
            repository.result = AppResult.Failure(ApiError.Validation(field = null, message = "rejected"))

            reconciler.report(mapOf("com.game" to 60))
            val held = reconciler.reconcile()

            assertEquals(540, held.balanceSeconds)
            assertTrue(pendingSync.hasPendingConsumption())
        }

    @Test
    fun `ledger retry preserves the original client event id`() =
        runTest {
            repository.result = AppResult.Success(standing(600))
            reconciler.reconcile()
            repository.result = AppResult.Failure(ApiError.Network)
            reconciler.report(mapOf("com.game" to 60))
            val firstId =
                repository.reported
                    .last()
                    .single()
                    .clientEventId
            clock.advance(15_000)
            repository.result = AppResult.Success(standing(540))

            val reconciled = reconciler.reconcile()

            assertEquals(
                firstId,
                repository.reported
                    .last()
                    .single()
                    .clientEventId,
            )
            assertEquals(540, reconciled.balanceSeconds)
            assertFalse(pendingSync.hasPendingConsumption())
        }

    private suspend fun flowValue(flow: Flow<CachedBalance?>): CachedBalance? = flow.first()
}

package com.example.brainxp.data.repo

import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.core.time.FakeAppClock
import com.example.brainxp.data.prefs.CachedBalance
import com.example.brainxp.data.prefs.RewardCache
import com.example.brainxp.domain.model.BlockReason
import com.example.brainxp.domain.model.ConsumptionEntry
import com.example.brainxp.domain.model.LedgerDirection
import com.example.brainxp.domain.model.LedgerEntry
import com.example.brainxp.domain.model.Progress
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

private fun standing(balanceSeconds: Int) =
    Standing(
        balanceSeconds = balanceSeconds,
        playableSeconds = balanceSeconds,
        ceilingSeconds = 7_200,
        dailyCapSeconds = 5_400,
        spentTodaySeconds = 0,
        secondsUntilReset = 18_000,
        blockReason = BlockReason.NONE,
        streakCurrent = 0,
        points = 0,
        freezeTokens = 0,
    )

private class StubRewardRepository : RewardRepository {
    var result: AppResult<Standing> = AppResult.Success(standing(0))

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

    override suspend fun reportConsumption(entries: List<ConsumptionEntry>): AppResult<Standing> = result

    override suspend fun history(): AppResult<List<LedgerEntry>> = AppResult.Success(emptyList())

    override suspend fun adjust(
        direction: LedgerDirection,
        seconds: Int,
        note: String,
    ): AppResult<Standing> = result
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
}

class RewardReconcilerTest {
    private val repository = StubRewardRepository()
    private val cache = InMemoryRewardCache()
    private val clock = FakeAppClock()
    private val reconciler = RewardReconciler(repository, cache, clock)

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

    private suspend fun flowValue(flow: Flow<CachedBalance?>): CachedBalance? = flow.first()
}

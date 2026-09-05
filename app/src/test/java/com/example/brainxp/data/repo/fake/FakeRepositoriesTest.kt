package com.example.brainxp.data.repo.fake

import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.domain.model.ActivityEvent
import com.example.brainxp.domain.model.ActivityKind
import com.example.brainxp.domain.model.BlockReason
import com.example.brainxp.domain.model.ConsumptionEntry
import com.example.brainxp.domain.model.LedgerDirection
import com.example.brainxp.domain.model.MaterialType
import com.example.brainxp.domain.model.Question
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeRepositoriesTest {
    private lateinit var backend: FakeBackend
    private lateinit var materials: FakeMaterialRepository
    private lateinit var rewards: FakeRewardRepository
    private lateinit var activity: FakeActivityLogRepository
    private lateinit var family: FakeFamilyRepository

    @Before
    fun setUp() {
        backend = FakeBackend().apply { latencyMillis = FakeBackend.INSTANT }
        materials = FakeMaterialRepository(backend)
        rewards = FakeRewardRepository(backend)
        activity = FakeActivityLogRepository(backend)
        family = FakeFamilyRepository(backend)
    }

    private fun <T> success(result: AppResult<T>): T {
        assertTrue("expected success, got $result", result is AppResult.Success)
        return (result as AppResult.Success).value
    }

    private fun <T> failure(result: AppResult<T>): ApiError {
        assertTrue("expected failure, got $result", result is AppResult.Failure)
        return (result as AppResult.Failure).error
    }

    @Test
    fun uploadAddsMaterialToTheCacheAndCanFail() =
        runTest {
            val before = materials.observeCached().first().size

            val created = success(materials.upload("Bab 5", MaterialType.TEXT, null))
            assertEquals(before + 1, materials.observeCached().first().size)
            assertEquals("Bab 5", created.title)

            backend.failNext(FakeBackend.MATERIAL_UPLOAD, ApiError.Network)
            assertEquals(ApiError.Network, failure(materials.upload("Bab 6", MaterialType.TEXT, null)))
        }

    @Test
    fun materialPagingWalksTheCursorAndCanFail() =
        runTest {
            val first = success(materials.page(null))
            assertNotNull(first.nextCursor)

            val second = success(materials.page(first.nextCursor))
            assertTrue(second.items.isNotEmpty())
            assertTrue(first.items.none { item -> second.items.any { it.id == item.id } })

            backend.failNext(FakeBackend.MATERIAL_PAGE, ApiError.ServerBusy)
            assertEquals(ApiError.ServerBusy, failure(materials.page(null)))
        }

    @Test
    fun materialDetailReturnsTheMaterialAndFailsForUnknownId() =
        runTest {
            val detail = success(materials.detail("mat-1"))
            assertEquals("mat-1", detail.id)

            assertTrue(failure(materials.detail("nope")) is ApiError.Unknown)

            backend.failNext(FakeBackend.MATERIAL_DETAIL, ApiError.Network)
            assertEquals(ApiError.Network, failure(materials.detail("mat-1")))
        }

    @Test
    fun standingReportsPlayableSecondsAndCanFail() =
        runTest {
            val standing = success(rewards.standing())

            assertTrue(standing.balanceSeconds > 0)
            assertTrue(standing.playableSeconds > 0)
            assertEquals(BlockReason.NONE, standing.blockReason)
            assertTrue(standing.playable)

            backend.failNext(FakeBackend.REWARD_STANDING, ApiError.Network)
            assertEquals(ApiError.Network, failure(rewards.standing()))
        }

    @Test
    fun reportedConsumptionReducesBalanceAndCountsTowardTheDailyCap() =
        runTest {
            val before = success(rewards.standing())

            val after =
                success(
                    rewards.reportConsumption(
                        listOf(ConsumptionEntry("evt-1", "YouTube", 300, null)),
                    ),
                )

            assertEquals(before.balanceSeconds - 300, after.balanceSeconds)
            assertEquals(300, after.spentTodaySeconds)
        }

    @Test
    fun replayingTheSameClientEventIdIsIgnored() =
        runTest {
            val entry = ConsumptionEntry("evt-dup", "YouTube", 120, null)

            val first = success(rewards.reportConsumption(listOf(entry)))
            val second = success(rewards.reportConsumption(listOf(entry)))

            assertEquals(first.balanceSeconds, second.balanceSeconds)
            assertEquals(first.spentTodaySeconds, second.spentTodaySeconds)
        }

    @Test
    fun consumptionRejectsNegativeSecondsAndCanFail() =
        runTest {
            assertTrue(
                failure(
                    rewards.reportConsumption(
                        listOf(ConsumptionEntry("evt-bad", "YouTube", -5, null)),
                    ),
                ) is ApiError.Validation,
            )

            backend.failNext(FakeBackend.LEDGER_SYNC, ApiError.ServerBusy)
            assertEquals(
                ApiError.ServerBusy,
                failure(
                    rewards.reportConsumption(
                        listOf(ConsumptionEntry("evt-2", "YouTube", 10, null)),
                    ),
                ),
            )
        }

    @Test
    fun spendingEverythingBlocksWithNoBalance() =
        runTest {
            val standing = success(rewards.standing())
            success(
                rewards.reportConsumption(
                    listOf(ConsumptionEntry("evt-all", "YouTube", standing.balanceSeconds, null)),
                ),
            )

            val after = success(rewards.standing())
            assertEquals(0, after.playableSeconds)
            assertEquals(BlockReason.NO_BALANCE, after.blockReason)
            assertFalse(after.playable)
        }

    @Test
    fun staleGuardianBlocksRegardlessOfBalance() =
        runTest {
            rewards.guardianStale = true

            val standing = success(rewards.standing())

            assertTrue(standing.balanceSeconds > 0)
            assertEquals(BlockReason.GUARDIAN_STALE, standing.blockReason)
            assertFalse(standing.playable)
        }

    @Test
    fun parentGrantAndRedeemMoveTheBalanceAndAreValidated() =
        runTest {
            val before = success(rewards.standing()).balanceSeconds

            val granted = success(rewards.adjust(LedgerDirection.GRANT, 600, "bonus"))
            assertEquals(before + 600, granted.balanceSeconds)

            val redeemed = success(rewards.adjust(LedgerDirection.REDEEM, 100, "koreksi"))
            assertEquals(granted.balanceSeconds - 100, redeemed.balanceSeconds)

            assertTrue(failure(rewards.adjust(LedgerDirection.GRANT, 0, "nol")) is ApiError.Validation)
            assertTrue(
                failure(rewards.adjust(LedgerDirection.REDEEM, 999_999, "terlalu besar"))
                    is ApiError.Validation,
            )

            backend.failNext(FakeBackend.LEDGER_ADJUST, ApiError.Network)
            assertEquals(ApiError.Network, failure(rewards.adjust(LedgerDirection.GRANT, 60, "x")))
        }

    @Test
    fun ledgerHistoryRecordsEveryMovementAndCanFail() =
        runTest {
            success(rewards.reportConsumption(listOf(ConsumptionEntry("evt-h", "YouTube", 60, null))))
            success(rewards.adjust(LedgerDirection.GRANT, 120, "bonus"))

            val history = success(rewards.history())

            assertEquals(2, history.size)
            assertTrue(history.any { it.deltaSeconds < 0 })
            assertTrue(history.any { it.deltaSeconds > 0 })

            backend.failNext(FakeBackend.LEDGER_HISTORY, ApiError.ServerBusy)
            assertEquals(ApiError.ServerBusy, failure(rewards.history()))
        }

    @Test
    fun activityLogRecordsNewestFirstAndFlushesPending() =
        runTest {
            success(activity.record(ActivityEvent(ActivityKind.UNLOCK_STARTED, Long.MAX_VALUE)))

            val recent = activity.observeRecent(2).first()
            assertEquals(ActivityKind.UNLOCK_STARTED, recent.first().kind)
            assertEquals(2, recent.size)

            assertEquals(1, success(activity.flushPending()))
            assertEquals(0, success(activity.flushPending()))

            backend.failNext(FakeBackend.ACTIVITY_RECORD, ApiError.Network)
            assertEquals(
                ApiError.Network,
                failure(activity.record(ActivityEvent(ActivityKind.REWARD_EARNED, 0))),
            )

            backend.failNext(FakeBackend.ACTIVITY_FLUSH, ApiError.ServerBusy)
            assertEquals(ApiError.ServerBusy, failure(activity.flushPending()))
        }

    @Test
    fun familyListingPairingAndConfigAllHaveFailurePaths() =
        runTest {
            val children = success(family.children())
            assertTrue(children.isNotEmpty())

            assertTrue(failure(family.pair("12")) is ApiError.Validation)
            assertTrue(failure(family.pair("000000")) is ApiError.Validation)

            val paired = success(family.pair("123456"))
            assertEquals(children.size + 1, success(family.children()).size)

            val config = FakeData.defaultChildConfig()
            assertEquals(config, success(family.updateConfig(paired.childId, config)))
            assertTrue(failure(family.updateConfig("child-missing", config)) is ApiError.Unknown)
            assertTrue(
                failure(family.updateConfig(paired.childId, config.copy(dailyCapMinutes = 0)))
                    is ApiError.Validation,
            )

            backend.failNext(FakeBackend.FAMILY_CHILDREN, ApiError.Network)
            assertEquals(ApiError.Network, failure(family.children()))
        }

    @Test
    fun latencyIsAppliedAndCallsAreCounted() =
        runTest {
            backend.latencyMillis = 50L..50L

            success(rewards.standing())
            success(rewards.standing())

            assertEquals(2, backend.callsTo(FakeBackend.REWARD_STANDING))
        }
}

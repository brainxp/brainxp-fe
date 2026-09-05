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
    private lateinit var family: FakeFamilyRepository

    @Before
    fun setUp() {
        backend = FakeBackend().apply { latencyMillis = FakeBackend.INSTANT }
        materials = FakeMaterialRepository(backend)
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
}

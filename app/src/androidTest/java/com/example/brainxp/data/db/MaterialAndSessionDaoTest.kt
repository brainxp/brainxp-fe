package com.example.brainxp.data.db

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MaterialAndSessionDaoTest : DbTest() {
    @Test
    fun materialsAreObservedNewestFirst() =
        runTest {
            seedMaterial("m-old", createdAt = 100)
            seedMaterial("m-new", createdAt = 300)
            seedMaterial("m-mid", createdAt = 200)

            val titles =
                db
                    .materialDao()
                    .observeAll()
                    .first()
                    .map { it.id }

            assertEquals(listOf("m-new", "m-mid", "m-old"), titles)
        }

    @Test
    fun upsertReplacesExistingMaterialRatherThanDuplicating() =
        runTest {
            seedMaterial("m1", title = "first")
            seedMaterial("m1", title = "second")

            assertEquals(1, db.materialDao().count())
            assertEquals("second", db.materialDao().findById("m1")?.title)
        }

    @Test
    fun findByStatusFiltersAndOrders() =
        runTest {
            seedMaterial("m1", status = "PROCESSING", createdAt = 100)
            seedMaterial("m2", status = "READY", createdAt = 200)
            seedMaterial("m3", status = "READY", createdAt = 300)

            val ready = db.materialDao().findByStatus("READY").map { it.id }

            assertEquals(listOf("m3", "m2"), ready)
        }

    @Test
    fun sessionsForAGivenMaterialAreScopedAndNewestFirst() =
        runTest {
            seedMaterial("m1")
            seedMaterial("m2")
            seedSession("s1", "m1", createdAt = 100)
            seedSession("s2", "m1", createdAt = 300)
            seedSession("s3", "m2", createdAt = 200)

            val forM1 = db.questionSessionDao().findForMaterial("m1").map { it.id }

            assertEquals(listOf("s2", "s1"), forM1)
        }

    @Test
    fun observingSessionsForMaterialEmitsOnlyThatMaterial() =
        runTest {
            seedMaterial("m1")
            seedMaterial("m2")
            seedSession("s1", "m1")
            seedSession("s2", "m2")

            val observed = db.questionSessionDao().observeForMaterial("m2").first()

            assertEquals(listOf("s2"), observed.map { it.id })
        }

    @Test
    fun deletingMaterialCascadesToItsSessions() =
        runTest {
            seedMaterial("m1")
            seedSession("s1", "m1")
            seedSession("s2", "m1")

            db.materialDao().deleteById("m1")

            assertEquals(emptyList<String>(), db.questionSessionDao().findForMaterial("m1").map { it.id })
        }

    @Test
    fun recordResultStoresScoreAndRewardFromServer() =
        runTest {
            seedMaterial("m1")
            seedSession("s1", "m1")

            db.questionSessionDao().recordResult("s1", "COMPLETED", score = 0.8, rewardSeconds = 900)

            val session = db.questionSessionDao().findById("s1")
            assertEquals("COMPLETED", session?.status)
            assertEquals(0.8, session?.score)
            assertEquals(900, session?.rewardSeconds)
        }

    @Test
    fun recentSessionsAreLimitedAndOrdered() =
        runTest {
            seedMaterial("m1")
            (1..5).forEach { seedSession("s$it", "m1", createdAt = it * 100L) }

            val recent = db.questionSessionDao().findRecent(2).map { it.id }

            assertEquals(listOf("s5", "s4"), recent)
        }

    @Test
    fun unfinishedSessionsCanBeFoundForResume() =
        runTest {
            seedMaterial("m1")
            seedSession("s1", "m1", status = "COMPLETED", createdAt = 100)
            seedSession("s2", "m1", status = "IN_PROGRESS", createdAt = 200)

            val open = db.questionSessionDao().findByStatus("IN_PROGRESS").map { it.id }

            assertEquals(listOf("s2"), open)
        }

    @Test
    fun missingMaterialReturnsNull() =
        runTest {
            assertNull(db.materialDao().findById("nope"))
        }
}

package com.example.brainxp.data.db

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnlockAndRestrictionDaoTest : DbTest() {
    private suspend fun unlock(
        id: String,
        status: UnlockStatus,
        allowed: List<String> = listOf("com.google.android.youtube"),
        endAtElapsed: Long = 60_000L,
    ) {
        db.unlockSessionDao().upsert(
            UnlockSessionEntity(
                id = id,
                endAtElapsed = endAtElapsed,
                endAtWallClock = 1_700_000_000_000L,
                bootWallClock = 1_699_999_000_000L,
                allowedPackages = allowed,
                status = status,
            ),
        )
    }

    @Test
    fun onlyTheActiveUnlockIsFoundAfterProcessDeath() =
        runTest {
            unlock("u1", UnlockStatus.EXPIRED)
            unlock("u2", UnlockStatus.ENDED)
            unlock("u3", UnlockStatus.ACTIVE)

            assertEquals("u3", db.unlockSessionDao().findActive()?.id)
        }

    @Test
    fun noActiveUnlockReturnsNull() =
        runTest {
            unlock("u1", UnlockStatus.EXPIRED)

            assertNull(db.unlockSessionDao().findActive())
        }

    @Test
    fun bothTimestampsAndAllowedPackagesSurviveStorage() =
        runTest {
            unlock("u1", UnlockStatus.ACTIVE, allowed = listOf("com.a", "com.b"), endAtElapsed = 123_456L)

            val stored = db.unlockSessionDao().findActive()

            assertEquals(123_456L, stored?.endAtElapsed)
            assertEquals(1_700_000_000_000L, stored?.endAtWallClock)
            assertEquals(1_699_999_000_000L, stored?.bootWallClock)
            assertEquals(listOf("com.a", "com.b"), stored?.allowedPackages)
        }

    @Test
    fun expiringTheActiveUnlockLeavesNoneActive() =
        runTest {
            unlock("u1", UnlockStatus.ACTIVE)

            db.unlockSessionDao().updateStatus("u1", UnlockStatus.EXPIRED)

            assertNull(db.unlockSessionDao().findActive())
            assertEquals(0, db.unlockSessionDao().countActive())
        }

    @Test
    fun closingAllActiveHandlesTheTamperCase() =
        runTest {
            unlock("u1", UnlockStatus.ACTIVE)
            unlock("u2", UnlockStatus.ACTIVE)

            db.unlockSessionDao().closeAllActive(UnlockStatus.ENDED)

            assertEquals(0, db.unlockSessionDao().countActive())
        }

    @Test
    fun observingActiveUnlockEmitsTheCurrentOne() =
        runTest {
            unlock("u1", UnlockStatus.ACTIVE)

            assertEquals(
                "u1",
                db
                    .unlockSessionDao()
                    .observeActive()
                    .first()
                    ?.id,
            )
        }

    @Test
    fun onlyEnabledPackagesReachTheDetectionLoop() =
        runTest {
            val dao = db.restrictedAppDao()
            dao.upsert(RestrictedAppEntity("com.b.app", enabled = true, addedAt = 1))
            dao.upsert(RestrictedAppEntity("com.a.app", enabled = true, addedAt = 2))
            dao.upsert(RestrictedAppEntity("com.c.app", enabled = false, addedAt = 3))

            assertEquals(listOf("com.a.app", "com.b.app"), dao.findEnabledPackages())
        }

    @Test
    fun togglingAppOffRemovesItFromTheEnabledSetWithoutDeleting() =
        runTest {
            val dao = db.restrictedAppDao()
            dao.upsert(RestrictedAppEntity("com.a.app", enabled = true, addedAt = 1))

            dao.setEnabled("com.a.app", false)

            assertTrue(dao.findEnabledPackages().isEmpty())
            assertEquals(1, dao.observeAll().first().size)
        }

    @Test
    fun insertIgnoringPreservesTheOriginalAddedAt() =
        runTest {
            val dao = db.restrictedAppDao()
            dao.insertIgnoring(listOf(RestrictedAppEntity("com.a.app", enabled = true, addedAt = 111)))
            dao.insertIgnoring(listOf(RestrictedAppEntity("com.a.app", enabled = false, addedAt = 999)))

            val stored = dao.observeAll().first().single()
            assertEquals(111L, stored.addedAt)
            assertTrue(stored.enabled)
        }

    @Test
    fun removingAnAppDropsItEntirely() =
        runTest {
            val dao = db.restrictedAppDao()
            dao.upsert(RestrictedAppEntity("com.a.app", enabled = true, addedAt = 1))

            dao.deleteByPackage("com.a.app")

            assertTrue(dao.observeAll().first().isEmpty())
        }
}

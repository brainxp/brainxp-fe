package com.example.brainxp.data.repo

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.brainxp.data.db.DbTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomRestrictionRepositoryTest : DbTest() {
    private val repository get() = RoomRestrictionRepository(db.restrictedAppDao())

    @Test
    fun anUnknownPackageCanBeRestricted() =
        runTest {
            repository.setRestricted(UNKNOWN, enabled = true)

            val rows = repository.observeRestricted().first()

            assertEquals(1, rows.size)
            assertEquals(UNKNOWN, rows.single().packageName)
            assertTrue(rows.single().enabled)
        }

    @Test
    fun restrictingThenUnrestrictingKeepsTheRow() =
        runTest {
            repository.setRestricted(UNKNOWN, enabled = true)
            repository.setRestricted(UNKNOWN, enabled = false)

            val rows = repository.observeRestricted().first()

            assertEquals(1, rows.size)
            assertFalse(rows.single().enabled)
        }

    @Test
    fun manyUnknownPackagesCanEachBeRestricted() =
        runTest {
            val packages = listOf("com.a.one", "com.b.two", "com.c.three")

            packages.forEach { repository.setRestricted(it, enabled = true) }

            val enabled =
                repository
                    .observeRestricted()
                    .first()
                    .filter { it.enabled }
                    .map { it.packageName }
            assertEquals(packages.sorted(), enabled.sorted())
        }

    @Test
    fun togglingOneAppLeavesTheOthersAlone() =
        runTest {
            repository.setRestricted("com.a.one", enabled = true)
            repository.setRestricted("com.b.two", enabled = true)

            repository.setRestricted("com.a.one", enabled = false)

            val rows = repository.observeRestricted().first().associate { it.packageName to it.enabled }
            assertEquals(false, rows["com.a.one"])
            assertEquals(true, rows["com.b.two"])
        }

    @Test
    fun replaceEnablesOnlyTheGivenPackages() =
        runTest {
            repository.setRestricted("com.a.one", enabled = true)
            repository.setRestricted("com.b.two", enabled = true)

            repository.replaceRestricted(listOf("com.b.two", "com.c.three"))

            val enabled =
                repository
                    .observeRestricted()
                    .first()
                    .filter { it.enabled }
                    .map { it.packageName }
            assertEquals(listOf("com.b.two", "com.c.three"), enabled.sorted())
        }

    @Test
    fun replaceWithAnEmptyListDisablesEverything() =
        runTest {
            repository.setRestricted("com.a.one", enabled = true)

            repository.replaceRestricted(emptyList())

            assertTrue(repository.observeRestricted().first().none { it.enabled })
        }

    @Test
    fun theSelectionSurvivesANewRepositoryOverTheSameDatabase() =
        runTest {
            repository.setRestricted(UNKNOWN, enabled = true)

            val reopened = RoomRestrictionRepository(db.restrictedAppDao())

            assertTrue(
                reopened
                    .observeRestricted()
                    .first()
                    .single { it.packageName == UNKNOWN }
                    .enabled,
            )
        }

    private companion object {
        const val UNKNOWN = "com.brand.new.game"
    }
}

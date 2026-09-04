package com.example.brainxp.core.detect

import com.example.brainxp.core.permission.SpecialPermission
import com.example.brainxp.data.prefs.DetectorChoice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SelectableForegroundAppDetectorTest {
    private val usageStats = FakeForegroundAppDetector()
    private val accessibility = FakeForegroundAppDetector()
    private val choice = MutableStateFlow(DetectorChoice.USAGE_STATS)

    private val detector =
        SelectableForegroundAppDetector(
            usageStats = usageStats,
            accessibility = accessibility,
            choice = choice,
        )

    @Test
    fun `emits from usage stats while it is the choice`() =
        runTest {
            val seen = mutableListOf<String>()
            val job = backgroundScope.launch { detector.foregroundPackage.collect { seen += it } }
            runCurrent()

            usageStats.emit("com.game.one")
            accessibility.emit("com.ignored")
            runCurrent()

            assertEquals(listOf("com.game.one"), seen)
            job.cancel()
        }

    @Test
    fun `emits from accessibility once it becomes the choice`() =
        runTest {
            choice.value = DetectorChoice.ACCESSIBILITY
            val seen = mutableListOf<String>()
            val job = backgroundScope.launch { detector.foregroundPackage.collect { seen += it } }
            runCurrent()

            accessibility.emit("com.social.two")
            usageStats.emit("com.ignored")
            runCurrent()

            assertEquals(listOf("com.social.two"), seen)
            job.cancel()
        }

    @Test
    fun `switching the choice swaps the source without a restart`() =
        runTest {
            val seen = mutableListOf<String>()
            val job = backgroundScope.launch { detector.foregroundPackage.collect { seen += it } }
            runCurrent()

            usageStats.emit("com.game.one")
            runCurrent()

            choice.value = DetectorChoice.ACCESSIBILITY
            runCurrent()

            accessibility.emit("com.social.two")
            runCurrent()

            assertEquals(listOf("com.game.one", "com.social.two"), seen)
            job.cancel()
        }

    @Test
    fun `the previous source stops being collected after a switch`() =
        runTest {
            val seen = mutableListOf<String>()
            val job = backgroundScope.launch { detector.foregroundPackage.collect { seen += it } }
            runCurrent()

            choice.value = DetectorChoice.ACCESSIBILITY
            runCurrent()

            usageStats.emit("com.game.one")
            runCurrent()

            assertTrue(seen.isEmpty())
            job.cancel()
        }

    @Test
    fun `switching back resumes the original source`() =
        runTest {
            val seen = mutableListOf<String>()
            val job = backgroundScope.launch { detector.foregroundPackage.collect { seen += it } }
            runCurrent()

            choice.value = DetectorChoice.ACCESSIBILITY
            runCurrent()
            choice.value = DetectorChoice.USAGE_STATS
            runCurrent()

            usageStats.emit("com.game.one")
            runCurrent()

            assertEquals(listOf("com.game.one"), seen)
            job.cancel()
        }

    @Test
    fun `a repeated choice does not resubscribe`() =
        runTest {
            val job = backgroundScope.launch { detector.foregroundPackage.collect { } }
            runCurrent()

            choice.value = DetectorChoice.USAGE_STATS
            runCurrent()

            assertEquals(1, usageStats.collections)
            job.cancel()
        }

    @Test
    fun `availability follows the selected detector`() {
        usageStats.available = true
        accessibility.available = false

        assertTrue(detector.isAvailable())

        choice.value = DetectorChoice.ACCESSIBILITY

        assertFalse(detector.isAvailable())
    }

    @Test
    fun `missing requirements follow the selected detector`() {
        usageStats.missing = listOf(SpecialPermission.USAGE_ACCESS)
        accessibility.missing = listOf(SpecialPermission.ACCESSIBILITY)

        assertEquals(listOf(SpecialPermission.USAGE_ACCESS), detector.missingRequirements())

        choice.value = DetectorChoice.ACCESSIBILITY

        assertEquals(listOf(SpecialPermission.ACCESSIBILITY), detector.missingRequirements())
    }

    @Test
    fun `an available detector reports nothing missing`() {
        usageStats.missing = emptyList()

        assertTrue(detector.missingRequirements().isEmpty())
    }
}

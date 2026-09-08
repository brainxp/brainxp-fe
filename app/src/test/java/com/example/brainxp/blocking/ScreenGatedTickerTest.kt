package com.example.brainxp.blocking

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val INTERVAL = 600L

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenGatedTickerTest {
    @Test
    fun `ticks immediately when the screen is already on`() =
        runTest {
            val screen = MutableStateFlow(true)
            val seen = mutableListOf<Long>()
            val job = backgroundScope.launch { ScreenGatedTicker(screen, INTERVAL).ticks().collect { seen += it } }

            runCurrent()

            assertEquals(listOf(0L), seen)
            job.cancel()
        }

    @Test
    fun `does not tick at all while the screen is off`() =
        runTest {
            val screen = MutableStateFlow(false)
            val seen = mutableListOf<Long>()
            val job = backgroundScope.launch { ScreenGatedTicker(screen, INTERVAL).ticks().collect { seen += it } }

            runCurrent()
            advanceTimeBy(10 * INTERVAL)
            runCurrent()

            assertTrue(seen.isEmpty())
            job.cancel()
        }

    @Test
    fun `ticks on every interval while the screen stays on`() =
        runTest {
            val screen = MutableStateFlow(true)
            val seen = mutableListOf<Long>()
            val job = backgroundScope.launch { ScreenGatedTicker(screen, INTERVAL).ticks().collect { seen += it } }

            runCurrent()
            advanceTimeBy(3 * INTERVAL)
            runCurrent()

            assertEquals(listOf(0L, 1L, 2L, 3L), seen)
            job.cancel()
        }

    @Test
    fun `stops ticking the moment the screen turns off`() =
        runTest {
            val screen = MutableStateFlow(true)
            val seen = mutableListOf<Long>()
            val job = backgroundScope.launch { ScreenGatedTicker(screen, INTERVAL).ticks().collect { seen += it } }

            runCurrent()
            advanceTimeBy(3 * INTERVAL)
            runCurrent()
            val whileOn = seen.size

            screen.value = false
            runCurrent()
            advanceTimeBy(100 * INTERVAL)
            runCurrent()

            assertEquals(whileOn, seen.size)
            job.cancel()
        }

    @Test
    fun `resumes ticking when the screen comes back on`() =
        runTest {
            val screen = MutableStateFlow(true)
            val seen = mutableListOf<Long>()
            val job = backgroundScope.launch { ScreenGatedTicker(screen, INTERVAL).ticks().collect { seen += it } }

            runCurrent()
            screen.value = false
            runCurrent()
            advanceTimeBy(10 * INTERVAL)
            runCurrent()
            val whileOff = seen.size

            screen.value = true
            runCurrent()

            assertEquals(whileOff + 1, seen.size)
            job.cancel()
        }

    @Test
    fun `each screen on session restarts the tick index`() =
        runTest {
            val screen = MutableStateFlow(true)
            val seen = mutableListOf<Long>()
            val job = backgroundScope.launch { ScreenGatedTicker(screen, INTERVAL).ticks().collect { seen += it } }

            runCurrent()
            advanceTimeBy(2 * INTERVAL)
            runCurrent()
            screen.value = false
            runCurrent()
            screen.value = true
            runCurrent()

            assertEquals(listOf(0L, 1L, 2L, 0L), seen)
            job.cancel()
        }

    @Test
    fun `repeated screen off does not accumulate ticks`() =
        runTest {
            val screen = MutableStateFlow(false)
            val seen = mutableListOf<Long>()
            val job = backgroundScope.launch { ScreenGatedTicker(screen, INTERVAL).ticks().collect { seen += it } }

            repeat(5) {
                screen.value = false
                runCurrent()
                advanceTimeBy(5 * INTERVAL)
                runCurrent()
            }

            assertTrue(seen.isEmpty())
            job.cancel()
        }
}

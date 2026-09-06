package com.example.brainxp.blocking

import android.app.UiAutomation
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.FileInputStream
import javax.inject.Inject

@HiltAndroidTest
class BlockOverlayControllerTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var controller: BlockOverlayController

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Before
    fun setUp() {
        hiltRule.inject()
        shell("appops set ${instrumentation.targetContext.packageName} SYSTEM_ALERT_WINDOW allow")
    }

    @After
    fun tearDown() {
        onMain { controller.hide() }
        awaitWindowCount(0)
    }

    @Test
    fun showAttachesExactlyOneWindow() {
        onMain { controller.show(PACKAGE) { _, _ -> } }

        awaitWindowCount(1)
        assertTrue(controller.isShowing)
        assertEquals(PACKAGE, controller.showingFor)
    }

    @Test
    fun hideRemovesTheWindow() {
        onMain { controller.show(PACKAGE) { _, _ -> } }
        awaitWindowCount(1)

        onMain { controller.hide() }

        awaitWindowCount(0)
        assertFalse(controller.isShowing)
    }

    @Test
    fun showIsIdempotent() {
        onMain {
            controller.show(PACKAGE) { _, _ -> }
            controller.show(PACKAGE) { _, _ -> }
            controller.show(PACKAGE) { _, _ -> }
        }

        awaitWindowCount(1)
    }

    @Test
    fun hideIsIdempotent() {
        onMain { controller.show(PACKAGE) { _, _ -> } }
        awaitWindowCount(1)

        onMain {
            controller.hide()
            controller.hide()
            controller.hide()
        }

        awaitWindowCount(0)
    }

    @Test
    fun hideWithoutShowDoesNothing() {
        onMain { controller.hide() }

        awaitWindowCount(0)
        assertFalse(controller.isShowing)
    }

    @Test
    fun switchingTargetKeepsASingleWindow() {
        onMain { controller.show(PACKAGE) { _, _ -> } }
        awaitWindowCount(1)

        onMain { controller.show(OTHER_PACKAGE) { _, _ -> } }

        awaitWindowCount(1)
        assertEquals(OTHER_PACKAGE, controller.showingFor)
    }

    @Test
    fun oneHundredCyclesLeaveNoWindowBehind() {
        repeat(CYCLES) {
            onMain { controller.show(PACKAGE) { _, _ -> } }
            onMain { controller.hide() }
        }

        awaitWindowCount(0)
        assertFalse(controller.isShowing)
    }

    @Test
    fun oneHundredCyclesNeverExceedOneWindowAtATime() {
        var peak = 0
        repeat(CYCLES) {
            onMain { controller.show(PACKAGE) { _, _ -> } }
            instrumentation.waitForIdleSync()
            peak = maxOf(peak, overlayWindowCount())
            onMain { controller.hide() }
        }
        awaitWindowCount(0)

        assertEquals(1, peak)
    }

    @Test
    fun aCycleAfterOneHundredStillWorks() {
        repeat(CYCLES) {
            onMain { controller.show(PACKAGE) { _, _ -> } }
            onMain { controller.hide() }
        }
        awaitWindowCount(0)

        onMain { controller.show(PACKAGE) { _, _ -> } }

        awaitWindowCount(1)
        onMain { controller.hide() }
        awaitWindowCount(0)
    }

    @Test
    fun showFromABackgroundThreadStillAttaches() {
        val thread = Thread { controller.show(PACKAGE) { _, _ -> } }
        thread.start()
        thread.join()

        awaitWindowCount(1)
    }

    @Test
    fun hideFromABackgroundThreadStillDetaches() {
        onMain { controller.show(PACKAGE) { _, _ -> } }
        awaitWindowCount(1)

        val thread = Thread { controller.hide() }
        thread.start()
        thread.join()

        awaitWindowCount(0)
    }

    private fun onMain(block: () -> Unit) {
        instrumentation.runOnMainSync(block)
    }

    private fun matchedLines(): List<String> =
        shell("dumpsys window windows")
            .lineSequence()
            .filter { it.contains(BlockOverlayController.WINDOW_TITLE) }
            .map { it.trim() }
            .toList()

    private fun overlayWindowCount(): Int =
        matchedLines()
            .mapNotNull { WINDOW_ID.find(it)?.groupValues?.get(1) }
            .distinct()
            .size

    private fun awaitWindowCount(expected: Int) {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        var actual = -1
        while (System.currentTimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            actual = overlayWindowCount()
            if (actual == expected) {
                return
            }
            Thread.sleep(POLL_MS)
        }
        assertEquals(
            "overlay windows attached, matched lines = ${matchedLines()}",
            expected,
            actual,
        )
    }

    private fun shell(command: String): String {
        val automation: UiAutomation = instrumentation.uiAutomation
        val descriptor = automation.executeShellCommand(command)
        return FileInputStream(descriptor.fileDescriptor).use { it.readBytes().decodeToString() }
    }

    private companion object {
        const val PACKAGE = "com.mobile.legends"
        const val OTHER_PACKAGE = "com.supercell.clashofclans"
        const val CYCLES = 100
        const val TIMEOUT_MS = 5_000L
        const val POLL_MS = 50L
        val WINDOW_ID = Regex("""Window\{([0-9a-f]+)""")
    }
}

package com.example.brainxp.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavMotionTest {
    @Test
    fun `a push brings the new screen in from the right and sends the old one left`() {
        assertEquals(SlideSpec(SlideAxis.HORIZONTAL, TOWARDS_END), enterSlideOf(NavMotion.PUSH))
        assertEquals(SlideSpec(SlideAxis.HORIZONTAL, TOWARDS_START), exitSlideOf(NavMotion.PUSH))
    }

    @Test
    fun `popping a push reverses it exactly`() {
        assertEquals(SlideSpec(SlideAxis.HORIZONTAL, TOWARDS_START), popEnterSlideOf(NavMotion.PUSH))
        assertEquals(SlideSpec(SlideAxis.HORIZONTAL, TOWARDS_END), popExitSlideOf(NavMotion.PUSH))
    }

    @Test
    fun `switching tabs does not animate at all`() {
        listOf(
            enterSlideOf(NavMotion.NONE),
            exitSlideOf(NavMotion.NONE),
            popEnterSlideOf(NavMotion.NONE),
            popExitSlideOf(NavMotion.NONE),
        ).forEach { slide ->
            assertEquals(SlideAxis.NONE, slide.axis)
        }
    }

    @Test
    fun `a flow rises from the bottom`() {
        assertEquals(SlideSpec(SlideAxis.VERTICAL, TOWARDS_END), enterSlideOf(NavMotion.FLOW))
    }

    @Test
    fun `leaving a flow slides it back down`() {
        assertEquals(SlideSpec(SlideAxis.VERTICAL, TOWARDS_END), popExitSlideOf(NavMotion.FLOW))
    }

    @Test
    fun `a flow leaves the screen beneath it exactly where it is`() {
        assertEquals(SlideAxis.NONE, exitSlideOf(NavMotion.FLOW).axis)
        assertEquals(SlideAxis.NONE, popEnterSlideOf(NavMotion.FLOW).axis)
    }

    @Test
    fun `moving between two tabs is the silent motion`() {
        assertEquals(NavMotion.NONE, navMotionOf(fromTab = 0, toTab = 3, fromFlow = false, toFlow = false))
        assertEquals(NavMotion.NONE, navMotionOf(fromTab = 3, toTab = 0, fromFlow = false, toFlow = false))
    }

    @Test
    fun `entering a flow rises`() {
        assertEquals(NavMotion.FLOW, navMotionOf(fromTab = 0, toTab = null, fromFlow = false, toFlow = true))
    }

    @Test
    fun `leaving a flow is still a flow, so back slides down instead of sideways`() {
        assertEquals(NavMotion.FLOW, navMotionOf(fromTab = null, toTab = 0, fromFlow = true, toFlow = false))
    }

    @Test
    fun `drilling from a tab into a detail screen is a push`() {
        assertEquals(NavMotion.PUSH, navMotionOf(fromTab = 2, toTab = null, fromFlow = false, toFlow = false))
    }

    @Test
    fun `coming back out of a detail screen to a tab is a push too`() {
        assertEquals(NavMotion.PUSH, navMotionOf(fromTab = null, toTab = 2, fromFlow = false, toFlow = false))
    }

    @Test
    fun `a screen entering forwards is drawn above the one it covers`() {
        assertEquals(1f, forwardOf(NavMotion.FLOW).targetContentZIndex, 0f)
        assertEquals(1f, forwardOf(NavMotion.PUSH).targetContentZIndex, 0f)
    }

    @Test
    fun `going back puts the revealed screen strictly below, never merely level with, the leaving one`() {
        NavMotion.entries.forEach { motion ->
            val revealed = backwardOf(motion).targetContentZIndex
            val covering = forwardOf(motion).targetContentZIndex

            assertTrue("$motion: revealed screen must sit below the one sliding away", revealed < covering)
            assertTrue("$motion: level z would let composition order decide", revealed < 0f)
        }
    }

    @Test
    fun `no transition ever resizes the content`() {
        NavMotion.entries.forEach { motion ->
            assertEquals("$motion forward", null, forwardOf(motion).sizeTransform)
            assertEquals("$motion backward", null, backwardOf(motion).sizeTransform)
        }
    }
}

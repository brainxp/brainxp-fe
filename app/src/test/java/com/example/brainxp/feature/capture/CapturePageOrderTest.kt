package com.example.brainxp.feature.capture

import org.junit.Assert.assertEquals
import org.junit.Test

private fun state(vararg ids: String) = CaptureUiState(pages = ids.map { CapturedPage(it, "/tmp/$it.jpg") })

private fun CaptureUiState.order() = pages.map { it.id }

class CapturePageOrderTest {
    @Test
    fun `moving a middle page left swaps it with its neighbour`() {
        assertEquals(listOf("a", "c", "b"), state("a", "b", "c").movePage("c", -1).order())
    }

    @Test
    fun `moving a middle page right swaps it with its neighbour`() {
        assertEquals(listOf("b", "a", "c"), state("a", "b", "c").movePage("a", 1).order())
    }

    @Test
    fun `moving the first page left changes nothing`() {
        assertEquals(listOf("a", "b", "c"), state("a", "b", "c").movePage("a", -1).order())
    }

    @Test
    fun `moving the last page right changes nothing`() {
        assertEquals(listOf("a", "b", "c"), state("a", "b", "c").movePage("c", 1).order())
    }

    @Test
    fun `moving an unknown page changes nothing`() {
        assertEquals(listOf("a", "b"), state("a", "b").movePage("z", 1).order())
    }

    @Test
    fun `a single page cannot be moved`() {
        assertEquals(listOf("a"), state("a").movePage("a", 1).order())
        assertEquals(listOf("a"), state("a").movePage("a", -1).order())
    }

    @Test
    fun `continuing needs at least one page`() {
        assertEquals(false, CaptureUiState().canContinue)
        assertEquals(true, state("a").canContinue)
    }

    @Test
    fun `continuing is blocked while a shot is in flight`() {
        assertEquals(false, state("a").copy(capturing = true).canContinue)
    }
}

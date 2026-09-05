package com.example.brainxp.core.ocr

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FlakyEngine(
    private val failing: Set<String>,
) : OcrEngine {
    var calls = 0
        private set

    override suspend fun extract(
        pageId: String,
        path: String,
    ): PageText {
        calls++
        return if (pageId in failing) {
            PageText.Failed(pageId, "gagal")
        } else {
            PageText.Extracted(pageId, "isi $pageId")
        }
    }

    override suspend fun extractBatch(pages: List<Pair<String, String>>): OcrBatch =
        OcrBatch(pages.map { (pageId, path) -> extract(pageId, path) })
}

private fun pages(vararg ids: String) = ids.map { it to "/tmp/$it.jpg" }

class OcrBatchTest {
    @Test
    fun `a failing page does not stop the pages after it`() =
        runTest {
            val engine = FlakyEngine(failing = setOf("b"))

            val batch = engine.extractBatch(pages("a", "b", "c"))

            assertEquals(3, engine.calls)
            assertEquals(listOf("a", "c"), batch.extracted.map { it.pageId })
            assertEquals(listOf("b"), batch.failed.map { it.pageId })
        }

    @Test
    fun `a failing first page does not stop the batch`() =
        runTest {
            val batch = FlakyEngine(failing = setOf("a")).extractBatch(pages("a", "b"))

            assertEquals(listOf("b"), batch.extracted.map { it.pageId })
            assertTrue(batch.anyExtracted)
        }

    @Test
    fun `every page failing is reported as such`() =
        runTest {
            val batch = FlakyEngine(failing = setOf("a", "b")).extractBatch(pages("a", "b"))

            assertTrue(batch.allFailed)
            assertFalse(batch.anyExtracted)
            assertEquals(2, batch.failed.size)
        }

    @Test
    fun `page order is preserved`() =
        runTest {
            val batch = FlakyEngine(failing = emptySet()).extractBatch(pages("x", "y", "z"))

            assertEquals(listOf("x", "y", "z"), batch.pages.map { it.pageId })
        }

    @Test
    fun `an empty batch is neither extracted nor failed`() =
        runTest {
            val batch = FlakyEngine(failing = emptySet()).extractBatch(emptyList())

            assertFalse(batch.anyExtracted)
            assertFalse(batch.allFailed)
        }

    @Test
    fun `each failure carries its own reason`() =
        runTest {
            val batch = FlakyEngine(failing = setOf("b")).extractBatch(pages("a", "b"))

            assertEquals("gagal", batch.failed.single().reason)
        }
}

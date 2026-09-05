package com.example.brainxp.core.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrBlockAssemblyTest {
    @Test
    fun `blocks are ordered top to bottom`() {
        val blocks =
            listOf(
                OcrBlock("bawah", top = 300, left = 10),
                OcrBlock("atas", top = 10, left = 10),
                OcrBlock("tengah", top = 150, left = 10),
            )

        assertEquals("atas\ntengah\nbawah", assemblePageText(blocks))
    }

    @Test
    fun `blocks on the same line are ordered left to right`() {
        val blocks =
            listOf(
                OcrBlock("kanan", top = 40, left = 400),
                OcrBlock("kiri", top = 40, left = 20),
            )

        assertEquals("kiri\nkanan", assemblePageText(blocks))
    }

    @Test
    fun `top wins over left`() {
        val blocks =
            listOf(
                OcrBlock("kolom kanan atas", top = 10, left = 500),
                OcrBlock("kolom kiri bawah", top = 200, left = 20),
            )

        assertEquals("kolom kanan atas\nkolom kiri bawah", assemblePageText(blocks))
    }

    @Test
    fun `blank blocks are dropped`() {
        val blocks =
            listOf(
                OcrBlock("   ", top = 10, left = 10),
                OcrBlock("nyata", top = 20, left = 10),
                OcrBlock("", top = 30, left = 10),
            )

        assertEquals("nyata", assemblePageText(blocks))
    }

    @Test
    fun `surrounding whitespace is trimmed per block`() {
        val blocks = listOf(OcrBlock("  padded  ", top = 10, left = 10))

        assertEquals("padded", assemblePageText(blocks))
    }

    @Test
    fun `no blocks yields an empty string`() {
        assertEquals("", assemblePageText(emptyList()))
    }

    @Test
    fun `equal positions keep a stable order`() {
        val blocks =
            listOf(
                OcrBlock("pertama", top = 10, left = 10),
                OcrBlock("kedua", top = 10, left = 10),
            )

        assertEquals("pertama\nkedua", assemblePageText(blocks))
    }
}

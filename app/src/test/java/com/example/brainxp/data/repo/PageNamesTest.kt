package com.example.brainxp.data.repo

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class PageNamesTest {
    private fun files(count: Int) = (1..count).map { File("/cache/upload-$it.jpg") }

    @Test
    fun `one page carries the material title on its own`() {
        assertEquals(listOf("Foto catatan 6 Sep 21.53"), pageNames("Foto catatan 6 Sep 21.53", files(1)))
    }

    @Test
    fun `several pages of one material are numbered against the total`() {
        val names = pageNames("Foto catatan 6 Sep 21.53", files(3))

        assertEquals(
            listOf(
                "Foto catatan 6 Sep 21.53 (1/3)",
                "Foto catatan 6 Sep 21.53 (2/3)",
                "Foto catatan 6 Sep 21.53 (3/3)",
            ),
            names,
        )
    }

    @Test
    fun `six pages stay one material and produce six names`() {
        val names = pageNames("Foto catatan", files(6))

        assertEquals(6, names.size)
        assertEquals("Foto catatan (6/6)", names.last())
    }

    @Test
    fun `a blank title falls back to the file on disk`() {
        assertEquals(listOf("upload-1.jpg"), pageNames("   ", files(1)))
    }
}

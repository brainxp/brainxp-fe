package com.example.brainxp.feature.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureTitleTest {
    private val stamp = "6 Sep 21.53"

    @Test
    fun `a single page is named without a page number`() {
        assertEquals(listOf("Foto catatan 6 Sep 21.53"), captureTitles("Foto catatan", stamp, pages = 1))
    }

    @Test
    fun `several pages are numbered so the library can tell them apart`() {
        val titles = captureTitles("Foto catatan", stamp, pages = 3)

        assertEquals(
            listOf(
                "Foto catatan 6 Sep 21.53 (1/3)",
                "Foto catatan 6 Sep 21.53 (2/3)",
                "Foto catatan 6 Sep 21.53 (3/3)",
            ),
            titles,
        )
    }

    @Test
    fun `no title carries a cache file name or a uuid`() {
        val titles = captureTitles("Foto catatan", stamp, pages = 4)

        titles.forEach { title ->
            assertTrue("$title looks like a file", !title.contains(".jpg"))
            assertTrue("$title looks like a uuid", !title.contains("-4"))
        }
    }

    @Test
    fun `every title in a batch is distinct`() {
        val titles = captureTitles("Foto catatan", stamp, pages = 5)

        assertEquals(titles.size, titles.toSet().size)
    }

    @Test
    fun `no pages produces no titles`() {
        assertEquals(emptyList<String>(), captureTitles("Foto catatan", stamp, pages = 0))
    }
}

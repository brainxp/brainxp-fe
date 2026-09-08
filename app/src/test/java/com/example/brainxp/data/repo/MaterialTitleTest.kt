package com.example.brainxp.data.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MaterialTitleTest {
    @Test
    fun `a page suffix is dropped`() {
        assertEquals("Foto catatan 8 Sep 19.07", readableTitle("Foto catatan 8 Sep 19.07 (1/3)"))
    }

    @Test
    fun `an extra photo suffix is dropped`() {
        assertEquals("Foto catatan 8 Sep 19.07", readableTitle("Foto catatan 8 Sep 19.07 (+2 foto)"))
    }

    @Test
    fun `both suffixes together are dropped`() {
        assertEquals("Foto catatan 8 Sep 19.07", readableTitle("Foto catatan 8 Sep 19.07 (1/3) (+2 foto)"))
    }

    @Test
    fun `the suffixes are dropped in either order`() {
        assertEquals("Catatan", readableTitle("Catatan (+3 foto) (1/4)"))
    }

    @Test
    fun `the english wording is dropped too`() {
        assertEquals("Notes", readableTitle("Notes (1/2) (+1 photo)"))
        assertEquals("Notes", readableTitle("Notes (+2 photos)"))
    }

    @Test
    fun `a plain document name is untouched`() {
        assertEquals("fotosintesis.txt", readableTitle("fotosintesis.txt"))
    }

    @Test
    fun `a single photo name is untouched`() {
        assertEquals("Foto catatan 8 Sep 20.21", readableTitle("Foto catatan 8 Sep 20.21"))
    }

    @Test
    fun `a name that only looks like a suffix elsewhere is untouched`() {
        assertEquals("Bab (1/3) lanjutan", readableTitle("Bab (1/3) lanjutan"))
        assertEquals("Laporan (+2 revisi)", readableTitle("Laporan (+2 revisi)"))
    }

    @Test
    fun `null stays null`() {
        assertNull(readableTitle(null))
    }

    @Test
    fun `a name left blank falls through`() {
        assertNull(readableTitle("(1/3) (+2 foto)"))
    }
}

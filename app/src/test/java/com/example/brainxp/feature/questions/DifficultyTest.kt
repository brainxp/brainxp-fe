package com.example.brainxp.feature.questions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DifficultyTest {
    @Test
    fun `the three server difficulty words map to their levels`() {
        assertEquals(Difficulty.EASY, difficultyOf("mudah"))
        assertEquals(Difficulty.MEDIUM, difficultyOf("sedang"))
        assertEquals(Difficulty.HARD, difficultyOf("sulit"))
    }

    @Test
    fun `casing and stray spaces from the server do not matter`() {
        assertEquals(Difficulty.MEDIUM, difficultyOf("  Sedang "))
    }

    @Test
    fun `an unknown word is left for the screen to show as it came`() {
        assertNull(difficultyOf("ekstrem"))
        assertNull(difficultyOf(""))
    }
}

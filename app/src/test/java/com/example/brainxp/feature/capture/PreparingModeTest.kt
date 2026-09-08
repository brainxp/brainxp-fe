package com.example.brainxp.feature.capture

import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.upload.PreparingStage
import org.junit.Assert.assertEquals
import org.junit.Test

class PreparingModeTest {
    private val loading =
        PreparingUiState(
            materialName = "Hukum Newton",
            stage = PreparingStage.READING,
            materialId = "m-1",
        )

    @Test
    fun `a partly written set still shows the game rather than the start action`() {
        val partial = loading.copy(stage = PreparingStage.PARTIAL, readyQuestions = 3, totalQuestions = 10)

        assertEquals(PreparingMode.GAME, modeOf(partial))
    }

    @Test
    fun `the whole set landing hands the screen to the start action`() {
        val ready = loading.copy(stage = PreparingStage.READY, readyQuestions = 10, totalQuestions = 10)

        assertEquals(PreparingMode.START, modeOf(ready))
    }

    @Test
    fun `a refusal is never played over`() {
        val refused = loading.copy(stage = PreparingStage.REJECTED, rejected = true, reasonCode = "not_study_material")

        assertEquals(PreparingMode.START, modeOf(refused))
    }

    @Test
    fun `a stalled server shows retry rather than a game`() {
        assertEquals(PreparingMode.START, modeOf(loading.copy(error = ApiError.Timeout)))
    }

    @Test
    fun `a second material goes straight to the game`() {
        val second = PreparingUiState(stage = PreparingStage.READING, materialId = "m-2")

        assertEquals(PreparingMode.GAME, modeOf(second))
    }
}

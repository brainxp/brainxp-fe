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
    fun `an unseen tutorial takes the screen before anything else`() {
        assertEquals(PreparingMode.TUTORIAL, modeOf(loading.copy(guide = true)))
    }

    @Test
    fun `the tutorial wins even once the questions are ready`() {
        val ready = loading.copy(guide = true, stage = PreparingStage.READY, readyQuestions = 10)

        assertEquals(PreparingMode.TUTORIAL, modeOf(ready))
    }

    @Test
    fun `finishing the tutorial while questions are still being written lands on the game`() {
        val finished = loading.copy(guide = false, stage = PreparingStage.VALIDATING)

        assertEquals(PreparingMode.GAME, modeOf(finished))
    }

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
    fun `a second material skips the tutorial and goes straight to the game`() {
        val second = PreparingUiState(stage = PreparingStage.READING, materialId = "m-2", guide = false)

        assertEquals(PreparingMode.GAME, modeOf(second))
    }
}

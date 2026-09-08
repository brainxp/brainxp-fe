package com.example.brainxp.feature.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class LevelStepTest {
    @Test
    fun `an account with no subject yet has to create one`() {
        assertEquals(LevelStep.CREATE_SUBJECT, levelStepOf(subjectId = null))
    }

    @Test
    fun `a blank subject id counts as no subject at all`() {
        assertEquals(LevelStep.CREATE_SUBJECT, levelStepOf(subjectId = "   "))
    }

    @Test
    fun `an account that already has a subject only updates its level`() {
        assertEquals(LevelStep.UPDATE_LEVEL, levelStepOf(subjectId = "subject-7"))
    }
}

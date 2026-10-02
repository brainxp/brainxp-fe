package com.example.brainxp.feature.questions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuestionTypeTest {
    @Test
    fun `the server question types map to their kinds`() {
        assertEquals(QuestionType.MULTIPLE_CHOICE, questionTypeOf("mcq"))
        assertEquals(QuestionType.ESSAY, questionTypeOf("essay"))
    }

    @Test
    fun `casing from the server does not matter`() {
        assertEquals(QuestionType.ESSAY, questionTypeOf(" Essay "))
    }

    @Test
    fun `an unknown type is left out of the title rather than guessed`() {
        assertNull(questionTypeOf("matching"))
        assertNull(questionTypeOf(""))
    }
}

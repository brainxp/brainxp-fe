package com.example.brainxp.feature.questions

import com.example.brainxp.domain.model.Question
import com.example.brainxp.domain.model.QuestionSession
import com.example.brainxp.domain.model.SavedAnswer
import com.example.brainxp.domain.model.SessionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun mcq(id: String) =
    QuizQuestion(
        id = id,
        stem = "Soal $id",
        difficulty = "sedang",
        factor = 1.2,
        options = listOf("A", "B", "C", "D"),
    )

private fun essay(id: String) =
    QuizQuestion(
        id = id,
        stem = "Soal $id",
        difficulty = "sulit",
        factor = 1.8,
        rubricCriteria = 3,
    )

private fun state(vararg questions: QuizQuestion) = QuizUiState(questions = questions.toList())

class QuizUiStateTest {
    @Test
    fun `choosing an option fills the answer`() {
        val filled = state(mcq("a"), mcq("b")).withChoice("a", 2)

        assertEquals(2, filled.chosenFor("a"))
        assertEquals("C", filled.answers["a"])
        assertEquals(1, filled.filled)
        assertFalse(filled.complete)
    }

    @Test
    fun `choosing an option out of range changes nothing`() {
        val untouched = state(mcq("a")).withChoice("a", 9)

        assertNull(untouched.chosenFor("a"))
        assertEquals(0, untouched.filled)
    }

    @Test
    fun `a short essay draft is not saved`() {
        val typed = state(essay("a")).withDraft("a", "belum")

        assertEquals("belum", typed.draftFor("a"))
        assertEquals(0, typed.withEssaySaved("a").filled)
    }

    @Test
    fun `a long enough essay draft is saved trimmed`() {
        val saved = state(essay("a")).withDraft("a", "  gaya adalah massa kali percepatan  ").withEssaySaved("a")

        assertEquals("gaya adalah massa kali percepatan", saved.answers["a"])
        assertTrue(saved.complete)
    }

    @Test
    fun `doubt toggles on and off`() {
        val marked = state(mcq("a")).withDoubtToggled("a")
        assertTrue("a" in marked.doubts)
        assertEquals(1, marked.doubtedCount)

        val cleared = marked.withDoubtToggled("a")
        assertFalse("a" in cleared.doubts)
        assertEquals(0, cleared.doubtedCount)
    }

    @Test
    fun `doubt count ignores questions outside the session`() {
        val stale = state(mcq("a")).copy(doubts = setOf("a", "gone"))

        assertEquals(1, stale.doubtedCount)
    }

    @Test
    fun `a doubted question keeps its dot even when it is the current one`() {
        val marked = state(mcq("a"), mcq("b")).withChoice("a", 0).withDoubtToggled("a")

        assertEquals(DotState.DOUBTED_ANSWERED, marked.dotState(0))
        assertEquals(DotState.CURRENT, marked.copy(index = 1).dotState(1))
    }

    @Test
    fun `dots read answered then empty`() {
        val partial = state(mcq("a"), mcq("b")).withChoice("a", 0).copy(index = 1)

        assertEquals(DotState.ANSWERED, partial.dotState(0))
        assertEquals(DotState.CURRENT, partial.dotState(1))
        assertEquals(DotState.EMPTY, partial.dotState(2))
    }

    @Test
    fun `progress tracks filled answers`() {
        val half = state(mcq("a"), mcq("b")).withChoice("a", 1)

        assertEquals(0.5f, half.progress, 0f)
        assertEquals(0f, state().progress, 0f)
    }

    @Test
    fun `a resumed session restores choices and drafts`() {
        val session =
            QuestionSession(
                sessionId = "s1",
                title = "Bab 4",
                materialId = "m1",
                mode = SessionMode.NEW,
                createdAt = 0L,
                questions =
                    listOf(
                        Question.MultipleChoice(
                            id = "a",
                            conceptIds = emptyList(),
                            stem = "Soal a",
                            options = listOf("A", "B", "C", "D"),
                        ),
                        Question.ShortAnswer(id = "b", conceptIds = emptyList(), stem = "Soal b"),
                        Question.MultipleChoice(
                            id = "c",
                            conceptIds = emptyList(),
                            stem = "Soal c",
                            options = listOf("A", "B", "C", "D"),
                        ),
                    ),
                answeredIds = setOf("a", "b"),
                answers =
                    listOf(
                        SavedAnswer(questionId = "a", chosenIndex = 1),
                        SavedAnswer(questionId = "b", essayText = "gaya adalah massa kali percepatan"),
                    ),
            )

        val resumed = session.toUiState()

        assertEquals(1, resumed.chosenFor("a"))
        assertEquals("gaya adalah massa kali percepatan", resumed.draftFor("b"))
        assertEquals(2, resumed.filled)
        assertEquals(2, resumed.index)
    }

    @Test
    fun `unsupported question types are dropped`() {
        val session =
            QuestionSession(
                sessionId = "s1",
                materialId = "m1",
                mode = SessionMode.NEW,
                createdAt = 0L,
                questions =
                    listOf(
                        Question.Unsupported(id = "x", conceptIds = emptyList(), rawType = "matching"),
                        Question.MultipleChoice(
                            id = "a",
                            conceptIds = emptyList(),
                            stem = "Soal a",
                            options = listOf("A", "B"),
                        ),
                    ),
            )

        val opened = session.toUiState()

        assertEquals(1, opened.total)
        assertEquals("a", opened.current?.id)
    }

    @Test
    fun `the essay threshold matches the eight character rule`() {
        assertFalse(essayLongEnough("tujuh  "))
        assertTrue(essayLongEnough("delapan!"))
    }
}

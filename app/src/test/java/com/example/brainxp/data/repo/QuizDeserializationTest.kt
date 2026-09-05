package com.example.brainxp.data.repo

import com.example.brainxp.core.network.QuizDto
import com.example.brainxp.domain.model.Question
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val json = Json { ignoreUnknownKeys = true }

private fun quizJson(vararg questions: String): String =
    """
    {
      "session_id": "11111111-1111-1111-1111-111111111111",
      "material_id": "22222222-2222-2222-2222-222222222222",
      "title": "Bab 4",
      "base_reward_seconds": 120,
      "level_factor": 1.0,
      "novelty_factor": 0.6,
      "max_reward_seconds": 900,
      "ready_count": ${questions.size},
      "total_count": ${questions.size},
      "status": "ready",
      "questions": [${questions.joinToString(",")}]
    }
    """.trimIndent()

private fun question(
    ordinal: Int,
    qtype: String,
    extra: String = "",
): String =
    """
    {
      "id": "0000000$ordinal-0000-0000-0000-000000000000",
      "ordinal": $ordinal,
      "qtype": "$qtype",
      "stem": "Soal $ordinal",
      "difficulty": "sedang",
      "bloom_level": "understand",
      "source_excerpt": "kutipan",
      "type_factor": 1.0,
      "difficulty_factor": 1.2$extra
    }
    """.trimIndent()

class QuizDeserializationTest {
    @Test
    fun `an unrecognised question type becomes Unsupported instead of throwing`() {
        val payload = quizJson(question(1, "diagram_labelling"))

        val session = json.decodeFromString<QuizDto>(payload).toSession()

        val only = session.questions.single()
        assertTrue("expected Unsupported but got $only", only is Question.Unsupported)
        assertEquals("diagram_labelling", (only as Question.Unsupported).rawType)
    }

    @Test
    fun `an unrecognised type does not discard the questions around it`() {
        val payload =
            quizJson(
                question(1, "mcq", ""","options":["a","b","c","d"]"""),
                question(2, "audio_clip"),
                question(3, "essay"),
            )

        val session = json.decodeFromString<QuizDto>(payload).toSession()

        assertEquals(3, session.questions.size)
        assertTrue(session.questions[0] is Question.MultipleChoice)
        assertTrue(session.questions[1] is Question.Unsupported)
        assertTrue(session.questions[2] is Question.ShortAnswer)
    }

    @Test
    fun `unknown top level fields do not break parsing`() {
        val payload =
            quizJson(question(1, "essay"))
                .replace("\"status\": \"ready\"", "\"status\": \"ready\", \"future_field\": 42")

        val session = json.decodeFromString<QuizDto>(payload).toSession()

        assertEquals(1, session.questions.size)
    }

    @Test
    fun `unknown fields inside a question do not break parsing`() {
        val payload = quizJson(question(1, "mcq", ""","options":["a"],"tts_url":"x""""))

        val session = json.decodeFromString<QuizDto>(payload).toSession()

        assertTrue(session.questions.single() is Question.MultipleChoice)
    }

    @Test
    fun `questions are ordered by ordinal, not by array position`() {
        val payload =
            quizJson(question(3, "essay"), question(1, "essay"), question(2, "essay"))

        val session = json.decodeFromString<QuizDto>(payload).toSession()

        assertEquals(
            listOf("Soal 1", "Soal 2", "Soal 3"),
            session.questions.map { (it as Question.ShortAnswer).stem },
        )
    }

    @Test
    fun `an mcq without options maps to an empty option list, not to null`() {
        val payload = quizJson(question(1, "mcq"))

        val session = json.decodeFromString<QuizDto>(payload).toSession()

        assertEquals(emptyList<String>(), (session.questions.single() as Question.MultipleChoice).options)
    }

    @Test
    fun `session and material ids survive the mapping`() {
        val session = json.decodeFromString<QuizDto>(quizJson(question(1, "mcq"))).toSession()

        assertEquals("11111111-1111-1111-1111-111111111111", session.sessionId)
        assertEquals("22222222-2222-2222-2222-222222222222", session.materialId)
    }
}

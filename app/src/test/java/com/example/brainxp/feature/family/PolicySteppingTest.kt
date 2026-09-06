package com.example.brainxp.feature.family

import com.example.brainxp.domain.model.PolicyLimits
import com.example.brainxp.domain.model.UploadMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicySteppingTest {
    private val base =
        PolicyUiState(
            subjectName = "Raka",
            questionsPerSession = 8,
            essayCount = 2,
            baseRewardSeconds = 60,
            dailyCapMinutes = List(PolicyLimits.WEEK_DAYS) { 60 },
            dailyGrantMinutes = List(PolicyLimits.WEEK_DAYS) { 0 },
            idleDaysAllowed = 2,
            dayResetHour = 4,
            uploadMethods = UploadMethod.entries.toSet(),
            apps = emptyList(),
        )

    private fun PolicyUiState.step(
        event: PolicyEvent,
        times: Int,
    ): PolicyUiState = (1..times).fold(this) { state, _ -> state.stepped(event) }

    @Test
    fun `stepping the reward always lands inside the server bounds`() {
        var state = base
        repeat(40) {
            state = state.stepped(PolicyEvent.StepBaseReward)
            assertTrue(
                "reward ${state.baseRewardSeconds} outside bounds",
                state.baseRewardSeconds in PolicyLimits.BASE_REWARD_SECONDS,
            )
        }
    }

    @Test
    fun `the reward cycles back round rather than sticking at the top`() {
        val seen = mutableSetOf<Int>()
        var state = base
        repeat(20) {
            state = state.stepped(PolicyEvent.StepBaseReward)
            seen += state.baseRewardSeconds
        }

        assertTrue("reward never moved", seen.size > 1)
        assertTrue("reward never came back round", seen.contains(base.baseRewardSeconds))
    }

    @Test
    fun `questions per session never leave the server bounds`() {
        var state = base
        repeat(20) {
            state = state.stepped(PolicyEvent.StepQuestions)
            assertTrue(
                "questions ${state.questionsPerSession} out of range",
                state.questionsPerSession in PolicyLimits.QUESTIONS_PER_SESSION,
            )
        }
    }

    @Test
    fun `essays never outnumber questions however much you step either`() {
        var state = base
        repeat(30) { index ->
            state = state.stepped(if (index % 3 == 0) PolicyEvent.StepQuestions else PolicyEvent.StepEssay)
            assertTrue(
                "${state.essayCount} essays in ${state.questionsPerSession} questions",
                state.essayCount <= state.questionsPerSession,
            )
        }
    }

    @Test
    fun `idle days and the reset hour stay inside their server bounds`() {
        val idle = base.step(PolicyEvent.StepIdleDays, times = 30)
        val hour = base.step(PolicyEvent.StepResetHour, times = 40)

        assertTrue(idle.idleDaysAllowed in PolicyLimits.IDLE_DAYS_ALLOWED)
        assertTrue(hour.dayResetHour in PolicyLimits.DAY_RESET_HOUR)
    }

    @Test
    fun `turning off one upload method leaves the other standing`() {
        val stepped = base.stepped(PolicyEvent.ToggleUploadMethod(UploadMethod.PHOTO))

        assertEquals(setOf(UploadMethod.DOCUMENT), stepped.uploadMethods)
    }

    @Test
    fun `the last upload method cannot be turned off`() {
        val onlyDocument = base.copy(uploadMethods = setOf(UploadMethod.DOCUMENT))

        val stepped = onlyDocument.stepped(PolicyEvent.ToggleUploadMethod(UploadMethod.DOCUMENT))

        assertEquals(setOf(UploadMethod.DOCUMENT), stepped.uploadMethods)
    }

    @Test
    fun `a method turned off can be turned back on`() {
        val stepped =
            base
                .stepped(PolicyEvent.ToggleUploadMethod(UploadMethod.PHOTO))
                .stepped(PolicyEvent.ToggleUploadMethod(UploadMethod.PHOTO))

        assertEquals(UploadMethod.entries.toSet(), stepped.uploadMethods)
    }
}

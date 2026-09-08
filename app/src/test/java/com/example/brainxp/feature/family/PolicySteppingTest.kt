package com.example.brainxp.feature.family

import com.example.brainxp.domain.model.PolicyLimits
import com.example.brainxp.domain.model.PolicyStep
import com.example.brainxp.domain.model.StepDirection
import com.example.brainxp.domain.model.UploadMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    private fun PolicyUiState.nudge(
        step: PolicyStep,
        direction: StepDirection,
        times: Int = 1,
    ): PolicyUiState = (1..times).fold(this) { state, _ -> state.stepped(PolicyEvent.Nudge(step, direction)) }

    @Test
    fun `the reward climbs and falls through the step list without wrapping`() {
        assertEquals(90, base.nudge(PolicyStep.BaseReward, StepDirection.UP).baseRewardSeconds)
        assertEquals(45, base.nudge(PolicyStep.BaseReward, StepDirection.DOWN).baseRewardSeconds)
    }

    @Test
    fun `holding the reward down stops at the floor instead of jumping to the ceiling`() {
        val floored = base.nudge(PolicyStep.BaseReward, StepDirection.DOWN, times = 40)

        assertEquals(PolicyLimits.BASE_REWARD_SECONDS.first, floored.baseRewardSeconds)
    }

    @Test
    fun `holding the reward up stops at the ceiling`() {
        val capped = base.nudge(PolicyStep.BaseReward, StepDirection.UP, times = 40)

        assertEquals(PolicyLimits.BASE_REWARD_SECONDS.last, capped.baseRewardSeconds)
    }

    @Test
    fun `questions per session can reach every value the server allows`() {
        val reachable = mutableSetOf<Int>()
        var state = base.nudge(PolicyStep.Questions, StepDirection.DOWN, times = 20)
        repeat(20) {
            reachable += state.questionsPerSession
            state = state.nudge(PolicyStep.Questions, StepDirection.UP)
        }

        assertTrue("odd counts unreachable: $reachable", reachable.any { it % 2 == 1 })
    }

    @Test
    fun `questions per session never leaves the server bounds`() {
        var state = base
        repeat(20) {
            state = state.nudge(PolicyStep.Questions, StepDirection.UP)
            assertTrue(state.questionsPerSession in PolicyLimits.QUESTIONS_PER_SESSION)
        }
    }

    @Test
    fun `essays never outnumber the questions they sit in`() {
        var state = base
        repeat(30) { index ->
            state =
                if (index % 3 == 0) {
                    state.nudge(PolicyStep.Questions, StepDirection.DOWN)
                } else {
                    state.nudge(PolicyStep.Essays, StepDirection.UP)
                }
            assertTrue(state.essayCount <= state.questionsPerSession)
        }
    }

    @Test
    fun `idle days and the reset hour stay inside their bounds`() {
        val idle = base.nudge(PolicyStep.IdleDays, StepDirection.UP, times = 30)
        val hour = base.nudge(PolicyStep.ResetHour, StepDirection.UP, times = 40)

        assertTrue(idle.idleDaysAllowed in PolicyLimits.IDLE_DAYS_ALLOWED)
        assertTrue(hour.dayResetHour in PolicyLimits.DAY_RESET_HOUR)
    }

    @Test
    fun `a stepper reports when it can no longer move`() {
        val floored = base.nudge(PolicyStep.IdleDays, StepDirection.DOWN, times = 30)

        assertFalse(floored.canNudge(PolicyStep.IdleDays, StepDirection.DOWN))
        assertTrue(floored.canNudge(PolicyStep.IdleDays, StepDirection.UP))
    }

    @Test
    fun `turning off one upload method leaves the other standing`() {
        val stepped = base.stepped(PolicyEvent.ToggleUpload(UploadMethod.PHOTO))

        assertEquals(setOf(UploadMethod.DOCUMENT), stepped.uploadMethods)
    }

    @Test
    fun `the last upload method cannot be turned off`() {
        val onlyDocument = base.copy(uploadMethods = setOf(UploadMethod.DOCUMENT))

        val stepped = onlyDocument.stepped(PolicyEvent.ToggleUpload(UploadMethod.DOCUMENT))

        assertEquals(setOf(UploadMethod.DOCUMENT), stepped.uploadMethods)
    }

    @Test
    fun `a whole week of caps arrives as one change`() {
        val week = List(PolicyLimits.WEEK_DAYS) { 90 }

        assertEquals(week, base.stepped(PolicyEvent.SetCaps(week)).dailyCapMinutes)
        assertEquals(week, base.stepped(PolicyEvent.SetGrants(week)).dailyGrantMinutes)
    }

    @Test
    fun `questions climb one at a time and stop at ten`() {
        var state = base.nudge(PolicyStep.Questions, StepDirection.DOWN, times = 20)
        assertEquals(PolicyLimits.QUESTIONS_PER_SESSION.first, state.questionsPerSession)

        val climbed =
            (1..3).map {
                state = state.nudge(PolicyStep.Questions, StepDirection.UP)
                state.questionsPerSession
            }

        assertEquals(listOf(2, 3, 4), climbed)
        assertEquals(
            PolicyLimits.QUESTIONS_PER_SESSION.last,
            state.nudge(PolicyStep.Questions, StepDirection.UP, times = 20).questionsPerSession,
        )
    }

    @Test
    fun `a single question can be the essay, leaving no multiple choice`() {
        val one = base.copy(questionsPerSession = 1, essayCount = 1)

        assertEquals(1, one.essayCount)
        assertEquals(0, one.mcqCount)
        assertEquals(100, one.essayPercent)
    }

    @Test
    fun `shrinking the questions drags the essay count down with it`() {
        val shrunk =
            base
                .copy(questionsPerSession = 10, essayCount = 8)
                .nudge(PolicyStep.Questions, StepDirection.DOWN, times = 9)

        assertEquals(PolicyLimits.QUESTIONS_PER_SESSION.first, shrunk.questionsPerSession)
        assertTrue(shrunk.essayCount <= shrunk.questionsPerSession)
    }
}

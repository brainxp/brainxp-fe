package com.example.brainxp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyNudgingTest {
    private val base =
        PolicyDraft(
            questionsPerSession = 8,
            essayCount = 2,
            baseRewardSeconds = 60,
            dailyCapSeconds = List(PolicyLimits.WEEK_DAYS) { 3_600 },
            dailyGrantSeconds = List(PolicyLimits.WEEK_DAYS) { 0 },
            idleDaysAllowed = 2,
            dayResetHour = 4,
            uploadMethods = UploadMethod.entries.toSet(),
        )

    private fun PolicyDraft.nudge(
        step: PolicyStep,
        direction: StepDirection,
        times: Int,
    ): PolicyDraft = (1..times).fold(this) { draft, _ -> draft.nudged(step, direction) }

    @Test
    fun `up then down returns every rule to where it started`() {
        val steps =
            listOf(
                PolicyStep.BaseReward,
                PolicyStep.Questions,
                PolicyStep.Essays,
                PolicyStep.ResetHour,
                PolicyStep.IdleDays,
            )

        steps.forEach { step ->
            val roundTrip =
                base
                    .nudged(step, StepDirection.UP)
                    .nudged(step, StepDirection.DOWN)

            assertEquals("$step did not come back", base, roundTrip)
        }
    }

    @Test
    fun `stepping down holds at the floor instead of wrapping to the top`() {
        val reward = base.nudge(PolicyStep.BaseReward, StepDirection.DOWN, times = 20)
        val hour = base.nudge(PolicyStep.ResetHour, StepDirection.DOWN, times = 20)
        val idle = base.nudge(PolicyStep.IdleDays, StepDirection.DOWN, times = 20)

        assertEquals(PolicyLimits.REWARD_STEPS.first(), reward.baseRewardSeconds)
        assertEquals(PolicyLimits.DAY_RESET_HOUR.first, hour.dayResetHour)
        assertEquals(PolicyLimits.IDLE_DAYS_ALLOWED.first, idle.idleDaysAllowed)
    }

    @Test
    fun `stepping up holds at the ceiling instead of wrapping to the bottom`() {
        val reward = base.nudge(PolicyStep.BaseReward, StepDirection.UP, times = 20)
        val hour = base.nudge(PolicyStep.ResetHour, StepDirection.UP, times = 40)
        val questions = base.nudge(PolicyStep.Questions, StepDirection.UP, times = 20)

        assertEquals(PolicyLimits.REWARD_STEPS.last(), reward.baseRewardSeconds)
        assertEquals(PolicyLimits.DAY_RESET_HOUR.last, hour.dayResetHour)
        assertEquals(PolicyLimits.QUESTIONS_PER_SESSION.last, questions.questionsPerSession)
    }

    @Test
    fun `essays never outnumber questions in either direction`() {
        var draft = base
        repeat(30) { index ->
            draft =
                when {
                    index % 3 == 0 -> draft.nudged(PolicyStep.Questions, StepDirection.DOWN)
                    else -> draft.nudged(PolicyStep.Essays, StepDirection.UP)
                }
            assertTrue(
                "${draft.essayCount} essays in ${draft.questionsPerSession} questions",
                draft.essayCount <= draft.questionsPerSession,
            )
        }
    }

    @Test
    fun `every rule stays inside the server bounds however far it is pushed`() {
        val pushed =
            base
                .nudge(PolicyStep.BaseReward, StepDirection.UP, times = 30)
                .nudge(PolicyStep.Questions, StepDirection.UP, times = 30)
                .nudge(PolicyStep.IdleDays, StepDirection.UP, times = 30)
                .nudge(PolicyStep.ResetHour, StepDirection.UP, times = 30)

        assertTrue(pushed.baseRewardSeconds in PolicyLimits.BASE_REWARD_SECONDS)
        assertTrue(pushed.questionsPerSession in PolicyLimits.QUESTIONS_PER_SESSION)
        assertTrue(pushed.idleDaysAllowed in PolicyLimits.IDLE_DAYS_ALLOWED)
        assertTrue(pushed.dayResetHour in PolicyLimits.DAY_RESET_HOUR)
    }

    @Test
    fun `a rule parked at its limit reports that it cannot move further`() {
        val topped = base.nudge(PolicyStep.IdleDays, StepDirection.UP, times = 30)
        val floored = base.nudge(PolicyStep.IdleDays, StepDirection.DOWN, times = 30)

        assertFalse(topped.canNudge(PolicyStep.IdleDays, StepDirection.UP))
        assertTrue(topped.canNudge(PolicyStep.IdleDays, StepDirection.DOWN))
        assertFalse(floored.canNudge(PolicyStep.IdleDays, StepDirection.DOWN))
        assertTrue(floored.canNudge(PolicyStep.IdleDays, StepDirection.UP))
    }
}

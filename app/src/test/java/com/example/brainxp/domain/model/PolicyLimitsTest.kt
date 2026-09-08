package com.example.brainxp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyLimitsTest {
    private val base =
        PolicyDraft(
            questionsPerSession = 8,
            essayCount = 2,
            baseRewardSeconds = 60,
            dailyCapSeconds = List(PolicyLimits.WEEK_DAYS) { 3_600 },
            dailyGrantSeconds = List(PolicyLimits.WEEK_DAYS) { 600 },
            idleDaysAllowed = 3,
            dayResetHour = 5,
            uploadMethods = UploadMethod.entries.toSet(),
        )

    @Test
    fun `a reward below the floor is lifted to the floor`() {
        assertEquals(15, PolicyLimits.clamp(base.copy(baseRewardSeconds = 1)).baseRewardSeconds)
    }

    @Test
    fun `a reward above the ceiling is cut to the ceiling`() {
        assertEquals(1_800, PolicyLimits.clamp(base.copy(baseRewardSeconds = 9_999)).baseRewardSeconds)
    }

    @Test
    fun `both reward bounds are accepted untouched`() {
        assertEquals(15, PolicyLimits.clamp(base.copy(baseRewardSeconds = 15)).baseRewardSeconds)
        assertEquals(1_800, PolicyLimits.clamp(base.copy(baseRewardSeconds = 1_800)).baseRewardSeconds)
    }

    @Test
    fun `questions per session stay within one and ten`() {
        assertEquals(1, PolicyLimits.clamp(base.copy(questionsPerSession = 0)).questionsPerSession)
        assertEquals(10, PolicyLimits.clamp(base.copy(questionsPerSession = 40)).questionsPerSession)
    }

    @Test
    fun `essays never outnumber the questions they sit in`() {
        assertEquals(4, PolicyLimits.clamp(base.copy(questionsPerSession = 4, essayCount = 9)).essayCount)
        assertEquals(0, PolicyLimits.clamp(base.copy(essayCount = -3)).essayCount)
    }

    @Test
    fun `the reset hour stays inside a day`() {
        assertEquals(0, PolicyLimits.clamp(base.copy(dayResetHour = -1)).dayResetHour)
        assertEquals(23, PolicyLimits.clamp(base.copy(dayResetHour = 99)).dayResetHour)
    }

    @Test
    fun `idle days stay within a fortnight`() {
        assertEquals(0, PolicyLimits.clamp(base.copy(idleDaysAllowed = -5)).idleDaysAllowed)
        assertEquals(14, PolicyLimits.clamp(base.copy(idleDaysAllowed = 30)).idleDaysAllowed)
    }

    @Test
    fun `a short week is padded to seven days`() {
        val clamped = PolicyLimits.clamp(base.copy(dailyCapSeconds = listOf(60), dailyGrantSeconds = emptyList()))

        assertEquals(PolicyLimits.WEEK_DAYS, clamped.dailyCapSeconds.size)
        assertEquals(PolicyLimits.WEEK_DAYS, clamped.dailyGrantSeconds.size)
    }

    @Test
    fun `a long week is trimmed to seven days`() {
        val clamped = PolicyLimits.clamp(base.copy(dailyCapSeconds = List(30) { 60 }))

        assertEquals(PolicyLimits.WEEK_DAYS, clamped.dailyCapSeconds.size)
    }

    @Test
    fun `negative day figures are floored at zero`() {
        val clamped = PolicyLimits.clamp(base.copy(dailyCapSeconds = List(PolicyLimits.WEEK_DAYS) { -60 }))

        assertTrue(clamped.dailyCapSeconds.all { it == 0 })
    }

    @Test
    fun `an empty set of upload methods would strand the user, so both come back`() {
        val clamped = PolicyLimits.clamp(base.copy(uploadMethods = emptySet()))

        assertEquals(UploadMethod.entries.toSet(), clamped.uploadMethods)
    }

    @Test
    fun `an essay count survives the trip through a ratio and back`() {
        (1..PolicyLimits.QUESTIONS_PER_SESSION.last).forEach { questions ->
            (0..questions).forEach { essays ->
                val ratio = PolicyLimits.essayRatioOf(essays, questions)
                assertEquals(
                    "$essays of $questions",
                    essays,
                    PolicyLimits.essayCountOf(ratio, questions),
                )
            }
        }
    }

    @Test
    fun `one essay in six does not round away to none`() {
        val ratio = PolicyLimits.essayRatioOf(essayCount = 1, questionsPerSession = 6)

        assertEquals(1, PolicyLimits.essayCountOf(ratio, questionsPerSession = 6))
    }

    @Test
    fun `a ratio is never reported outside zero and one`() {
        assertEquals(0.0, PolicyLimits.essayRatioOf(essayCount = -4, questionsPerSession = 10), 0.0001)
        assertEquals(1.0, PolicyLimits.essayRatioOf(essayCount = 40, questionsPerSession = 10), 0.0001)
    }

    @Test
    fun `a session with no questions reports no essay ratio instead of dividing by zero`() {
        assertEquals(0.0, PolicyLimits.essayRatioOf(essayCount = 3, questionsPerSession = 0), 0.0001)
    }

    @Test
    fun `the reward steps reach both ends of what the server allows`() {
        assertEquals(PolicyLimits.BASE_REWARD_SECONDS.first, PolicyLimits.REWARD_STEPS.first())
        assertEquals(PolicyLimits.BASE_REWARD_SECONDS.last, PolicyLimits.REWARD_STEPS.last())
    }

    @Test
    fun `every reward step is a value the server would accept`() {
        PolicyLimits.REWARD_STEPS.forEach { step ->
            assertTrue("$step outside the server bounds", step in PolicyLimits.BASE_REWARD_SECONDS)
        }
    }

    @Test
    fun `the reward steps only ever climb`() {
        assertEquals(PolicyLimits.REWARD_STEPS.sorted(), PolicyLimits.REWARD_STEPS)
        assertEquals(PolicyLimits.REWARD_STEPS.distinct(), PolicyLimits.REWARD_STEPS)
    }

    private fun rewardAfter(
        from: Int,
        direction: StepDirection,
    ): Int = base.copy(baseRewardSeconds = from).nudged(PolicyStep.BaseReward, direction).baseRewardSeconds

    @Test
    fun `nudging a reward that sits on a step moves to the neighbouring step`() {
        assertEquals(90, rewardAfter(from = 60, direction = StepDirection.UP))
        assertEquals(45, rewardAfter(from = 60, direction = StepDirection.DOWN))
    }

    @Test
    fun `a reward the server set between steps climbs to the next step up`() {
        assertEquals(180, rewardAfter(from = 137, direction = StepDirection.UP))
    }

    @Test
    fun `a reward the server set between steps falls to the next step down`() {
        assertEquals(120, rewardAfter(from = 137, direction = StepDirection.DOWN))
    }

    @Test
    fun `an off-step reward never jumps back to the floor`() {
        val climbed = rewardAfter(from = 1_000, direction = StepDirection.UP)

        assertEquals(1_200, climbed)
    }

    @Test
    fun `the ends of the step list hold instead of wrapping`() {
        assertEquals(15, rewardAfter(from = 15, direction = StepDirection.DOWN))
        assertEquals(1_800, rewardAfter(from = 1_800, direction = StepDirection.UP))
    }
}

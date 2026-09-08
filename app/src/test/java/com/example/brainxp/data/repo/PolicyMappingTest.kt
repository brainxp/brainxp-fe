package com.example.brainxp.data.repo

import com.example.brainxp.domain.model.PolicyDraft
import com.example.brainxp.domain.model.PolicyLimits
import com.example.brainxp.domain.model.UploadMethod
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyMappingTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
            isLenient = true
        }

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
    fun `every rule the editor owns reaches the wire`() {
        val body = json.encodeToString(base.toPatch())

        listOf(
            "questions_per_session",
            "base_reward_seconds",
            "essay_ratio",
            "daily_caps",
            "daily_grants",
            "day_reset_hour",
            "idle_days_allowed",
            "allowed_upload_methods",
        ).forEach { key ->
            assertTrue("$key missing from $body", body.contains(key))
        }
    }

    @Test
    fun `the reward per question is carried in seconds`() {
        assertEquals(90, base.copy(baseRewardSeconds = 90).toPatch().baseRewardSeconds)
    }

    @Test
    fun `an essay count becomes a ratio the server understands`() {
        assertEquals(0.25, base.copy(questionsPerSession = 8, essayCount = 2).toPatch().essayRatio!!, 0.0001)
    }

    @Test
    fun `values outside the server bounds are clamped before sending`() {
        val patch = base.copy(questionsPerSession = 99, baseRewardSeconds = 5, idleDaysAllowed = 40, dayResetHour = 26).toPatch()

        assertEquals(10, patch.questionsPerSession)
        assertEquals(15, patch.baseRewardSeconds)
        assertEquals(14, patch.idleDaysAllowed)
        assertEquals(23, patch.dayResetHour)
    }

    @Test
    fun `a week always leaves as seven figures`() {
        val patch = base.copy(dailyCapSeconds = listOf(60), dailyGrantSeconds = List(20) { 30 }).toPatch()

        assertEquals(PolicyLimits.WEEK_DAYS, patch.dailyCaps?.size)
        assertEquals(PolicyLimits.WEEK_DAYS, patch.dailyGrants?.size)
    }

    @Test
    fun `upload methods travel as the wire names the server expects`() {
        val patch = base.copy(uploadMethods = setOf(UploadMethod.DOCUMENT)).toPatch()

        assertEquals(listOf("document"), patch.allowedUploadMethods)
    }

    @Test
    fun `a policy read from the server round-trips back into an identical draft`() {
        val original = base.copy(questionsPerSession = 6, essayCount = 1)

        val restored =
            original.toPatch().let { patch ->
                original.copy(
                    essayCount = PolicyLimits.essayCountOf(patch.essayRatio!!, patch.questionsPerSession!!),
                )
            }

        assertEquals(original.essayCount, restored.essayCount)
    }
}

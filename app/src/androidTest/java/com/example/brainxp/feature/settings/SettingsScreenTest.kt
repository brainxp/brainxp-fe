package com.example.brainxp.feature.settings

import androidx.annotation.StringRes
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.example.brainxp.R
import com.example.brainxp.data.repo.SubjectPolicy
import com.example.brainxp.domain.model.AcademicLevel
import com.example.brainxp.domain.model.PolicyLimits
import com.example.brainxp.domain.model.PolicyStep
import com.example.brainxp.domain.model.UploadMethod
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun str(
        @StringRes id: Int,
    ) = context.getString(id)

    private val policy =
        SubjectPolicy(
            level = AcademicLevel.SMA,
            language = LANGUAGE_ID,
            questionsPerSession = 8,
            dayResetHour = 4,
            idleDaysAllowed = 2,
            pendingWeakenAt = null,
            essayCount = 2,
            dailyCapSeconds = List(PolicyLimits.WEEK_DAYS) { 3_600 },
            dailyGrantSeconds = List(PolicyLimits.WEEK_DAYS) { 0 },
            lockedApps = emptyList(),
            baseRewardSeconds = 60,
            uploadMethods = UploadMethod.entries.toSet(),
        )

    private fun tapRow(
        @StringRes title: Int,
    ): SettingsPolicyEdit? {
        var reported: SettingsPolicyEdit? = null
        compose.setContent {
            SettingsScreen(
                policy = policy,
                onEdit = { reported = it },
                onApps = {},
                onPermissions = {},
                onSignOut = {},
                onPrivacyPolicy = {},
                onDeleteAccount = {},
            )
        }
        compose.onNodeWithText(str(title)).performScrollTo().performClick()
        return reported
    }

    @Test
    fun theRewardRowStepsTheRewardPerQuestion() {
        assertEquals(SettingsPolicyEdit.Rule(PolicyStep.BaseReward), tapRow(R.string.settings_reward))
    }

    @Test
    fun theQuestionRowStepsTheQuestionCount() {
        assertEquals(SettingsPolicyEdit.Rule(PolicyStep.Questions), tapRow(R.string.settings_questions))
    }

    @Test
    fun theEssayRowStepsTheEssayShare() {
        assertEquals(SettingsPolicyEdit.Rule(PolicyStep.Essays), tapRow(R.string.settings_essay))
    }

    @Test
    fun theResetHourRowStepsTheResetHour() {
        assertEquals(SettingsPolicyEdit.Rule(PolicyStep.ResetHour), tapRow(R.string.settings_reset_hour))
    }

    @Test
    fun theIdleRowStepsTheDaysOffAllowance() {
        assertEquals(SettingsPolicyEdit.Rule(PolicyStep.IdleDays), tapRow(R.string.settings_idle_allowed))
    }

    @Test
    fun thePhotoRowTogglesThatUploadMethod() {
        assertEquals(
            SettingsPolicyEdit.Rule(PolicyStep.Upload(UploadMethod.PHOTO)),
            tapRow(R.string.settings_upload_photo),
        )
    }

    @Test
    fun theDocumentRowTogglesThatUploadMethod() {
        assertEquals(
            SettingsPolicyEdit.Rule(PolicyStep.Upload(UploadMethod.DOCUMENT)),
            tapRow(R.string.settings_upload_document),
        )
    }

    @Test
    fun everyRuleRowIsReachableAndReportsSomething() {
        listOf(
            R.string.settings_reward,
            R.string.settings_questions,
            R.string.settings_essay,
            R.string.settings_reset_hour,
            R.string.settings_idle_allowed,
        ).forEach { row ->
            assertEquals("row ${str(row)} reported nothing", true, tapRow(row) != null)
        }
    }
}

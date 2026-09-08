package com.example.brainxp.feature.settings

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.data.repo.SubjectPolicy
import com.example.brainxp.data.repo.toDraft
import com.example.brainxp.domain.model.AcademicLevel
import com.example.brainxp.domain.model.DeviceRole
import com.example.brainxp.domain.model.PolicyLimits
import com.example.brainxp.domain.model.UploadMethod
import org.junit.Rule
import org.junit.Test

class ManagedSettingsTest {
    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun str(
        @StringRes id: Int,
    ) = context.getString(id)

    private val policy =
        SubjectPolicy(
            level = AcademicLevel.SMP,
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

    private fun show(managed: Boolean) {
        compose.setContent {
            BrainXPTheme {
                SettingsScreen(
                    state =
                        SettingsUiState(
                            policy = policy,
                            draft = policy.toDraft(),
                            loading = false,
                            role = if (managed) DeviceRole.CHILD else DeviceRole.PARENT,
                        ),
                    onEdit = {},
                    onSave = {},
                    onDiscard = {},
                    onApps = {},
                    onPermissions = {},
                    onSignOut = {},
                    onPrivacyPolicy = {},
                    onDeleteAccount = {},
                )
            }
        }
    }

    @Test
    fun aManagedChildKeepsSystemPermissionsAndThePrivacyPolicy() {
        show(managed = true)

        compose.onNodeWithText(str(R.string.settings_permissions)).assertExists().assertHasClickAction()
        compose.onNodeWithText(str(R.string.settings_privacy)).assertExists().assertHasClickAction()
    }

    @Test
    fun aManagedChildIsToldWhoHoldsTheRules() {
        show(managed = true)

        compose.onNodeWithText(str(R.string.settings_managed)).assertExists()
    }

    @Test
    fun aManagedChildCannotReachTheLockedAppList() {
        show(managed = true)

        compose.onNodeWithText(str(R.string.policy_apps)).assertDoesNotExist()
    }

    @Test
    fun aManagedChildSeesNoRuleControls() {
        show(managed = true)

        listOf(
            R.string.settings_level,
            R.string.settings_language,
            R.string.settings_reward,
            R.string.settings_questions,
            R.string.settings_essay,
            R.string.policy_reset_hour,
            R.string.settings_idle_allowed,
        ).forEach { row ->
            compose.onNodeWithText(str(row)).assertDoesNotExist()
        }
    }

    @Test
    fun aManagedChildCanNeitherSignOutNorDeleteTheAccount() {
        show(managed = true)

        compose.onNodeWithText(str(R.string.settings_sign_out)).assertDoesNotExist()
        compose.onNodeWithText(str(R.string.settings_delete_account)).assertDoesNotExist()
    }

    @Test
    fun aSelfManagedDeviceStillGetsEveryControl() {
        show(managed = false)

        compose.onNodeWithText(str(R.string.settings_level)).assertExists()
        compose.onNodeWithText(str(R.string.settings_reward)).assertExists()
        compose.onNodeWithText(str(R.string.policy_apps)).assertExists()
        compose.onNodeWithText(str(R.string.settings_sign_out)).assertExists()
    }
}

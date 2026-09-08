package com.example.brainxp.feature.settings

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.example.brainxp.R
import com.example.brainxp.data.repo.SubjectPolicy
import com.example.brainxp.domain.model.AcademicLevel
import com.example.brainxp.domain.model.DeviceRole
import com.example.brainxp.domain.model.PolicyDraft
import com.example.brainxp.domain.model.PolicyLimits
import com.example.brainxp.domain.model.PolicyStep
import com.example.brainxp.domain.model.StepDirection
import com.example.brainxp.domain.model.UploadMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val ROW_QUESTIONS = 0
private const val ROW_ESSAY = 1
private const val ROW_REWARD = 2

class SettingsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun str(
        @StringRes id: Int,
    ) = context.getString(id)

    private val draft =
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

    private fun policy(pendingWeakenAt: String? = null) =
        SubjectPolicy(
            level = AcademicLevel.SMA,
            language = LANGUAGE_ID,
            questionsPerSession = draft.questionsPerSession,
            dayResetHour = draft.dayResetHour,
            idleDaysAllowed = draft.idleDaysAllowed,
            pendingWeakenAt = pendingWeakenAt,
            essayCount = draft.essayCount,
            dailyCapSeconds = draft.dailyCapSeconds,
            dailyGrantSeconds = draft.dailyGrantSeconds,
            lockedApps = emptyList(),
            baseRewardSeconds = draft.baseRewardSeconds,
            uploadMethods = draft.uploadMethods,
        )

    private var reported: SettingsPolicyEdit? = null
    private var saved = false
    private var discarded = false
    private var signedOut = false

    private fun show(state: SettingsUiState = SettingsUiState(loading = false, policy = policy(), draft = draft)) {
        compose.setContent {
            SettingsScreen(
                state = state,
                onEdit = { reported = it },
                onSave = { saved = true },
                onDiscard = { discarded = true },
                onApps = {},
                onPermissions = {},
                onSignOut = { signedOut = true },
                onPrivacyPolicy = {},
                onDeleteAccount = {},
            )
        }
    }

    private fun stepper(
        row: Int,
        direction: StepDirection,
    ) = compose
        .onAllNodesWithContentDescription(
            str(if (direction == StepDirection.UP) R.string.stepper_increase else R.string.stepper_decrease),
        )[row]

    private fun tapStepper(
        row: Int,
        direction: StepDirection,
    ): SettingsPolicyEdit? {
        stepper(row, direction).performScrollTo().performClick()
        return reported
    }

    @Test
    fun theEssayStepperEditsTheDraftRatherThanSavingAtOnce() {
        show()

        val edit = tapStepper(ROW_ESSAY, StepDirection.UP)

        assertEquals(SettingsPolicyEdit.Draft(DraftChange.Step(PolicyStep.Essays, StepDirection.UP)), edit)
        assertFalse("editing must not save on its own", saved)
    }

    @Test
    fun theRewardStepperStepsDown() {
        show()

        assertEquals(
            SettingsPolicyEdit.Draft(DraftChange.Step(PolicyStep.BaseReward, StepDirection.DOWN)),
            tapStepper(ROW_REWARD, StepDirection.DOWN),
        )
    }

    @Test
    fun savingIsClosedOffUntilSomethingActuallyChanged() {
        show()

        compose.onNodeWithText(str(R.string.policy_save)).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun savingOpensUpOnceTheDraftDiffersFromTheServer() {
        show(
            SettingsUiState(
                loading = false,
                policy = policy(),
                draft = draft.copy(idleDaysAllowed = draft.idleDaysAllowed + 1),
            ),
        )

        val save = compose.onNodeWithText(str(R.string.policy_save)).performScrollTo()
        save.assertIsEnabled()
        save.performClick()

        assertTrue(saved)
    }

    @Test
    fun unsavedEditsCanBePutBack() {
        show(
            SettingsUiState(
                loading = false,
                policy = policy(),
                draft = draft.copy(idleDaysAllowed = draft.idleDaysAllowed + 1),
            ),
        )

        compose.onNodeWithText(str(R.string.policy_discard)).performScrollTo().performClick()

        assertTrue(discarded)
    }

    @Test
    fun thereIsNothingToPutBackWhenNothingChanged() {
        show()

        compose.onNodeWithText(str(R.string.policy_discard)).assertDoesNotExist()
    }

    @Test
    fun aManagedChildCannotStepOrSaveAnything() {
        show(
            SettingsUiState(
                loading = false,
                policy = policy(),
                draft = draft.copy(idleDaysAllowed = draft.idleDaysAllowed + 1),
                role = DeviceRole.CHILD,
            ),
        )

        compose
            .onAllNodesWithContentDescription(str(R.string.stepper_increase))
            .assertCountEquals(0)
        compose.onNodeWithText(str(R.string.policy_save)).assertDoesNotExist()
        assertNull(reported)
    }

    @Test
    fun signingOutAsksInADialogBeforeItHappens() {
        show()

        compose.onNodeWithText(str(R.string.settings_sign_out)).performScrollTo().performClick()

        compose.onNodeWithText(str(R.string.settings_sign_out_warning)).assertExists()
        assertFalse("the dialog must not sign out on its own", signedOut)

        compose.onNodeWithText(str(R.string.settings_sign_out_confirm)).performClick()
        assertTrue(signedOut)
    }

    @Test
    fun backingOutOfTheSignOutDialogKeepsYouSignedIn() {
        show()

        compose.onNodeWithText(str(R.string.settings_sign_out)).performScrollTo().performClick()
        compose.onNodeWithText(str(R.string.detail_delete_cancel)).performClick()

        compose.onNodeWithText(str(R.string.settings_sign_out_warning)).assertDoesNotExist()
        assertFalse(signedOut)
    }

    @Test
    fun aPendingLooseningIsSpelledOutAsADateRatherThanARawTimestamp() {
        show(
            SettingsUiState(
                loading = false,
                policy = policy(pendingWeakenAt = "2026-09-07T14:34:25.086513Z"),
                draft = draft,
            ),
        )

        compose.onNodeWithText("2026-09-07T14:34:25.086513Z", substring = true).assertDoesNotExist()
    }

    @Test
    fun theUploadRowsStillToggleTheirMethod() {
        show()

        compose.onNodeWithText(str(R.string.settings_upload_photo)).performScrollTo().performClick()

        assertEquals(
            SettingsPolicyEdit.Draft(DraftChange.Upload(UploadMethod.PHOTO)),
            reported,
        )
    }

    @Test
    fun theRuleRowsKeepTheOrderTheTestsAddress() {
        show()

        assertEquals(
            SettingsPolicyEdit.Draft(DraftChange.Step(PolicyStep.Questions, StepDirection.UP)),
            tapStepper(ROW_QUESTIONS, StepDirection.UP),
        )
    }
}

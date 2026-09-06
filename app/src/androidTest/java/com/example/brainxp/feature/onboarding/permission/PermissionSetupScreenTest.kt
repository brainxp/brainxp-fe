package com.example.brainxp.feature.onboarding.permission

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.example.brainxp.R
import com.example.brainxp.core.permission.PermissionEntry
import com.example.brainxp.core.permission.PermissionSnapshot
import com.example.brainxp.core.permission.SpecialPermission
import com.example.brainxp.core.ui.BrainXPTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PermissionSetupScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun str(
        @StringRes id: Int,
    ) = context.getString(id)

    private fun state(granted: Set<SpecialPermission>) =
        PermissionSnapshot(SpecialPermission.entries.map { PermissionEntry(it, it in granted) }).toSetupState()

    private fun show(
        granted: Set<SpecialPermission> = emptySet(),
        onOpenSettings: (SpecialPermission) -> Unit = {},
        onContinue: () -> Unit = {},
        reentrant: Boolean = false,
    ) {
        compose.setContent {
            BrainXPTheme {
                PermissionSetupScreen(
                    state = state(granted),
                    onOpenSettings = onOpenSettings,
                    onContinue = onContinue,
                    reentrant = reentrant,
                )
            }
        }
    }

    @Test
    fun everyStepStatesWhatBreaksWithoutIt() {
        show()

        listOf(
            R.string.permission_notifications_breaks,
            R.string.permission_usage_access_breaks,
            R.string.permission_overlay_breaks,
            R.string.permission_battery_breaks,
            R.string.permission_accessibility_breaks,
        ).forEach { id ->
            compose.onNodeWithText(str(id)).assertExists()
        }
    }

    @Test
    fun everyStepIsTitledAndNumbered() {
        show()

        compose.onNodeWithText(str(R.string.permission_usage_access_title)).assertExists()
        compose.onNodeWithText(context.getString(R.string.permission_setup_step_label, 1, 5)).assertExists()
        compose.onNodeWithText(context.getString(R.string.permission_setup_step_label, 5, 5)).assertExists()
    }

    @Test
    fun anUngrantedStepOffersADeepLinkButton() {
        show(granted = SpecialPermission.entries.toSet() - SpecialPermission.OVERLAY)

        compose
            .onNodeWithText(str(R.string.permission_setup_open))
            .assertExists()
            .assertHasClickAction()
    }

    @Test
    fun aGrantedStepShowsNoButton() {
        show(granted = SpecialPermission.entries.toSet())

        compose.onAllNodesWithText(str(R.string.permission_setup_open)).assertCountEquals(0)
        compose.onAllNodesWithText(str(R.string.permission_setup_granted)).assertCountEquals(5)
    }

    @Test
    fun oneDeepLinkButtonPerMissingStep() {
        show(granted = setOf(SpecialPermission.NOTIFICATIONS, SpecialPermission.USAGE_ACCESS))

        compose.onAllNodesWithText(str(R.string.permission_setup_open)).assertCountEquals(3)
    }

    @Test
    fun tappingTheButtonReportsThatExactPermission() {
        val opened = mutableListOf<SpecialPermission>()
        show(
            granted = SpecialPermission.entries.toSet() - SpecialPermission.BATTERY_EXEMPTION,
            onOpenSettings = { opened += it },
        )

        compose.onNodeWithText(str(R.string.permission_setup_open)).performScrollTo().performClick()

        assertEquals(listOf(SpecialPermission.BATTERY_EXEMPTION), opened)
    }

    @Test
    fun continueIsBlockedWhileARequiredPermissionIsMissing() {
        show(granted = setOf(SpecialPermission.NOTIFICATIONS))

        compose.onNodeWithText(str(R.string.permission_setup_continue)).performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText(str(R.string.permission_setup_blocked)).assertExists()
    }

    @Test
    fun continueIsAllowedOnceRequiredPermissionsAreGranted() {
        show(granted = setOf(SpecialPermission.USAGE_ACCESS, SpecialPermission.OVERLAY))

        compose.onNodeWithText(str(R.string.permission_setup_continue)).performScrollTo().assertIsEnabled()
    }

    @Test
    fun skippingTheOptionalStepDoesNotBlockCompletion() {
        val granted = SpecialPermission.entries.toSet() - SpecialPermission.ACCESSIBILITY
        var done = false
        show(granted = granted, onContinue = { done = true })

        compose.onNodeWithText(str(R.string.permission_setup_continue)).performScrollTo().performClick()

        assertEquals(true, done)
        compose.onNodeWithText(str(R.string.permission_setup_degraded)).assertDoesNotExist()
    }

    @Test
    fun theReentrantScreenClosesInsteadOfAdvancing() {
        show(granted = SpecialPermission.entries.toSet(), reentrant = true)

        compose.onNodeWithText(str(R.string.permission_setup_finish)).performScrollTo().assertIsEnabled()
        compose.onNodeWithText(str(R.string.permission_setup_continue)).assertDoesNotExist()
    }

    @Test
    fun theOnboardingScreenAdvancesInsteadOfClosing() {
        show(granted = SpecialPermission.entries.toSet())

        compose.onNodeWithText(str(R.string.permission_setup_continue)).performScrollTo().assertIsEnabled()
        compose.onNodeWithText(str(R.string.permission_setup_finish)).assertDoesNotExist()
    }

    @Test
    fun skippingARecommendedStepWarnsThatProtectionIsDegraded() {
        show(granted = setOf(SpecialPermission.USAGE_ACCESS, SpecialPermission.OVERLAY))

        compose.onNodeWithText(str(R.string.permission_setup_degraded)).assertExists()
    }

    @Test
    fun neitherAdvisoryShowsWhenEverythingIsGranted() {
        show(granted = SpecialPermission.entries.toSet())

        compose.onNodeWithText(str(R.string.permission_setup_degraded)).assertDoesNotExist()
        compose.onNodeWithText(str(R.string.permission_setup_blocked)).assertDoesNotExist()
        compose.onNodeWithText(str(R.string.permission_setup_continue)).performScrollTo().assertIsEnabled()
    }

    @Test
    fun theOptionalStepIsLabelledOptional() {
        show()

        compose.onNodeWithText(str(R.string.permission_setup_optional)).assertExists()
        compose.onAllNodesWithText(str(R.string.permission_setup_recommended)).assertCountEquals(2)
    }

    @Test
    fun theAccessibilityStepAsksForConsentBeforeOpeningSettings() {
        val opened = mutableListOf<SpecialPermission>()
        show(
            granted = SpecialPermission.entries.toSet() - SpecialPermission.ACCESSIBILITY,
            onOpenSettings = { opened += it },
        )

        compose.onNodeWithText(str(R.string.permission_setup_open)).performScrollTo().performClick()

        assertEquals(emptyList<SpecialPermission>(), opened)
        compose.onNodeWithText(str(R.string.accessibility_disclosure_title)).assertExists()
    }

    @Test
    fun theDisclosureNamesWhatIsReadAndWhatIsNot() {
        show(granted = SpecialPermission.entries.toSet() - SpecialPermission.ACCESSIBILITY)

        compose.onNodeWithText(str(R.string.permission_setup_open)).performScrollTo().performClick()

        compose.onNodeWithText(str(R.string.accessibility_disclosure_reads)).assertExists()
        compose.onNodeWithText(str(R.string.accessibility_disclosure_not)).assertExists()
        compose.onNodeWithText(str(R.string.accessibility_disclosure_optional)).assertExists()
    }

    @Test
    fun acceptingTheDisclosureOpensAccessibilitySettings() {
        val opened = mutableListOf<SpecialPermission>()
        show(
            granted = SpecialPermission.entries.toSet() - SpecialPermission.ACCESSIBILITY,
            onOpenSettings = { opened += it },
        )

        compose.onNodeWithText(str(R.string.permission_setup_open)).performScrollTo().performClick()
        compose.onNodeWithText(str(R.string.accessibility_disclosure_continue)).performClick()

        assertEquals(listOf(SpecialPermission.ACCESSIBILITY), opened)
        compose.onNodeWithText(str(R.string.accessibility_disclosure_title)).assertDoesNotExist()
    }

    @Test
    fun decliningTheDisclosureOpensNothing() {
        val opened = mutableListOf<SpecialPermission>()
        show(
            granted = SpecialPermission.entries.toSet() - SpecialPermission.ACCESSIBILITY,
            onOpenSettings = { opened += it },
        )

        compose.onNodeWithText(str(R.string.permission_setup_open)).performScrollTo().performClick()
        compose.onNodeWithText(str(R.string.detail_delete_cancel)).performClick()

        assertEquals(emptyList<SpecialPermission>(), opened)
        compose.onNodeWithText(str(R.string.accessibility_disclosure_title)).assertDoesNotExist()
    }

    @Test
    fun aNonAccessibilityStepOpensSettingsWithNoDisclosure() {
        show(granted = SpecialPermission.entries.toSet() - SpecialPermission.OVERLAY)

        compose.onNodeWithText(str(R.string.permission_setup_open)).performScrollTo().performClick()

        compose.onNodeWithText(str(R.string.accessibility_disclosure_title)).assertDoesNotExist()
    }
}

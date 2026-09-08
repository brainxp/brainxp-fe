package com.example.brainxp.feature.family

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FamilyHomeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun str(
        @StringRes id: Int,
    ) = context.getString(id)

    private fun show(onSignOut: () -> Unit = {}) {
        compose.setContent {
            BrainXPTheme {
                FamilyHomeScreen(
                    state = FamilyHomeUiState(),
                    onOpenChild = {},
                    onNewChild = {},
                    onSelfRules = {},
                    onJoinRules = {},
                    onBack = {},
                    onSignOut = onSignOut,
                )
            }
        }
    }

    @Test
    fun theHeaderActionIsSigningOut() {
        show()

        compose
            .onNodeWithContentDescription(str(R.string.settings_sign_out))
            .assertExists()
            .assertHasClickAction()
    }

    @Test
    fun signingOutAsksBeforeItHappens() {
        var signedOut = 0
        show(onSignOut = { signedOut++ })

        compose.onNodeWithContentDescription(str(R.string.settings_sign_out)).performClick()

        assertEquals("signed out without asking", 0, signedOut)
        compose.onNodeWithText(str(R.string.settings_sign_out_warning)).assertExists()
    }

    @Test
    fun confirmingActuallySignsOut() {
        var signedOut = 0
        show(onSignOut = { signedOut++ })

        compose.onNodeWithContentDescription(str(R.string.settings_sign_out)).performClick()
        compose.onNodeWithText(str(R.string.settings_sign_out_confirm)).performClick()

        assertEquals(1, signedOut)
    }

    @Test
    fun theParentHeaderOffersNoRouteIntoTheChildApp() {
        show()

        listOf(
            R.string.settings_title,
            R.string.tab_settings,
            R.string.home_title,
            R.string.library_title,
        ).forEach { label ->
            compose.onNodeWithText(str(label)).assertDoesNotExist()
        }
    }
}

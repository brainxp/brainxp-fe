package com.example.brainxp.feature.home

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import org.junit.Rule
import org.junit.Test

class HomeModeHeaderTest {
    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun str(
        @StringRes id: Int,
    ) = context.getString(id)

    private fun show(managed: Boolean) {
        compose.setContent {
            BrainXPTheme {
                HomeScreen(
                    state = HomeUiState(phase = HomeUiState.Phase.Ready, managed = managed),
                    onEvent = {},
                )
            }
        }
    }

    @Test
    fun aManagedPhoneNamesItselfAsTheChild() {
        show(managed = true)

        compose.onNodeWithText(str(R.string.home_mode_child)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.home_mode_self)).assertDoesNotExist()
    }

    @Test
    fun aManagedPhoneNeverAsksTheChildToPickApps() {
        show(managed = true)

        compose.onNodeWithText(str(R.string.home_apps_pick_title)).assertDoesNotExist()
        compose.onNodeWithText(str(R.string.home_apps_pick_sub)).assertDoesNotExist()
        compose.onNodeWithText(str(R.string.home_no_apps_managed)).assertIsDisplayed()
    }

    @Test
    fun anUnmanagedPhoneNamesItselfAsSoloAndOffersThePicker() {
        show(managed = false)

        compose.onNodeWithText(str(R.string.home_mode_self)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.home_mode_child)).assertDoesNotExist()
        compose.onNodeWithText(str(R.string.home_apps_pick_title)).assertIsDisplayed()
    }
}

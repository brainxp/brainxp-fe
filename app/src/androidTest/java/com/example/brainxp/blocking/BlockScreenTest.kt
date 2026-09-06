package com.example.brainxp.blocking

import androidx.annotation.StringRes
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.example.brainxp.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BlockScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun str(
        @StringRes id: Int,
    ) = context.getString(id)

    private fun labelOf(state: BlockedState) =
        str(
            when (state) {
                BlockedState.NO_BALANCE -> R.string.block_screen_action
                BlockedState.NOT_STARTED -> R.string.block_screen_idle_action
                BlockedState.DAILY_CAP -> R.string.block_screen_cap_action
                BlockedState.IDLE_HOLD -> R.string.block_screen_hold_action
                BlockedState.GUARDIAN_STALE -> R.string.block_screen_stale_action
            },
        )

    private fun tap(state: BlockedState): BlockAction? {
        var reported: BlockAction? = null
        compose.setContent {
            BlockScreen(
                appLabel = "Mobile Legends",
                info = BlockedInfo(state = state, balanceSeconds = 1_500),
                onAction = { reported = it },
            )
        }
        compose.onNodeWithText(labelOf(state)).performClick()
        return reported
    }

    @Test
    fun aStandingBalanceStartsPlayingInsteadOfLeavingTheGame() {
        assertEquals(BlockAction.START_SESSION, tap(BlockedState.NOT_STARTED))
    }

    @Test
    fun anEmptyBalanceSendsTheUserToStudy() {
        assertEquals(BlockAction.STUDY, tap(BlockedState.NO_BALANCE))
    }

    @Test
    fun theDailyCapSendsTheUserToStudy() {
        assertEquals(BlockAction.STUDY, tap(BlockedState.DAILY_CAP))
    }

    @Test
    fun aHeldBalanceSendsTheUserToStudy() {
        assertEquals(BlockAction.STUDY, tap(BlockedState.IDLE_HOLD))
    }

    @Test
    fun aStaleGuardianSendsTheUserToStudy() {
        assertEquals(BlockAction.STUDY, tap(BlockedState.GUARDIAN_STALE))
    }
}

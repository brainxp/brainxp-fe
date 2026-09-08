package com.example.brainxp.core.ui

import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private val READOUT_WIDTH = 46.dp

class StepperTest {
    @get:Rule
    val compose = createComposeRule()

    private val label = "Reward per soal"

    private fun show(
        initial: Int = 60,
        range: IntRange = 15..1800,
        onCommit: (Int) -> Unit = {},
    ) {
        compose.setContent {
            BrainXPTheme {
                Stepper(
                    value = initial.toString(),
                    onDecrease = {},
                    onIncrease = {},
                    entry = StepperEntry(label = label, value = initial, range = range, onCommit = onCommit),
                )
            }
        }
    }

    @Test
    fun theReadoutStaysOpenLongEnoughToType() {
        show()

        compose.onNodeWithText("60").performClick()

        compose.onNodeWithContentDescription(label).assertExists()
    }

    @Test
    fun aTypedNumberIsCommitted() {
        var committed: Int? = null
        show(onCommit = { committed = it })

        compose.onNodeWithText("60").performClick()
        compose.onNodeWithContentDescription(label).performTextClearance()
        compose.onNodeWithContentDescription(label).performTextInput("240")
        compose.onNodeWithContentDescription(label).performImeAction()

        assertEquals(240, committed)
    }

    @Test
    fun aNumberAboveTheRangeIsPulledBackToTheCeiling() {
        var committed: Int? = null
        show(onCommit = { committed = it })

        compose.onNodeWithText("60").performClick()
        compose.onNodeWithContentDescription(label).performTextClearance()
        compose.onNodeWithContentDescription(label).performTextInput("9999")
        compose.onNodeWithContentDescription(label).performImeAction()

        assertEquals(1_800, committed)
    }

    @Test
    fun aNumberBelowTheRangeIsLiftedToTheFloor() {
        var committed: Int? = null
        show(onCommit = { committed = it })

        compose.onNodeWithText("60").performClick()
        compose.onNodeWithContentDescription(label).performTextClearance()
        compose.onNodeWithContentDescription(label).performTextInput("1")
        compose.onNodeWithContentDescription(label).performImeAction()

        assertEquals(15, committed)
    }

    @Test
    fun clearingTheFieldKeepsTheValueItAlreadyHad() {
        var committed: Int? = null
        show(onCommit = { committed = it })

        compose.onNodeWithText("60").performClick()
        compose.onNodeWithContentDescription(label).performTextClearance()
        compose.onNodeWithContentDescription(label).performImeAction()

        assertEquals(60, committed)
    }

    @Test
    fun theValueIsCommittedOnlyOnce() {
        var commits = 0
        show(onCommit = { commits++ })

        compose.onNodeWithText("60").performClick()
        compose.onNodeWithContentDescription(label).performTextInput("0")
        compose.onNodeWithContentDescription(label).performImeAction()

        assertEquals(1, commits)
    }

    @Test
    fun theReadoutDoesNotGrowWhenTypingStarts() {
        show()

        compose.onNodeWithText("60").performClick()

        compose.onNodeWithContentDescription(label).assertWidthIsEqualTo(READOUT_WIDTH)
    }
}

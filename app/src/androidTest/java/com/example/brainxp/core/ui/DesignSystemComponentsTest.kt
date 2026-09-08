package com.example.brainxp.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class DesignSystemComponentsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun statusPillRendersItsLabel() {
        compose.setContent {
            BrainXPTheme {
                Column {
                    StatusPill("Aktif", tone = PillTone.BLUE)
                    StatusPill("Benar", tone = PillTone.OK)
                }
            }
        }

        compose.onNodeWithText("Aktif").assertIsDisplayed()
        compose.onNodeWithText("Benar").assertIsDisplayed()
    }

    @Test
    fun rowGroupRendersEveryItemWithItsValue() {
        compose.setContent {
            BrainXPTheme {
                RowGroup {
                    item(title = "Saldo waktu", value = "25 menit", emphasiseValue = true)
                    item(title = "Batas harian", subtitle = "Sampai tengah malam", value = "90 menit")
                }
            }
        }

        compose.onNodeWithText("Saldo waktu").assertIsDisplayed()
        compose.onNodeWithText("25 menit").assertIsDisplayed()
        compose.onNodeWithText("Sampai tengah malam").assertIsDisplayed()
        compose.onNodeWithText("90 menit").assertIsDisplayed()
    }

    @Test
    fun heroCardShowsLabelValueUnitAndFooter() {
        compose.setContent {
            BrainXPTheme {
                HeroCard(
                    label = "Sisa waktu",
                    value = "25",
                    unit = "menit",
                    progress = 0.5f,
                    footer = { StatusPill("Aktif", tone = PillTone.ON_DARK) },
                )
            }
        }

        compose.onNodeWithText("Sisa waktu").assertIsDisplayed()
        compose.onNodeWithText("25").assertIsDisplayed()
        compose.onNodeWithText("menit").assertIsDisplayed()
        compose.onNodeWithText("Aktif").assertIsDisplayed()
    }

    @Test
    fun segmentedControlReportsAndMarksTheSelection() {
        val picked = mutableListOf<String>()
        compose.setContent {
            BrainXPTheme {
                var selected by remember { mutableStateOf("Sendiri") }
                SegmentedControl(
                    options = listOf("Sendiri", "Keluarga"),
                    selected = selected,
                    onSelect = {
                        selected = it
                        picked += it
                    },
                    label = { it },
                )
            }
        }

        compose.onNodeWithText("Keluarga").performClick()

        assertEquals(listOf("Keluarga"), picked)
        compose.onNodeWithText("Keluarga").assertIsSelected()
    }

    @Test
    fun receiptCardRendersLinesAndTotal() {
        compose.setContent {
            BrainXPTheme {
                ReceiptCard(
                    header = "SESI 12 MEI",
                    lines =
                        listOf(
                            ReceiptLine("Benar 7 dari 8", "+21 menit"),
                            ReceiptLine("Materi baru", "x1.2"),
                        ),
                    totalLabel = "Waktu diperoleh",
                    totalValue = "25 menit",
                )
            }
        }

        compose.onNodeWithText("SESI 12 MEI").assertIsDisplayed()
        compose.onNodeWithText("Benar 7 dari 8").assertIsDisplayed()
        compose.onNodeWithText("x1.2").assertIsDisplayed()
        compose.onNodeWithText("Waktu diperoleh").assertIsDisplayed()
        compose.onNodeWithText("25 menit").assertIsDisplayed()
    }

    @Test
    fun successColoursSwapBetweenLightAndDark() {
        assertEquals(Tokens.Ok, LightExtendedColors.ok)
        assertEquals(Tokens.Ok, DarkExtendedColors.ok)
        assertNotEquals(LightExtendedColors.okSurface, DarkExtendedColors.okSurface)
        assertNotEquals(LightExtendedColors.okInk, DarkExtendedColors.okInk)
        assertEquals(LightExtendedColors.okSurface, DarkExtendedColors.okInk)
    }

    @Test
    fun themeExposesSuccessColoursForTheCurrentMode() {
        var light: ExtendedColors? = null
        var dark: ExtendedColors? = null

        compose.setContent {
            BrainXPTheme(darkTheme = false) { light = BrainXPTheme.extendedColors }
            BrainXPTheme(darkTheme = true) { dark = BrainXPTheme.extendedColors }
        }

        assertEquals(LightExtendedColors, light)
        assertEquals(DarkExtendedColors, dark)
    }
}

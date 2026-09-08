package com.example.brainxp.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ExtendedColors(
    val ok: Color,
    val okSurface: Color,
    val okInk: Color,
    val alertBorder: Color,
)

internal val LightExtendedColors =
    ExtendedColors(
        ok = Tokens.Ok,
        okSurface = Tokens.OkSurface,
        okInk = Tokens.OkInk,
        alertBorder = Tokens.AlertBorder,
    )

internal val DarkExtendedColors =
    ExtendedColors(
        ok = Tokens.Ok,
        okSurface = Tokens.OkInk,
        okInk = Tokens.OkSurface,
        alertBorder = Tokens.AlertInk,
    )

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

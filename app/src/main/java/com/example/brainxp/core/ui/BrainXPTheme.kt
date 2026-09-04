package com.example.brainxp.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun BrainXPTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) BrainXPDarkColors else BrainXPLightColors
    val extended = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalExtendedColors provides extended,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = BrainXPTypography,
            shapes = BrainXPShapes,
            content = content,
        )
    }
}

object BrainXPTheme {
    val spacing: Spacing
        @Composable get() = LocalSpacing.current

    val extendedColors: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}

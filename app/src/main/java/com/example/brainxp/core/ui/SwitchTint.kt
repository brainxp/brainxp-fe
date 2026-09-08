package com.example.brainxp.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private const val HELD = 0.55f

@Composable
fun brainxpSwitchColors(): SwitchColors {
    val scheme = MaterialTheme.colorScheme
    return SwitchDefaults.colors(
        checkedTrackColor = scheme.primary,
        checkedThumbColor = scheme.onPrimary,
        checkedBorderColor = Color.Transparent,
        uncheckedTrackColor = scheme.surfaceContainerHigh,
        uncheckedThumbColor = scheme.onSurfaceVariant,
        uncheckedBorderColor = scheme.outlineVariant,
        disabledCheckedTrackColor = scheme.primary.copy(alpha = HELD),
        disabledCheckedThumbColor = scheme.onPrimary,
        disabledCheckedBorderColor = Color.Transparent,
        disabledUncheckedTrackColor = scheme.surfaceContainerHigh,
        disabledUncheckedThumbColor = scheme.onSurfaceVariant.copy(alpha = HELD),
        disabledUncheckedBorderColor = scheme.outlineVariant,
    )
}

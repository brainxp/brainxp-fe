package com.example.brainxp.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,
) {
    val screenHorizontal: Dp get() = xl
    val screenBottom: Dp get() = xxl
    val systemBarHeight: Dp get() = 46.dp
}

val LocalSpacing = staticCompositionLocalOf { Spacing() }

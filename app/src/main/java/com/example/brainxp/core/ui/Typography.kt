package com.example.brainxp.core.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.brainxp.R

@OptIn(ExperimentalTextApi::class)
private fun variableFont(
    resId: Int,
    weight: FontWeight,
) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val PlusJakartaSans =
    FontFamily(
        variableFont(R.font.plus_jakarta_sans, FontWeight.Normal),
        variableFont(R.font.plus_jakarta_sans, FontWeight.Medium),
        variableFont(R.font.plus_jakarta_sans, FontWeight.SemiBold),
        variableFont(R.font.plus_jakarta_sans, FontWeight.Bold),
        variableFont(R.font.plus_jakarta_sans, FontWeight.ExtraBold),
    )

val JetBrainsMono =
    FontFamily(
        variableFont(R.font.jetbrains_mono, FontWeight.Normal),
        variableFont(R.font.jetbrains_mono, FontWeight.Medium),
    )

private fun sans(
    size: Int,
    weight: FontWeight,
    tracking: Float,
    lineHeight: Int = (size * LINE_HEIGHT_FACTOR).toInt(),
) = TextStyle(
    fontFamily = PlusJakartaSans,
    fontSize = size.sp,
    fontWeight = weight,
    letterSpacing = tracking.em,
    lineHeight = lineHeight.sp,
)

private const val LINE_HEIGHT_FACTOR = 1.35

val BrainXPTypography =
    Typography(
        displaySmall = sans(28, FontWeight.ExtraBold, -0.04f, lineHeight = 32),
        headlineMedium = sans(22, FontWeight.ExtraBold, -0.035f, lineHeight = 26),
        headlineSmall = sans(20, FontWeight.ExtraBold, -0.03f, lineHeight = 24),
        titleLarge = sans(19, FontWeight.ExtraBold, -0.035f, lineHeight = 26),
        titleMedium = sans(17, FontWeight.Bold, -0.025f, lineHeight = 22),
        titleSmall = sans(15, FontWeight.Bold, -0.02f, lineHeight = 20),
        bodyLarge = sans(15, FontWeight.Medium, -0.01f),
        bodyMedium = sans(14, FontWeight.Medium, -0.01f, lineHeight = 22),
        bodySmall = sans(13, FontWeight.Medium, -0.01f, lineHeight = 20),
        labelLarge = sans(16, FontWeight.Bold, -0.02f, lineHeight = 20),
        labelMedium = sans(13, FontWeight.SemiBold, -0.015f, lineHeight = 18),
        labelSmall = sans(12, FontWeight.Bold, 0f, lineHeight = 16),
    )

object BrainXPTextStyles {
    val numeric =
        TextStyle(
            fontFamily = PlusJakartaSans,
            fontSize = 46.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.045f).em,
            lineHeight = 46.sp,
            fontFeatureSettings = TABULAR_FIGURES,
        )

    val numericSmall =
        TextStyle(
            fontFamily = PlusJakartaSans,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.035f).em,
            fontFeatureSettings = TABULAR_FIGURES,
        )

    val mono =
        TextStyle(
            fontFamily = JetBrainsMono,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 23.sp,
            fontFeatureSettings = TABULAR_FIGURES,
        )

    const val TABULAR_FIGURES = "tnum"
}

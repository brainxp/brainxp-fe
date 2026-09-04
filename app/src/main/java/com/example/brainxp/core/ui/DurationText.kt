package com.example.brainxp.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.brainxp.R
import com.example.brainxp.core.time.durationParts
import java.util.Locale
import kotlin.time.Duration

@Composable
fun longDuration(seconds: Int): String {
    val parts = durationParts(seconds)
    return when {
        parts.hasHours -> stringResource(R.string.duration_hours, parts.hours, parts.minutes)
        parts.hasMinutes -> stringResource(R.string.duration_minutes, parts.minutes)
        parts.seconds > 0 -> stringResource(R.string.duration_seconds, parts.seconds)
        else -> stringResource(R.string.duration_zero)
    }
}

@Composable
fun shortDuration(seconds: Int): String {
    val parts = durationParts(seconds)
    return when {
        parts.hasHours -> stringResource(R.string.duration_short_hours, parts.hours, parts.minutes)
        parts.hasMinutes -> stringResource(R.string.duration_short_minutes, parts.minutes)
        parts.seconds > 0 -> stringResource(R.string.duration_short_seconds, parts.seconds)
        else -> stringResource(R.string.duration_short_minutes, 0)
    }
}

@Composable
fun shortDuration(duration: Duration): String = shortDuration(duration.inWholeSeconds.toInt())

@Composable
fun multiplierText(value: Double): String = stringResource(R.string.duration_multiplier, String.format(INDONESIAN, "%.2f", value))

private val INDONESIAN = Locale.forLanguageTag("id-ID")

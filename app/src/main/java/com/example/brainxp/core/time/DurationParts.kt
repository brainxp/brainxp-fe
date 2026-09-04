package com.example.brainxp.core.time

import kotlin.time.Duration

data class DurationParts(
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
) {
    val hasHours: Boolean get() = hours > 0

    val hasMinutes: Boolean get() = minutes > 0
}

fun durationParts(seconds: Int): DurationParts {
    val total = seconds.coerceAtLeast(0)
    return DurationParts(
        hours = total / SECONDS_PER_HOUR,
        minutes = (total % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE,
        seconds = total % SECONDS_PER_MINUTE,
    )
}

fun durationParts(duration: Duration): DurationParts = durationParts(duration.inWholeSeconds.toInt())

fun clock(seconds: Int): String {
    val parts = durationParts(seconds)
    val tail = "%02d:%02d".format(parts.minutes, parts.seconds)
    return if (parts.hasHours) "${parts.hours}:$tail" else tail
}

fun clock(duration: Duration): String = clock(duration.inWholeSeconds.toInt())

private const val SECONDS_PER_HOUR = 3_600
private const val SECONDS_PER_MINUTE = 60

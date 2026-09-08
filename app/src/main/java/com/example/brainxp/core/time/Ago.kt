package com.example.brainxp.core.time

import java.time.Instant
import java.time.ZoneId

enum class AgoScale {
    NOW,
    MINUTES,
    HOURS,
    YESTERDAY,
    DATE,
}

data class Ago(
    val scale: AgoScale,
    val amount: Int = 0,
)

fun agoOf(
    then: Long,
    now: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): Ago {
    val minutes = (now - then) / MILLIS_PER_MINUTE
    val thenDay = Instant.ofEpochMilli(then).atZone(zone).toLocalDate()
    val nowDay = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

    return when {
        minutes < 1 -> Ago(AgoScale.NOW)
        minutes < MINUTES_PER_HOUR -> Ago(AgoScale.MINUTES, minutes.toInt())
        thenDay == nowDay -> Ago(AgoScale.HOURS, (minutes / MINUTES_PER_HOUR).toInt())
        thenDay == nowDay.minusDays(1) -> Ago(AgoScale.YESTERDAY)
        else -> Ago(AgoScale.DATE)
    }
}

private const val MILLIS_PER_MINUTE = 60_000L
private const val MINUTES_PER_HOUR = 60L

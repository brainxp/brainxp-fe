package com.example.brainxp.core.time

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MOMENT = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.forLanguageTag("id-ID"))

fun localMoment(iso: String): String = runCatching { MOMENT.format(Instant.parse(iso).atZone(ZoneId.systemDefault())) }.getOrDefault(iso)

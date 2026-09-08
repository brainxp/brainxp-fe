package com.example.brainxp.feature.capture

import com.example.brainxp.R

fun rejectionReasonRes(code: String?): Int =
    when (code?.trim()?.lowercase()) {
        LEVEL_TOO_LOW -> R.string.reject_reason_level_too_low
        "too_thin", "too_short" -> R.string.reject_reason_too_thin
        "unreadable" -> R.string.reject_reason_unreadable
        "not_study_material" -> R.string.reject_reason_not_study
        else -> R.string.reject_reason_unknown
    }

fun isLevelRejection(code: String?): Boolean = code?.trim()?.lowercase() == LEVEL_TOO_LOW

fun rejectionNote(reason: String?): String? {
    val text = reason?.trim().orEmpty()
    return text.takeIf { it.isNotEmpty() && it.contains(' ') && !it.containsRawCode() }
}

private fun String.containsRawCode(): Boolean =
    split(' ', ',', ':', ';', '(', ')', '.').any { word ->
        val token = word.trim()
        token.length > 1 && token.contains('_') && token.none(Char::isWhitespace)
    }

private const val LEVEL_TOO_LOW = "level_too_low"

package com.example.brainxp.feature.capture

fun captureTitles(
    prefix: String,
    stamp: String,
    pages: Int,
): List<String> =
    when {
        pages <= 0 -> emptyList()
        pages == 1 -> listOf("$prefix $stamp")
        else -> (1..pages).map { page -> "$prefix $stamp ($page/$pages)" }
    }

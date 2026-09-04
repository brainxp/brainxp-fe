package com.example.brainxp.core.ui

enum class PillTone {
    NEUTRAL,
    BLUE,
    ALERT,
    OK,
    ON_DARK,
    OUTLINE,
}

enum class HeroTone {
    PRIMARY,
    DARK,
    GHOST,
}

data class ReceiptLine(
    val label: String,
    val value: String,
)

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
    val voided: Boolean = false,
    val heading: Boolean = false,
)

data class StepperEntry(
    val value: Int,
    val range: IntRange,
    val label: String,
    val onCommit: (Int) -> Unit,
)

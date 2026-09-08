package com.example.brainxp.domain.model

enum class AcademicLevel(
    val wire: String,
) {
    SD("sd"),
    SMP("smp"),
    SMA("sma"),
    KULIAH("kuliah"),
    UMUM("profesional"),
    ;

    companion object {
        fun fromWire(value: String?): AcademicLevel? = entries.firstOrNull { it.wire == value }
    }
}

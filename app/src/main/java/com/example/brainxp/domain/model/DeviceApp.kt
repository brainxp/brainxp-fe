package com.example.brainxp.domain.model

data class DeviceApp(
    val packageName: String,
    val label: String,
    val locked: Boolean = false,
    val system: Boolean = false,
    val fresh: Boolean = false,
)

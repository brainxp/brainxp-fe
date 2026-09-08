package com.example.brainxp

import androidx.navigation3.runtime.NavKey

fun notificationRoute(
    rejectedMaterial: String?,
    readyMaterial: String?,
    blockedPackage: String?,
): NavKey? =
    when {
        rejectedMaterial != null -> MainRoute.Preparing(rejectedMaterial)
        readyMaterial != null -> MainRoute.Questions(readyMaterial)
        blockedPackage != null -> MainRoute.Capture
        else -> null
    }

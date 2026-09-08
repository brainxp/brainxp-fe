package com.example.brainxp.core.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun LightSystemBars() {
    val view = LocalView.current
    if (view.isInEditMode) {
        return
    }

    DisposableEffect(view) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        val statusWasLight = controller.isAppearanceLightStatusBars
        val navigationWasLight = controller.isAppearanceLightNavigationBars

        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false

        onDispose {
            controller.isAppearanceLightStatusBars = statusWasLight
            controller.isAppearanceLightNavigationBars = navigationWasLight
        }
    }
}

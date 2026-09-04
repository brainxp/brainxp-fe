package com.example.brainxp.feature.onboarding.permission

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

@Suppress("SwallowedException")
fun launchFirstResolvable(
    context: Context,
    candidates: List<Intent>,
): Boolean {
    candidates.forEach { intent ->
        try {
            context.startActivity(intent)
            return true
        } catch (ignored: ActivityNotFoundException) {
        }
    }
    return false
}

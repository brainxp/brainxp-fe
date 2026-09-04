package com.example.brainxp.feature.onboarding.permission

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

/**
 * Tries each candidate in order and reports whether any of them opened. Package
 * visibility since API 30 makes a pre-flight resolveActivity check unreliable, so the
 * launch attempt itself is the probe.
 */
@Suppress("SwallowedException")
fun launchFirstResolvable(
    context: Context,
    candidates: List<Intent>,
): Boolean {
    candidates.forEach { intent ->
        try {
            context.startActivity(intent)
            return true
        } catch (notFound: ActivityNotFoundException) {
            // An OEM build without this screen is expected; fall through to the next candidate.
        }
    }
    return false
}

package com.example.brainxp.feature.onboarding.permission

import androidx.annotation.StringRes
import com.example.brainxp.R
import com.example.brainxp.core.permission.SpecialPermission

@StringRes
internal fun titleRes(permission: SpecialPermission): Int =
    when (permission) {
        SpecialPermission.NOTIFICATIONS -> R.string.permission_notifications_title
        SpecialPermission.USAGE_ACCESS -> R.string.permission_usage_access_title
        SpecialPermission.OVERLAY -> R.string.permission_overlay_title
        SpecialPermission.BATTERY_EXEMPTION -> R.string.permission_battery_title
        SpecialPermission.ACCESSIBILITY -> R.string.permission_accessibility_title
    }

/** FR-ON-1 requires each step to state what breaks without it. */
@StringRes
internal fun breaksRes(permission: SpecialPermission): Int =
    when (permission) {
        SpecialPermission.NOTIFICATIONS -> R.string.permission_notifications_breaks
        SpecialPermission.USAGE_ACCESS -> R.string.permission_usage_access_breaks
        SpecialPermission.OVERLAY -> R.string.permission_overlay_breaks
        SpecialPermission.BATTERY_EXEMPTION -> R.string.permission_battery_breaks
        SpecialPermission.ACCESSIBILITY -> R.string.permission_accessibility_breaks
    }

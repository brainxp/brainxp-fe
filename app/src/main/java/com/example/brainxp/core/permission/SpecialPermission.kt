package com.example.brainxp.core.permission

enum class PermissionRequirement {
    REQUIRED,
    RECOMMENDED,
    OPTIONAL,
}

/**
 * Declared in the setup order FR-ON-1 prescribes: notifications, usage access,
 * overlay, battery optimisation, then optional accessibility.
 */
enum class SpecialPermission(
    val requirement: PermissionRequirement,
) {
    NOTIFICATIONS(PermissionRequirement.RECOMMENDED),
    USAGE_ACCESS(PermissionRequirement.REQUIRED),
    OVERLAY(PermissionRequirement.REQUIRED),
    BATTERY_EXEMPTION(PermissionRequirement.RECOMMENDED),
    ACCESSIBILITY(PermissionRequirement.OPTIONAL),
}

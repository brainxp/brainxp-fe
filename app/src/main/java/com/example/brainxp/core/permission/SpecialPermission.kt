package com.example.brainxp.core.permission

enum class PermissionRequirement {
    REQUIRED,
    RECOMMENDED,
    OPTIONAL,
}

enum class SpecialPermission(
    val requirement: PermissionRequirement,
) {
    USAGE_ACCESS(PermissionRequirement.REQUIRED),
    OVERLAY(PermissionRequirement.REQUIRED),
    NOTIFICATIONS(PermissionRequirement.RECOMMENDED),
    BATTERY_EXEMPTION(PermissionRequirement.RECOMMENDED),
    ACCESSIBILITY(PermissionRequirement.OPTIONAL),
}

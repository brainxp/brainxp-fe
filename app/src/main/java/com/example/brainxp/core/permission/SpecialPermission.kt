package com.example.brainxp.core.permission

enum class PermissionRequirement {
    REQUIRED,
    RECOMMENDED,
    OPTIONAL,
}

enum class SpecialPermission(
    val requirement: PermissionRequirement,
) {
    NOTIFICATIONS(PermissionRequirement.RECOMMENDED),
    USAGE_ACCESS(PermissionRequirement.REQUIRED),
    OVERLAY(PermissionRequirement.REQUIRED),
    BATTERY_EXEMPTION(PermissionRequirement.RECOMMENDED),
    ACCESSIBILITY(PermissionRequirement.OPTIONAL),
}

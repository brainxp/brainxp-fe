package com.example.brainxp.core.permission

data class PermissionEntry(
    val permission: SpecialPermission,
    val granted: Boolean,
) {
    val requirement: PermissionRequirement get() = permission.requirement
}

data class PermissionSnapshot(
    val entries: List<PermissionEntry>,
) {
    fun isGranted(permission: SpecialPermission): Boolean = entries.firstOrNull { it.permission == permission }?.granted == true

    fun missing(requirement: PermissionRequirement): List<SpecialPermission> =
        entries.filter { it.requirement == requirement && !it.granted }.map { it.permission }

    val missingRequired: List<SpecialPermission> get() = missing(PermissionRequirement.REQUIRED)

    val protectionReady: Boolean get() = missingRequired.isEmpty()
}

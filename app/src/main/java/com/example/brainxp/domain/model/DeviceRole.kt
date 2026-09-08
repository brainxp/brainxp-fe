package com.example.brainxp.domain.model

enum class DeviceRole {
    PARENT,
    CHILD,
}

fun deviceRoleOf(serverRole: String?): DeviceRole =
    if (serverRole.equals(SERVER_ROLE_CHILD, ignoreCase = true)) DeviceRole.CHILD else DeviceRole.PARENT

fun familyParent(
    role: DeviceRole?,
    familyId: String?,
    serverRole: String?,
): Boolean =
    role == DeviceRole.PARENT &&
        (familyId != null || serverRole.equals(SERVER_ROLE_PARENT, ignoreCase = true))

private const val SERVER_ROLE_CHILD = "child"
private const val SERVER_ROLE_PARENT = "parent"

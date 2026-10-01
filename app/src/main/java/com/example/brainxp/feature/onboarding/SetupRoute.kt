package com.example.brainxp.feature.onboarding

enum class SetupRoute {
    LEVEL,
    PERMISSIONS,
    HOME,
}

fun setupRouteOf(
    family: Boolean,
    hasSubject: Boolean,
    permissionsReady: Boolean,
): SetupRoute =
    when {
        family -> SetupRoute.HOME
        !hasSubject -> SetupRoute.LEVEL
        !permissionsReady -> SetupRoute.PERMISSIONS
        else -> SetupRoute.HOME
    }

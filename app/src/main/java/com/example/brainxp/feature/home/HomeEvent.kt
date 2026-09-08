package com.example.brainxp.feature.home

sealed interface HomeEvent {
    data object Retry : HomeEvent

    data object EndUnlockEarly : HomeEvent

    data object FixPermissions : HomeEvent

    data object OpenNotifications : HomeEvent

    data object OpenProgress : HomeEvent

    data object OpenPreparing : HomeEvent

    data object OpenApps : HomeEvent

    data class OpenApp(
        val packageName: String,
    ) : HomeEvent

    data class Resume(
        val materialId: String,
    ) : HomeEvent
}

sealed interface HomeEffect {
    data class OpenQuestions(
        val materialId: String,
    ) : HomeEffect

    data object OpenPermissionSetup : HomeEffect

    data object OpenProgress : HomeEffect

    data object OpenNotifications : HomeEffect

    data object OpenPreparing : HomeEffect

    data object OpenApps : HomeEffect

    data class LaunchApp(
        val packageName: String,
    ) : HomeEffect
}

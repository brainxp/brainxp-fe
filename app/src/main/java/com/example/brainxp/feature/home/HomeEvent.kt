package com.example.brainxp.feature.home

sealed interface HomeEvent {
    data object Retry : HomeEvent

    data object StartEarning : HomeEvent

    data object EndUnlockEarly : HomeEvent

    data object StartSession : HomeEvent

    data class SelectDuration(
        val seconds: Int,
    ) : HomeEvent

    data object FixPermissions : HomeEvent

    data object ToggleProtection : HomeEvent

    data object OpenLibrary : HomeEvent

    data object OpenProgress : HomeEvent

    data class OpenApp(
        val packageName: String,
    ) : HomeEvent

    data class Resume(
        val materialId: String,
    ) : HomeEvent
}

sealed interface HomeEffect {
    data object OpenAddMaterial : HomeEffect

    data class OpenQuestions(
        val materialId: String,
    ) : HomeEffect

    data object OpenPermissionSetup : HomeEffect

    data object OpenLibrary : HomeEffect

    data object OpenProgress : HomeEffect

    data class LaunchApp(
        val packageName: String,
    ) : HomeEffect
}

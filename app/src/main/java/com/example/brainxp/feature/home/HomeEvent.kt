package com.example.brainxp.feature.home

import com.example.brainxp.core.result.ApiError

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
}

sealed interface HomeEffect {
    data class SpendFailed(
        val error: ApiError,
    ) : HomeEffect

    data object OpenAddMaterial : HomeEffect

    data object OpenPermissionSetup : HomeEffect

    data object OpenLibrary : HomeEffect

    data object OpenProgress : HomeEffect

    data class LaunchApp(
        val packageName: String,
    ) : HomeEffect
}

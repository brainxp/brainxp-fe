package com.example.brainxp.feature.home

sealed interface HomeEvent {
    data object Retry : HomeEvent

    data object StartEarning : HomeEvent

    data object EndUnlockEarly : HomeEvent

    data object FixPermissions : HomeEvent
}

sealed interface HomeEffect {
    data object OpenAddMaterial : HomeEffect

    data object OpenPermissionSetup : HomeEffect
}

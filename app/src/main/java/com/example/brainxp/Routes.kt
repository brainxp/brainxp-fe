package com.example.brainxp

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface OnboardingRoute : NavKey {
    @Serializable
    data object Welcome : OnboardingRoute

    @Serializable
    data object ModeSelect : OnboardingRoute

    @Serializable
    data object RoleSelect : OnboardingRoute

    @Serializable
    data class SignIn(
        val family: Boolean,
    ) : OnboardingRoute

    @Serializable
    data object PairDevice : OnboardingRoute

    @Serializable
    data object Level : OnboardingRoute

    @Serializable
    data object SetParentPin : OnboardingRoute

    @Serializable
    data object PermissionSetup : OnboardingRoute

    @Serializable
    data object SetupDone : OnboardingRoute
}

@Serializable
sealed interface MainRoute : NavKey {
    @Serializable
    data object Home : MainRoute

    @Serializable
    data object AppPicker : MainRoute

    @Serializable
    data object MaterialList : MainRoute

    @Serializable
    data class MaterialDetail(
        val materialId: String,
    ) : MainRoute

    @Serializable
    data object Capture : MainRoute

    @Serializable
    data object CameraCapture : MainRoute

    @Serializable
    data class Preparing(
        val materialId: String,
    ) : MainRoute

    @Serializable
    data class Rejected(
        val materialId: String,
    ) : MainRoute

    @Serializable
    data class Questions(
        val materialId: String,
    ) : MainRoute

    @Serializable
    data class Results(
        val sessionId: String,
    ) : MainRoute

    @Serializable
    data object History : MainRoute

    @Serializable
    data object Progress : MainRoute

    @Serializable
    data object ActivityLog : MainRoute

    @Serializable
    data object Settings : MainRoute

    @Serializable
    data object PermissionSetup : MainRoute

    @Serializable
    data object FamilyHome : MainRoute

    @Serializable
    data class FamilyChild(
        val childId: String,
    ) : MainRoute

    @Serializable
    data class FamilyChildPolicy(
        val childId: String,
    ) : MainRoute

    @Serializable
    data class FamilyPairing(
        val childId: String,
    ) : MainRoute

    @Serializable
    data object FamilyNewChild : MainRoute

    @Serializable
    data class FamilyBalance(
        val childId: String,
    ) : MainRoute
}

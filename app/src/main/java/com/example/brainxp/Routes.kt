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
    data object PermissionSetup : OnboardingRoute

    @Serializable
    data object OcrPrepare : OnboardingRoute
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
    data class OcrReview(
        val draftId: String,
    ) : MainRoute

    @Serializable
    data class Questions(
        val sessionId: String,
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
    data object DebugMenu : MainRoute

    @Serializable
    data object DebugUnlock : MainRoute

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
    data object FamilyPairing : MainRoute
}

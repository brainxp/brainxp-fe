package com.example.brainxp

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.example.brainxp.core.ui.PlaceholderAction
import com.example.brainxp.core.ui.PlaceholderScreen
import com.example.brainxp.feature.apps.AppPickerRoute
import com.example.brainxp.feature.onboarding.permission.PermissionSetupRoute

internal fun EntryProviderScope<NavKey>.onboardingEntries(
    backStack: NavBackStack<NavKey>,
    onSetupComplete: () -> Unit,
) {
    entry<OnboardingRoute.Welcome> {
        Placeholder("Welcome", "OnboardingRoute.Welcome") {
            step("Choose mode", backStack, OnboardingRoute.ModeSelect)
        }
    }
    entry<OnboardingRoute.ModeSelect> {
        Placeholder("Mode select", "OnboardingRoute.ModeSelect") {
            step("Set up permissions", backStack, OnboardingRoute.PermissionSetup)
        }
    }
    entry<OnboardingRoute.PermissionSetup> {
        PermissionSetupRoute(
            onDone = { backStack.add(OnboardingRoute.OcrPrepare) },
        )
    }
    entry<OnboardingRoute.OcrPrepare> {
        Placeholder("OCR prepare", "OnboardingRoute.OcrPrepare") {
            listOf(PlaceholderAction("Finish setup", onSetupComplete))
        }
    }
}

internal fun EntryProviderScope<NavKey>.dailyEntries(
    backStack: NavBackStack<NavKey>,
    onResetSetup: () -> Unit,
    onToggleProtection: () -> Unit,
) {
    entry<MainRoute.Home> {
        Placeholder("Home", "MainRoute.Home") {
            listOf(
                action("Materials", backStack, MainRoute.MaterialList),
                action("Restricted apps", backStack, MainRoute.AppPicker),
                action("History", backStack, MainRoute.History),
                action("Progress", backStack, MainRoute.Progress),
                action("Activity log", backStack, MainRoute.ActivityLog),
                action("Family", backStack, MainRoute.FamilyHome),
                action("Settings", backStack, MainRoute.Settings),
            )
        }
    }
    entry<MainRoute.AppPicker> { AppPickerRoute() }
    entry<MainRoute.History> { Placeholder("History", "MainRoute.History") }
    entry<MainRoute.Progress> { Placeholder("Progress", "MainRoute.Progress") }
    entry<MainRoute.ActivityLog> { Placeholder("Activity log", "MainRoute.ActivityLog") }
    entry<MainRoute.Settings> {
        Placeholder("Settings", "MainRoute.Settings") {
            listOf(
                action("Restricted apps", backStack, MainRoute.AppPicker),
                action("Permissions", backStack, MainRoute.PermissionSetup),
                PlaceholderAction("Toggle protection", onToggleProtection),
                PlaceholderAction("Re-run setup", onResetSetup),
            )
        }
    }
    entry<MainRoute.PermissionSetup> {
        PermissionSetupRoute(
            onDone = { backStack.popOrIgnore() },
            reentrant = true,
        )
    }
}

internal fun EntryProviderScope<NavKey>.learningEntries(backStack: NavBackStack<NavKey>) {
    entry<MainRoute.MaterialList> {
        Placeholder("Materials", "MainRoute.MaterialList") {
            listOf(
                action("Open sample material", backStack, MainRoute.MaterialDetail("material-1")),
                action("Capture new", backStack, MainRoute.Capture),
            )
        }
    }
    entry<MainRoute.MaterialDetail> { key ->
        Placeholder("Material detail", "materialId = ${key.materialId}") {
            listOf(action("Start session", backStack, MainRoute.Questions("session-1")))
        }
    }
    entry<MainRoute.Capture> {
        Placeholder("Capture", "MainRoute.Capture") {
            listOf(action("Review extracted text", backStack, MainRoute.OcrReview("draft-1")))
        }
    }
    entry<MainRoute.OcrReview> { key ->
        Placeholder("OCR review", "draftId = ${key.draftId}")
    }
    entry<MainRoute.Questions> { key ->
        Placeholder("Questions", "sessionId = ${key.sessionId}") {
            listOf(action("See results", backStack, MainRoute.Results(key.sessionId)))
        }
    }
    entry<MainRoute.Results> { key ->
        Placeholder("Results", "sessionId = ${key.sessionId}")
    }
}

internal fun EntryProviderScope<NavKey>.familyEntries(backStack: NavBackStack<NavKey>) {
    entry<MainRoute.FamilyHome> {
        Placeholder("Family", "MainRoute.FamilyHome") {
            listOf(
                action("Pair a device", backStack, MainRoute.FamilyPairing),
                action("Open child", backStack, MainRoute.FamilyChild("child-1")),
            )
        }
    }
    entry<MainRoute.FamilyChild> { key ->
        Placeholder("Child detail", "childId = ${key.childId}") {
            listOf(action("Edit policy", backStack, MainRoute.FamilyChildPolicy(key.childId)))
        }
    }
    entry<MainRoute.FamilyChildPolicy> { key ->
        Placeholder("Child policy", "childId = ${key.childId}")
    }
    entry<MainRoute.FamilyPairing> { Placeholder("Pairing", "MainRoute.FamilyPairing") }
}

@Composable
private fun Placeholder(
    name: String,
    detail: String,
    actions: () -> List<PlaceholderAction> = { emptyList() },
) {
    PlaceholderScreen(name = name, detail = detail, actions = actions())
}

private fun action(
    label: String,
    backStack: NavBackStack<NavKey>,
    target: NavKey,
) = PlaceholderAction(label) { backStack.add(target) }

private fun step(
    label: String,
    backStack: NavBackStack<NavKey>,
    target: NavKey,
) = listOf(action(label, backStack, target))

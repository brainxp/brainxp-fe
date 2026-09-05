package com.example.brainxp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.example.brainxp.feature.family.BalanceAdjustScreen
import com.example.brainxp.feature.family.ChildReportScreen
import com.example.brainxp.feature.family.FamilyHomeScreen
import com.example.brainxp.feature.family.NewChildScreen
import com.example.brainxp.feature.family.PairingCodeScreen
import com.example.brainxp.feature.family.PolicyEditorScreen
import com.example.brainxp.feature.family.PolicyEvent
import com.example.brainxp.feature.family.SAMPLE_FAMILY
import com.example.brainxp.feature.family.SAMPLE_PAIRING_CODE
import com.example.brainxp.feature.family.SAMPLE_POLICY
import com.example.brainxp.feature.family.SAMPLE_REPORT
import com.example.brainxp.feature.family.stepped

internal fun EntryProviderScope<NavKey>.familyEntries(backStack: NavBackStack<NavKey>) {
    entry<MainRoute.FamilyHome> {
        FamilyHomeScreen(
            state = SAMPLE_FAMILY,
            onBack = { backStack.popOrIgnore() },
            onOpenChild = { backStack.add(MainRoute.FamilyChild(it)) },
            onNewChild = { backStack.add(MainRoute.FamilyNewChild) },
            onSelfRules = { backStack.add(MainRoute.FamilyChildPolicy("self")) },
            onJoinRules = { backStack.add(MainRoute.FamilyChildPolicy("self")) },
        )
    }
    entry<MainRoute.FamilyNewChild> {
        NewChildScreen(
            onBack = { backStack.popOrIgnore() },
            onCreated = { _, _, _ -> backStack.add(MainRoute.FamilyChildPolicy("child-new")) },
        )
    }
    entry<MainRoute.FamilyChild> { key ->
        ChildReportScreen(
            state = SAMPLE_REPORT,
            onBack = { backStack.popOrIgnore() },
            onEditPolicy = { backStack.add(MainRoute.FamilyChildPolicy(key.childId)) },
            onIssueCode = { backStack.add(MainRoute.FamilyPairing(key.childId)) },
            onAdjustBalance = { backStack.add(MainRoute.FamilyBalance(key.childId)) },
            onRemove = { backStack.popOrIgnore() },
        )
    }
    entry<MainRoute.FamilyChildPolicy> { key ->
        var policy by remember { mutableStateOf(SAMPLE_POLICY) }

        PolicyEditorScreen(
            state = policy,
            onBack = { backStack.popOrIgnore() },
            onEvent = { event ->
                when (event) {
                    PolicyEvent.Save -> backStack.add(MainRoute.FamilyPairing(key.childId))
                    else -> policy = policy.stepped(event)
                }
            },
        )
    }
    entry<MainRoute.FamilyPairing> {
        PairingCodeScreen(
            state = SAMPLE_PAIRING_CODE,
            onBack = { backStack.popOrIgnore() },
            onDone = { backStack.popOrIgnore() },
        )
    }
    entry<MainRoute.FamilyBalance> {
        BalanceAdjustScreen(
            subjectName = SAMPLE_REPORT.subjectName,
            balanceSeconds = SAMPLE_REPORT.balanceSeconds,
            idleDaysAllowed = SAMPLE_POLICY.idleDaysAllowed,
            onBack = { backStack.popOrIgnore() },
            onApply = { _, _, _ -> backStack.popOrIgnore() },
        )
    }
}

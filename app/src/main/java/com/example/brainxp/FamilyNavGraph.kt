package com.example.brainxp

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.example.brainxp.core.ui.ErrorState
import com.example.brainxp.core.ui.LoadingState
import com.example.brainxp.core.ui.Note
import com.example.brainxp.domain.model.AcademicLevel
import com.example.brainxp.feature.family.BalanceAdjustScreen
import com.example.brainxp.feature.family.BalanceAdjustViewModel
import com.example.brainxp.feature.family.ChildReportScreen
import com.example.brainxp.feature.family.ChildReportViewModel
import com.example.brainxp.feature.family.FamilyHomeScreen
import com.example.brainxp.feature.family.FamilyHomeViewModel
import com.example.brainxp.feature.family.NewChildScreen
import com.example.brainxp.feature.family.NewChildViewModel
import com.example.brainxp.feature.family.PairingCodeScreen
import com.example.brainxp.feature.family.PairingCodeViewModel
import com.example.brainxp.feature.family.PolicyEditorScreen
import com.example.brainxp.feature.family.PolicyEditorViewModel

internal fun EntryProviderScope<NavKey>.familyEntries(backStack: NavBackStack<NavKey>) {
    entry<MainRoute.FamilyHome> { FamilyHomeEntry(backStack) }
    entry<MainRoute.FamilyNewChild> { NewChildEntry(backStack) }
    entry<MainRoute.FamilyChild> { key -> ChildReportEntry(key, backStack) }
    entry<MainRoute.FamilyChildPolicy> { key -> ChildPolicyEntry(key, backStack) }
    entry<MainRoute.FamilyPairing> { key -> PairingCodeEntry(key, backStack) }
    entry<MainRoute.FamilyBalance> { key -> BalanceEntry(key, backStack) }
}

@Composable
private fun FamilyHomeEntry(backStack: NavBackStack<NavKey>) {
    val viewModel: FamilyHomeViewModel = hiltViewModel()
    val load by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.retry()
        onPauseOrDispose {}
    }

    when {
        load.error != null -> {
            ErrorState(error = load.error!!, onRetry = viewModel::retry)
        }

        load.loading -> {
            LoadingState()
        }

        else -> {
            FamilyHomeScreen(
                state = load.home,
                onBack = { backStack.popOrIgnore() },
                onOpenChild = { backStack.add(MainRoute.FamilyChild(it)) },
                onNewChild = { backStack.add(MainRoute.FamilyNewChild) },
                onSelfRules = {
                    val own = load.home.self
                    if (own == null) {
                        viewModel.claimOwnRules(AcademicLevel.UMUM)
                    } else {
                        backStack.add(MainRoute.FamilyChildPolicy(own.id))
                    }
                },
                onJoinRules = { viewModel.claimOwnRules(AcademicLevel.UMUM) },
            )
        }
    }
}

@Composable
private fun NewChildEntry(backStack: NavBackStack<NavKey>) {
    val viewModel: NewChildViewModel = hiltViewModel()
    val load by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(load.createdId) {
        val created = load.createdId ?: return@LaunchedEffect
        viewModel.consumeCreated()
        backStack.add(MainRoute.FamilyPairing(created))
    }

    NewChildScreen(
        onBack = { backStack.popOrIgnore() },
        onCreated = { name, level, language -> viewModel.create(name, level, language.name.lowercase()) },
    )
}

@Composable
private fun ChildReportEntry(
    key: MainRoute.FamilyChild,
    backStack: NavBackStack<NavKey>,
) {
    val viewModel: ChildReportViewModel = hiltViewModel()
    val load by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(key.childId) { viewModel.load(key.childId, "") }
    LaunchedEffect(load.removed) { if (load.removed) backStack.popOrIgnore() }

    val report = load.report
    when {
        load.error != null && report == null -> {
            ErrorState(error = load.error!!, onRetry = viewModel::retry)
        }

        report == null -> {
            LoadingState()
        }

        else -> {
            ChildReportScreen(
                state = report,
                onBack = { backStack.popOrIgnore() },
                onEditPolicy = { backStack.add(MainRoute.FamilyChildPolicy(key.childId)) },
                onIssueCode = { backStack.add(MainRoute.FamilyPairing(key.childId)) },
                onAdjustBalance = { backStack.add(MainRoute.FamilyBalance(key.childId)) },
                onRemove = viewModel::remove,
            )
        }
    }
}

@Composable
private fun ChildPolicyEntry(
    key: MainRoute.FamilyChildPolicy,
    backStack: NavBackStack<NavKey>,
) {
    val viewModel: PolicyEditorViewModel = hiltViewModel()
    val load by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(key.childId) { viewModel.load(key.childId, "") }

    val policy = load.policy
    when {
        load.error != null && policy == null -> {
            ErrorState(error = load.error!!, onRetry = viewModel::retry)
        }

        policy == null -> {
            LoadingState()
        }

        else -> {
            Column {
                load.error?.let { failure -> ErrorState(error = failure) }
                load.notice?.let { text -> Note(text = text, alert = true) }
                PolicyEditorScreen(
                    state = policy,
                    onBack = { backStack.popOrIgnore() },
                    onEvent = viewModel::onEvent,
                )
            }
        }
    }
}

@Composable
private fun PairingCodeEntry(
    key: MainRoute.FamilyPairing,
    backStack: NavBackStack<NavKey>,
) {
    val viewModel: PairingCodeViewModel = hiltViewModel()
    val load by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(key.childId) { viewModel.load(key.childId) }

    val code = load.code
    when {
        load.error != null && code == null -> {
            ErrorState(error = load.error!!, onRetry = viewModel::refresh)
        }

        code == null -> {
            LoadingState()
        }

        else -> {
            PairingCodeScreen(
                state = code,
                onBack = { backStack.popOrIgnore() },
                onDone = { backStack.popOrIgnore() },
            )
        }
    }
}

@Composable
private fun BalanceEntry(
    key: MainRoute.FamilyBalance,
    backStack: NavBackStack<NavKey>,
) {
    val reports: ChildReportViewModel = hiltViewModel()
    val adjuster: BalanceAdjustViewModel = hiltViewModel()
    val load by reports.state.collectAsStateWithLifecycle()
    val adjust by adjuster.state.collectAsStateWithLifecycle()

    LaunchedEffect(key.childId) { reports.load(key.childId, "") }
    LaunchedEffect(adjust.applied) { if (adjust.applied) backStack.popOrIgnore() }

    val report = load.report
    when {
        report == null -> {
            LoadingState()
        }

        adjust.error != null -> {
            ErrorState(error = adjust.error!!, onRetry = { backStack.popOrIgnore() })
        }

        else -> {
            BalanceAdjustScreen(
                subjectName = report.subjectName,
                balanceSeconds = report.balanceSeconds,
                idleDaysAllowed = 0,
                onBack = { backStack.popOrIgnore() },
                onApply = { direction, seconds, note ->
                    adjuster.apply(key.childId, direction, seconds, note)
                },
            )
        }
    }
}

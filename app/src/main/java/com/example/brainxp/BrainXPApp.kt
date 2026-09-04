package com.example.brainxp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay

@Composable
fun BrainXPApp(
    modifier: Modifier = Modifier,
    deepLink: NavKey? = null,
) {
    val viewModel: RootViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        val content = Modifier.padding(padding)
        when (state) {
            RootUiState.Loading -> {
                LoadingRoot(modifier = content)
            }

            RootUiState.Onboarding -> {
                OnboardingNavHost(
                    modifier = content,
                    onSetupComplete = viewModel::markSetupComplete,
                )
            }

            RootUiState.Main -> {
                MainNavHost(
                    modifier = content,
                    onResetSetup = viewModel::resetSetup,
                    onToggleProtection = viewModel::toggleProtection,
                    debug =
                        DebugActions(
                            grantUnlock = viewModel::grantDebugUnlock,
                            setWarningLead = viewModel::setWarningLead,
                            endUnlock = viewModel::endUnlock,
                        ),
                    deepLink = deepLink,
                )
            }
        }
    }
}

@Composable
private fun LoadingRoot(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun OnboardingNavHost(
    onSetupComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(OnboardingRoute.Welcome)

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.popOrIgnore() },
        entryDecorators = defaultDecorators(),
        entryProvider = entryProvider { onboardingEntries(backStack, onSetupComplete) },
    )
}

@Composable
private fun MainNavHost(
    onResetSetup: () -> Unit,
    onToggleProtection: () -> Unit,
    debug: DebugActions,
    modifier: Modifier = Modifier,
    deepLink: NavKey? = null,
) {
    val backStack =
        if (deepLink == null) {
            rememberNavBackStack(MainRoute.Home)
        } else {
            rememberNavBackStack(MainRoute.Home, deepLink)
        }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.popOrIgnore() },
        entryDecorators = defaultDecorators(),
        entryProvider =
            entryProvider {
                dailyEntries(backStack, onResetSetup, onToggleProtection)
                debugEntries(backStack, onResetSetup, onToggleProtection, debug)
                learningEntries(backStack)
                familyEntries(backStack)
            },
    )
}

@Composable
private fun defaultDecorators(): List<NavEntryDecorator<NavKey>> =
    listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
    )

internal fun NavBackStack<NavKey>.popOrIgnore() {
    if (size > 1) removeAt(lastIndex)
}

package com.example.brainxp

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.example.brainxp.core.ui.RevealHost

@Composable
fun BrainXPApp(
    modifier: Modifier = Modifier,
    deepLink: NavKey? = null,
) {
    val viewModel: RootViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val familyRoot by viewModel.familyRoot.collectAsStateWithLifecycle()

    RevealHost {
        Scaffold(modifier = modifier.fillMaxSize()) { padding ->
            val content = Modifier.padding(padding)
            when (state) {
                RootUiState.Loading -> {
                    LoadingRoot(modifier = content)
                }

                RootUiState.Onboarding -> {
                    OnboardingNavHost(
                        onSetupComplete = viewModel::markSetupComplete,
                    )
                }

                RootUiState.Main -> {
                    MainNavHost(
                        modifier = content,
                        root = MainRoute.FamilyHome.takeIf { familyRoot },
                        deepLink = deepLink,
                    )
                }
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
        entryProvider =
            entryProvider {
                onboardingEntries(backStack)
                onboardingTailEntries(backStack, onSetupComplete)
            },
    )
}

@Composable
private fun MainNavHost(
    modifier: Modifier = Modifier,
    root: NavKey? = null,
    deepLink: NavKey? = null,
) {
    val backStack = rememberNavBackStack(MainRoute.Home)
    val activity = LocalContext.current.hostActivity()

    LaunchedEffect(root) {
        if (root != null && backStack.singleOrNull() == MainRoute.Home) {
            backStack.clear()
            backStack.add(root)
        }
    }

    LaunchedEffect(deepLink) {
        if (deepLink != null && backStack.lastOrNull() != deepLink) backStack.add(deepLink)
    }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else activity?.finish() },
        entryDecorators = defaultDecorators(),
        entryProvider =
            entryProvider {
                dailyEntries(backStack)
                cameraEntries(backStack)
                captureEntries(backStack)
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

private tailrec fun Context.hostActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.hostActivity()
        else -> null
    }

package com.example.brainxp

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.brainxp.core.ui.BAR_SLIDE
import com.example.brainxp.core.ui.NavMotion
import com.example.brainxp.core.ui.RevealHost
import com.example.brainxp.core.ui.backwardOf
import com.example.brainxp.core.ui.enterOf
import com.example.brainxp.core.ui.exitOf
import com.example.brainxp.core.ui.forwardOf
import com.example.brainxp.core.ui.navMotionOf

@Composable
fun BrainXPApp(
    modifier: Modifier = Modifier,
    deepLink: NavKey? = null,
    openedNotification: String? = null,
) {
    val viewModel: RootViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val familyRoot by viewModel.familyRoot.collectAsStateWithLifecycle()

    LaunchedEffect(openedNotification) {
        openedNotification?.let(viewModel::markNotificationRead)
    }

    RevealHost {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            val content = Modifier
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
        transitionSpec = { forwardOf(NavMotion.PUSH) },
        popTransitionSpec = { backwardOf(NavMotion.PUSH) },
        predictivePopTransitionSpec = { backwardOf(NavMotion.PUSH) },
        entryProvider =
            entryProvider {
                onboardingEntries(backStack, onSetupComplete)
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
    val current = backStack.lastOrNull()

    LaunchedEffect(root) {
        if (root != null && backStack.singleOrNull() == MainRoute.Home) {
            backStack.clear()
            backStack.add(root)
        }
    }

    LaunchedEffect(deepLink) {
        if (deepLink != null && backStack.lastOrNull() != deepLink) backStack.add(deepLink)
    }

    val motion = rememberNavMotion(current)
    val barVisible = current?.showsBottomBar() == true

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .then(
                        if (barVisible) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier,
                    ),
        ) {
            NavDisplay(
                backStack = backStack,
                onBack = {
                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else activity?.finish()
                },
                entryDecorators = defaultDecorators(),
                transitionSpec = { forwardOf(motion.current) },
                popTransitionSpec = { backwardOf(motion.current) },
                predictivePopTransitionSpec = { backwardOf(motion.current) },
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

        AnimatedVisibility(visible = barVisible, enter = enterOf(BAR_SLIDE), exit = exitOf(BAR_SLIDE)) {
            MainBottomBar(
                selected = backStack.lastOrNull { it.selectedTab() != null }?.selectedTab(),
                onSelect = { tab -> backStack.switchTo(tab) },
                onAddMaterial = { backStack.add(MainRoute.Capture) },
            )
        }
    }
}

private fun NavBackStack<NavKey>.switchTo(tab: BottomTab) {
    if (lastOrNull() == tab.route) {
        return
    }
    clear()
    if (tab.route != MainRoute.Home) {
        add(MainRoute.Home)
    }
    add(tab.route)
}

private class NavMotionTracker {
    private var previous: NavKey? = null
    var current: NavMotion = NavMotion.PUSH
        private set

    fun observe(top: NavKey?) {
        if (top == previous) {
            return
        }
        current =
            navMotionOf(
                fromTab = previous?.selectedTab()?.ordinal,
                toTab = top?.selectedTab()?.ordinal,
                fromFlow = previous?.risesFromBottom() == true,
                toFlow = top?.risesFromBottom() == true,
            )
        previous = top
    }
}

@Composable
private fun rememberNavMotion(top: NavKey?): NavMotionTracker {
    val tracker = remember { NavMotionTracker() }
    tracker.observe(top)
    return tracker
}

@Composable
private fun defaultDecorators(): List<NavEntryDecorator<NavKey>> =
    listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
        opaqueEntryDecorator(),
    )

@Composable
private fun opaqueEntryDecorator(): NavEntryDecorator<NavKey> {
    val backdrop = MaterialTheme.colorScheme.background
    return remember(backdrop) {
        NavEntryDecorator<NavKey> { entry ->
            Box(modifier = Modifier.fillMaxSize().background(backdrop)) { entry.Content() }
        }
    }
}

internal fun NavBackStack<NavKey>.popOrIgnore() {
    if (size > 1) removeAt(lastIndex)
}

private tailrec fun Context.hostActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.hostActivity()
        else -> null
    }

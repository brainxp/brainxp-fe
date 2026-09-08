package com.example.brainxp.feature.onboarding.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PermissionSetupRoute(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    reentrant: Boolean = false,
) {
    val viewModel: PermissionSetupViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var deviceMissingSettings by remember { mutableStateOf(false) }

    PermissionSetupScreen(
        state = state,
        onOpenSettings = { permission ->
            deviceMissingSettings = !launchFirstResolvable(context, viewModel.settingsIntents(permission))
        },
        onContinue = onDone,
        deviceMissingSettings = deviceMissingSettings,
        reentrant = reentrant,
        modifier = modifier,
    )
}

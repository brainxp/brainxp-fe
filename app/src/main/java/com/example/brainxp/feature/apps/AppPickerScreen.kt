package com.example.brainxp.feature.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.brainxp.R
import com.example.brainxp.blocking.InstalledApp
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.EmptyState
import com.example.brainxp.core.ui.LoadingState
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.StatusPill

@Composable
fun AppPickerRoute(modifier: Modifier = Modifier) {
    val viewModel: AppPickerViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppPickerScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onToggle = viewModel::onToggle,
        modifier = modifier,
    )
}

@Composable
fun AppPickerScreen(
    state: AppPickerUiState,
    onQueryChange: (String) -> Unit,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = spacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text(
            text = stringResource(R.string.app_picker_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = spacing.xxl),
        )

        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            label = { Text(stringResource(R.string.app_picker_search)) },
            modifier = Modifier.fillMaxWidth(),
        )

        when {
            state.loading -> {
                LoadingState(message = stringResource(R.string.app_picker_loading))
            }

            state.visible.isEmpty() -> {
                EmptyState(
                    title = stringResource(R.string.app_picker_empty_title),
                    body = stringResource(R.string.app_picker_empty_body),
                )
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(state.visible, key = { it.packageName }) { app ->
                        AppRow(
                            app = app,
                            checked = app.packageName in state.restricted,
                            onToggle = { onToggle(app.packageName) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (app.isGame) {
            StatusPill(stringResource(R.string.app_picker_game), tone = PillTone.BLUE)
        }
    }
}

@Preview(name = "App picker", showBackground = true)
@Composable
private fun AppPickerPreview() {
    BrainXPTheme {
        AppPickerScreen(
            state =
                AppPickerUiState(
                    loading = false,
                    apps =
                        listOf(
                            InstalledApp("com.mobile.legends", "Mobile Legends", isGame = true),
                            InstalledApp("com.supercell.clashofclans", "Clash of Clans", isGame = true),
                            InstalledApp("com.instagram.android", "Instagram"),
                        ),
                    restricted = setOf("com.mobile.legends"),
                ),
            onQueryChange = {},
            onToggle = {},
        )
    }
}

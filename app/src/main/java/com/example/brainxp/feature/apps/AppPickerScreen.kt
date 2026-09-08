package com.example.brainxp.feature.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.example.brainxp.R
import com.example.brainxp.blocking.InstalledApp
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.EmptyState
import com.example.brainxp.core.ui.LoadingState
import com.example.brainxp.core.ui.MainHeader
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.RowGroup
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.core.ui.StatusPill

@Composable
fun AppPickerRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: AppPickerViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppPickerScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onToggle = viewModel::onToggle,
        onBack = onBack,
        modifier = modifier,
    )

    if (state.blocked) {
        Note(text = stringResource(R.string.apps_managed))
    }
}

@Composable
fun AppPickerScreen(
    state: AppPickerUiState,
    onQueryChange: (String) -> Unit,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = spacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        MainHeader(title = stringResource(R.string.app_picker_title), onBack = onBack)

        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            label = { Text(stringResource(R.string.app_picker_search)) },
            leadingIcon = {
                Icon(
                    imageVector = Lucide.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            colors =
                OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                ),
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = stringResource(R.string.app_picker_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(state.visible, key = { it.packageName }) { app ->
                        AppRow(
                            app = app,
                            icon = state.icons[app.packageName],
                            locked = app.packageName in state.restricted,
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
    icon: ImageBitmap?,
    locked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RowGroup(modifier = modifier) {
        item(
            title = app.label,
            leading = { AppMark(app = app, icon = icon) },
            onClick = onToggle,
            trailing = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.xs),
                ) {
                    if (app.isGame) {
                        StatusPill(text = stringResource(R.string.app_picker_game), tone = PillTone.OUTLINE)
                    }
                    StatusPill(
                        text = stringResource(if (locked) R.string.policy_locked else R.string.policy_free),
                        tone = if (locked) PillTone.BLUE else PillTone.NEUTRAL,
                    )
                }
            },
        )
    }
}

@Composable
private fun AppMark(
    app: InstalledApp,
    icon: ImageBitmap?,
) {
    if (icon == null) {
        Text(
            text = app.label.take(1).uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(ICON))
}

private val ICON = 26.dp

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
            onBack = {},
        )
    }
}

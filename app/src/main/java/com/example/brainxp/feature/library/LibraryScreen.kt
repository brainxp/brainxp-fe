package com.example.brainxp.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Lucide
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.ChoiceRow
import com.example.brainxp.core.ui.ErrorState
import com.example.brainxp.core.ui.LoadingState
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.core.ui.StatusPill
import com.example.brainxp.core.ui.multiplierText
import com.example.brainxp.domain.model.Material
import com.example.brainxp.domain.model.MaterialStatus
import com.example.brainxp.domain.model.MaterialType

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onEvent: (LibraryEvent) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val spacing = BrainXPTheme.spacing
    var asking by remember { mutableStateOf<Material?>(null) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal)
                .padding(bottom = spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        ScreenNav(title = stringResource(R.string.library_title), onBack = onBack) {
            if (state.phase == LibraryUiState.Phase.Ready) {
                StatusPill(text = state.items.size.toString(), tone = PillTone.OUTLINE)
            }
        }

        when (val phase = state.phase) {
            LibraryUiState.Phase.Loading -> {
                LoadingState(modifier = Modifier.fillMaxWidth())
            }

            is LibraryUiState.Phase.Error -> {
                ErrorState(
                    error = phase.error,
                    onRetry = { onEvent(LibraryEvent.Retry) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            LibraryUiState.Phase.Ready -> {
                ReadyLibrary(state = state, onEvent = onEvent, onAsk = { asking = it })
            }
        }
    }

    asking?.let { material ->
        RemoveDialog(
            material = material,
            removing = state.removing == material.id,
            onConfirm = {
                onEvent(LibraryEvent.Remove(material.id))
                asking = null
            },
            onDismiss = { asking = null },
        )
    }
}

@Composable
private fun ReadyLibrary(
    state: LibraryUiState,
    onEvent: (LibraryEvent) -> Unit,
    onAsk: (Material) -> Unit,
) {
    val spacing = BrainXPTheme.spacing

    Text(
        text = stringResource(R.string.library_intro),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (state.empty) {
        Note(text = stringResource(R.string.library_empty))
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        state.items.forEach { material ->
            MaterialRow(
                material = material,
                onOpen = { onEvent(LibraryEvent.Study(material.id)) },
                onAsk = { onAsk(material) },
            )
        }
    }

    Text(
        text = stringResource(R.string.library_swipe_hint),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = spacing.xs),
    )
}

@Composable
private fun MaterialRow(
    material: Material,
    onOpen: () -> Unit,
    onAsk: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val swipe = rememberSwipeToDismissBoxState(positionalThreshold = { width -> width * SWIPE_SHARE })

    LaunchedEffect(swipe.currentValue) {
        if (swipe.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onAsk()
            swipe.reset()
        }
    }

    SwipeToDismissBox(
        state = swipe,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = { RemoveBackdrop() },
    ) {
        ChoiceRow(
            title = material.title,
            subtitle = subtitleFor(material),
            icon = Lucide.FileText,
            highlight = material.sessionCount <= 1,
            onClick = onOpen,
        )
    }
}

@Composable
private fun RemoveBackdrop(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.medium),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text = stringResource(R.string.library_remove),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.fillMaxHeight().padding(horizontal = BACKDROP_PADDING),
        )
    }
}

@Composable
private fun RemoveDialog(
    material: Material,
    removing: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.library_remove_title)) },
        text = {
            Text(
                text =
                    stringResource(
                        R.string.library_remove_body,
                        material.title,
                        material.questionCount,
                    ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !removing) {
                Text(
                    text = stringResource(R.string.detail_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.detail_delete_cancel))
            }
        },
    )
}

@Composable
private fun subtitleFor(material: Material): String =
    listOf(
        stringResource(R.string.library_question_count, material.questionCount),
        if (material.sessionCount <= 0) {
            stringResource(R.string.library_never_studied)
        } else {
            stringResource(R.string.library_times_studied, material.sessionCount)
        },
        stringResource(R.string.library_reward, multiplierText(noveltyMultiplier(material.sessionCount))),
    ).joinToString(SEPARATOR)

private const val SEPARATOR = " · "
private const val SWIPE_SHARE = 0.4f
private val BACKDROP_PADDING = 18.dp

private val PREVIEW_MATERIALS =
    listOf(
        Material(
            id = "mat-1",
            title = "Bab 4 — Gerak Lurus",
            type = MaterialType.PHOTO,
            status = MaterialStatus.READY,
            createdAt = 0L,
            sessionCount = 0,
            questionCount = 10,
        ),
        Material(
            id = "mat-2",
            title = "Hukum Newton",
            type = MaterialType.DOCUMENT,
            status = MaterialStatus.READY,
            createdAt = 0L,
            sessionCount = 4,
            questionCount = 8,
        ),
    )

@Preview(name = "Library", showBackground = true, heightDp = 900)
@Composable
private fun LibraryPreview() {
    BrainXPTheme {
        LibraryScreen(
            state =
                LibraryUiState(
                    phase = LibraryUiState.Phase.Ready,
                    items = PREVIEW_MATERIALS,
                ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Library empty", showBackground = true, heightDp = 600)
@Composable
private fun LibraryEmptyPreview() {
    BrainXPTheme {
        LibraryScreen(
            state = LibraryUiState(phase = LibraryUiState.Phase.Ready),
            onEvent = {},
            onBack = {},
        )
    }
}

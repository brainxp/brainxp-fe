package com.example.brainxp.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.brainxp.core.ui.MainHeader
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.RevealRow
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
) {
    val spacing = BrainXPTheme.spacing
    var asking by remember { mutableStateOf<Material?>(null) }
    var revealed by remember { mutableStateOf<String?>(null) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal)
                .padding(bottom = spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        MainHeader(title = stringResource(R.string.library_title)) {
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
                ReadyLibrary(
                    state = state,
                    onEvent = onEvent,
                    onAsk = { asking = it },
                    revealed = revealed,
                    onReveal = { revealed = it },
                )
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
    revealed: String?,
    onReveal: (String?) -> Unit,
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
                open = revealed == material.id,
                onReveal = { stay -> onReveal(if (stay) material.id else null) },
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
    open: Boolean,
    onReveal: (Boolean) -> Unit,
    onOpen: () -> Unit,
    onAsk: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RevealRow(
        open = open,
        onReveal = onReveal,
        actionLabel = stringResource(R.string.library_remove),
        onAction = onAsk,
        modifier = modifier,
    ) {
        ChoiceRow(
            title = material.title,
            subtitle = subtitleFor(material),
            icon = Lucide.FileText,
            highlight = material.unfinished != null || material.sessionCount <= 1,
            onClick = onOpen,
            trailing =
                material.unfinished?.let {
                    { StatusPill(text = stringResource(R.string.library_resume), tone = PillTone.BLUE) }
                },
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
private fun subtitleFor(material: Material): String {
    val open = material.unfinished
    if (open != null) {
        return stringResource(R.string.library_pending, open.answered, open.total)
    }
    return listOf(
        stringResource(R.string.library_question_count, material.questionCount),
        if (material.sessionCount <= 0) {
            stringResource(R.string.library_never_studied)
        } else {
            stringResource(R.string.library_times_studied, material.sessionCount)
        },
        stringResource(R.string.library_reward, multiplierText(noveltyMultiplier(material.sessionCount))),
    ).joinToString(SEPARATOR)
}

private const val SEPARATOR = " · "

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
        )
    }
}

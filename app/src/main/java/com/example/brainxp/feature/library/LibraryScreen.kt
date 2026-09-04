package com.example.brainxp.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Lucide
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.ChoiceRow
import com.example.brainxp.core.ui.ErrorState
import com.example.brainxp.core.ui.LoadingState
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.RowGroup
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.core.ui.StatusPill
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
                ReadyLibrary(state = state, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun ReadyLibrary(
    state: LibraryUiState,
    onEvent: (LibraryEvent) -> Unit,
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
            ChoiceRow(
                title = material.title,
                subtitle = subtitleFor(material),
                icon = Lucide.FileText,
                highlight = material.sessionCount <= 1,
                onClick = { onEvent(LibraryEvent.Study(material.id)) },
                trailing = {
                    Text(
                        text =
                            stringResource(
                                R.string.duration_multiplier,
                                formatMultiplier(noveltyMultiplier(material.sessionCount)),
                            ),
                        style = MaterialTheme.typography.titleSmall,
                        color =
                            if (material.sessionCount <= 1) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                },
            )
        }
    }

    Text(
        text = stringResource(R.string.library_manage),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = spacing.xs),
    )

    RowGroup {
        state.items.forEach { material ->
            item(
                title = material.title,
                subtitle = stringResource(R.string.library_manage_hint),
                value =
                    if (state.removing == material.id) {
                        stringResource(R.string.library_removing)
                    } else {
                        stringResource(R.string.library_remove)
                    },
                destructiveValue = state.removing != material.id,
                onClick = { onEvent(LibraryEvent.Remove(material.id)) },
            )
        }
    }
}

@Composable
private fun subtitleFor(material: Material): String =
    listOf(
        stringResource(R.string.library_question_count, "%,d".format(material.charCount)),
        stringResource(R.string.library_times_studied, material.sessionCount),
    ).joinToString(SEPARATOR)

private fun formatMultiplier(value: Double): String = "%.1f".format(value)

private const val SEPARATOR = " · "

private val PREVIEW_MATERIALS =
    listOf(
        Material(
            id = "mat-1",
            title = "Bab 4 — Gerak Lurus",
            type = MaterialType.PHOTO,
            status = MaterialStatus.READY,
            charCount = 4_812,
            createdAt = 0L,
            sessionCount = 1,
        ),
        Material(
            id = "mat-2",
            title = "Hukum Newton",
            type = MaterialType.DOCUMENT,
            status = MaterialStatus.READY,
            charCount = 2_400,
            createdAt = 0L,
            sessionCount = 4,
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

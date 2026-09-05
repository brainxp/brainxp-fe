package com.example.brainxp.feature.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.ErrorState
import com.example.brainxp.core.ui.HeroCard
import com.example.brainxp.core.ui.LoadingState
import com.example.brainxp.core.ui.MiniBarChart
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.PillShape
import com.example.brainxp.core.ui.RowGroup
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.domain.model.Badge
import com.example.brainxp.domain.model.Progress

@Composable
fun ProgressScreen(
    state: ProgressUiState,
    onRetry: () -> Unit,
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
        ScreenNav(title = stringResource(R.string.progress_title), onBack = onBack)

        when (val phase = state.phase) {
            ProgressUiState.Phase.Loading -> {
                LoadingState(modifier = Modifier.fillMaxWidth())
            }

            is ProgressUiState.Phase.Error -> {
                ErrorState(
                    error = phase.error,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            ProgressUiState.Phase.Ready -> {
                state.progress?.let { ReadyProgress(progress = it, state = state) }
            }
        }
    }
}

@Composable
private fun ReadyProgress(
    progress: Progress,
    state: ProgressUiState,
) {
    val spacing = BrainXPTheme.spacing

    HeroCard(
        label = stringResource(R.string.progress_streak_label),
        value = progress.streakCurrent.toString(),
        unit = stringResource(R.string.progress_days),
    )

    if (state.chartWorthShowing) {
        MiniBarChart(
            values = state.days.map { it.earnedSeconds / SECONDS_PER_MINUTE },
            title = stringResource(R.string.progress_chart_title),
            caption = stringResource(R.string.progress_chart_caption, state.activeDays),
        )
    } else {
        Note(text = stringResource(R.string.progress_chart_too_early, MIN_ACTIVE_DAYS_FOR_CHART))
    }

    RowGroup {
        item(
            title = stringResource(R.string.progress_freeze),
            subtitle = stringResource(R.string.progress_freeze_sub),
            value = progress.freezeTokens.toString(),
        )
        item(
            title = stringResource(R.string.progress_longest),
            value = stringResource(R.string.home_rest_days_value, progress.streakLongest),
        )
        item(
            title = stringResource(R.string.progress_sessions),
            value = progress.sessions.toString(),
        )
        item(
            title = stringResource(R.string.progress_correct),
            value = progress.correctTotal.toString(),
        )
        item(
            title = stringResource(R.string.progress_essays),
            value = progress.essayPassed.toString(),
        )
    }

    Text(
        text =
            stringResource(
                R.string.progress_badges,
                progress.badgesEarned,
                progress.badges.size,
            ),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = spacing.xs),
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
        maxItemsInEachRow = BADGES_PER_ROW,
    ) {
        progress.badges.forEach { badge ->
            BadgeTile(badge = badge, modifier = Modifier.weight(1f))
        }
    }

    Text(
        text = stringResource(R.string.progress_badge_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = spacing.sm),
    )
}

@Composable
private fun BadgeTile(
    badge: Badge,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val spacing = BrainXPTheme.spacing

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (badge.earned) scheme.primaryContainer else scheme.surface,
        border =
            BorderStroke(
                HAIRLINE,
                if (badge.earned) scheme.primary else scheme.outline,
            ),
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Surface(
                modifier = Modifier.size(GLYPH),
                shape = PillShape,
                color = if (badge.earned) scheme.primary else scheme.surfaceContainerHigh,
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = badge.name.take(1),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (badge.earned) scheme.onPrimary else scheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(
                text = badge.name,
                style = MaterialTheme.typography.titleSmall,
                color = if (badge.earned) scheme.onPrimaryContainer else scheme.onSurface,
            )
            Text(
                text = badge.hint,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

private val HAIRLINE = 1.dp
private val GLYPH = 26.dp
private const val BADGES_PER_ROW = 2

private val PREVIEW_PROGRESS =
    Progress(
        streakCurrent = 3,
        streakLongest = 9,
        sessions = 14,
        correctTotal = 96,
        essayPassed = 3,
        freezeTokens = 1,
        badges =
            listOf(
                Badge("first-step", "Langkah pertama", "Selesaikan satu sesi", earned = true),
                Badge("week-streak", "Tujuh hari", "Belajar tujuh hari beruntun", earned = false),
                Badge("essayist", "Penulis", "Lolos ambang tiga esai", earned = true),
                Badge("hard-mode", "Soal sulit", "Benar sepuluh soal sulit", earned = false),
            ),
    )

@Preview(name = "Progress", showBackground = true, heightDp = 980)
@Composable
private fun ProgressPreview() {
    BrainXPTheme {
        ProgressScreen(
            state = ProgressUiState(ProgressUiState.Phase.Ready, PREVIEW_PROGRESS),
            onRetry = {},
            onBack = {},
        )
    }
}

private const val SECONDS_PER_MINUTE = 60

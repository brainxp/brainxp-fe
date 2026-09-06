package com.example.brainxp.feature.capture

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTextStyles
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.LightSystemBars
import com.example.brainxp.core.ui.LoadingStep
import com.example.brainxp.core.ui.LoadingStepState
import com.example.brainxp.core.ui.PillShape
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.core.ui.StatusPill
import com.example.brainxp.core.ui.StepList
import com.example.brainxp.core.ui.Tokens
import com.example.brainxp.core.upload.PreparingStage

@Composable
fun PreparingScreen(
    state: PreparingUiState,
    onStart: () -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
) {
    LightSystemBars()

    BrainXPTheme(darkTheme = true) {
        PreparingContent(
            state = state,
            onStart = onStart,
            onLeave = onLeave,
            onRetry = onRetry,
            modifier = modifier,
        )
    }
}

@Composable
private fun PreparingContent(
    state: PreparingUiState,
    onStart: () -> Unit,
    onLeave: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing
    val stalled = state.error != null

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Tokens.Blue900)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = spacing.screenHorizontal)
                .padding(bottom = spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        ScreenNav(title = stringResource(R.string.preparing_title)) {
            StatusPill(
                text =
                    stringResource(
                        if (state.done) R.string.preparing_status_ready else R.string.preparing_status_working,
                    ),
                tone = PillTone.ON_DARK,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (state.materialName.isNotBlank()) {
            Text(
                text = state.materialName,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = SECONDARY_INK),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        ReadyCount(state = state)
        ReadyMeter(state = state)

        Spacer(modifier = Modifier.size(spacing.lg))
        StepList(steps = stepsFor(state.stage))
        Spacer(modifier = Modifier.weight(1f))

        Surface(shape = MaterialTheme.shapes.medium, color = Color.White.copy(alpha = NOTE_FILL)) {
            Text(
                text = stringResource(if (stalled) R.string.preparing_stalled else R.string.preparing_note),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = SECONDARY_INK),
                modifier = Modifier.padding(spacing.md),
            )
        }

        PrimaryButton(
            text =
                when {
                    stalled -> stringResource(R.string.preparing_retry)
                    state.ready -> stringResource(R.string.preparing_start_count, state.readyQuestions)
                    else -> stringResource(R.string.preparing_start)
                },
            onClick = if (stalled) onRetry else onStart,
            enabled = stalled || state.ready,
            onDark = true,
        )

        TextButton(onClick = onLeave, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.preparing_leave),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = SECONDARY_INK),
            )
        }
    }
}

@Composable
private fun ReadyCount(
    state: PreparingUiState,
    modifier: Modifier = Modifier,
) {
    val ink by animateColorAsState(
        targetValue = if (state.ready) Tokens.Mint else Color.White.copy(alpha = SECONDARY_INK),
        label = "count",
    )

    if (!state.ready) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.xs)) {
            Text(
                text = stringResource(R.string.preparing_count_none),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            Text(
                text = stringResource(R.string.preparing_count_waiting),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = SECONDARY_INK),
            )
        }
        return
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.sm),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(text = state.readyQuestions.toString(), style = BrainXPTextStyles.numeric, color = ink)
        Text(
            text = stringResource(R.string.preparing_count_label),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = SECONDARY_INK),
            modifier = Modifier.padding(bottom = BASELINE_NUDGE),
        )
    }
}

@Composable
private fun ReadyMeter(
    state: PreparingUiState,
    modifier: Modifier = Modifier,
) {
    val filled by animateFloatAsState(targetValue = state.progress, label = "meter")
    val fill by animateColorAsState(
        targetValue = if (state.done) Tokens.Mint else Tokens.Blue500,
        label = "fill",
    )

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(TRACK)
                .background(Color.White.copy(alpha = TRACK_FILL), PillShape),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(filled)
                    .height(TRACK)
                    .background(fill, PillShape),
        )
    }
}

@Composable
private fun stepsFor(stage: PreparingStage): List<LoadingStep> {
    val labels =
        listOf(
            R.string.preparing_step_reading,
            R.string.preparing_step_validating,
            R.string.preparing_step_partial,
            R.string.preparing_step_ready,
        )
    val current = ORDER.indexOf(stage).coerceAtLeast(0)

    return labels.mapIndexed { index, label ->
        LoadingStep(
            label = stringResource(label),
            state =
                when {
                    index < current || stage == PreparingStage.READY -> LoadingStepState.Done
                    index == current -> LoadingStepState.Active
                    else -> LoadingStepState.Pending
                },
        )
    }
}

private val ORDER =
    listOf(
        PreparingStage.READING,
        PreparingStage.VALIDATING,
        PreparingStage.PARTIAL,
        PreparingStage.READY,
    )

private const val SECONDARY_INK = 0.68f
private const val NOTE_FILL = 0.09f
private const val TRACK_FILL = 0.14f
private val TRACK = 6.dp
private val BASELINE_NUDGE = 6.dp

@Preview(name = "Preparing reading", showBackground = true, heightDp = 820)
@Composable
private fun PreparingPreview() {
    BrainXPTheme {
        PreparingScreen(
            state =
                PreparingUiState(
                    materialName = "LK-01 Review Aplikasi_Muhammad Wildan.pdf",
                    stage = PreparingStage.VALIDATING,
                    totalQuestions = 10,
                ),
            onStart = {},
            onLeave = {},
        )
    }
}

@Preview(name = "Preparing partial", showBackground = true, heightDp = 820)
@Composable
private fun PreparingPartialPreview() {
    BrainXPTheme {
        PreparingScreen(
            state =
                PreparingUiState(
                    materialName = "LK-01 Review Aplikasi_Muhammad Wildan.pdf",
                    stage = PreparingStage.PARTIAL,
                    readyQuestions = 3,
                    totalQuestions = 10,
                ),
            onStart = {},
            onLeave = {},
        )
    }
}

@Preview(name = "Preparing ready", showBackground = true, heightDp = 820)
@Composable
private fun PreparingReadyPreview() {
    BrainXPTheme {
        PreparingScreen(
            state =
                PreparingUiState(
                    materialName = "LK-01 Review Aplikasi_Muhammad Wildan.pdf",
                    stage = PreparingStage.READY,
                    readyQuestions = 10,
                    totalQuestions = 10,
                ),
            onStart = {},
            onLeave = {},
        )
    }
}

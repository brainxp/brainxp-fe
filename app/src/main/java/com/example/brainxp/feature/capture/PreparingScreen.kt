package com.example.brainxp.feature.capture

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.brainxp.R
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
    val refused = state.rejected
    val runner = rememberRunnerGameState()
    var playing by rememberSaveable { mutableStateOf(true) }

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
                        when {
                            refused -> R.string.preparing_status_refused
                            state.done -> R.string.preparing_status_ready
                            else -> R.string.preparing_status_working
                        },
                    ),
                tone = PillTone.ON_DARK,
            )
        }

        PreparingBody(
            state = state,
            runner = runner,
            playing = playing,
            onPlaying = { playing = it },
            modifier = Modifier.weight(1f),
        )

        if (!refused) {
            Surface(shape = MaterialTheme.shapes.medium, color = Color.White.copy(alpha = NOTE_FILL)) {
                Text(
                    text = noteFor(stalled = stalled),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = SECONDARY_INK),
                    modifier = Modifier.padding(spacing.md),
                )
            }
        }

        PrimaryButton(
            text = actionFor(state = state, refused = refused, stalled = stalled),
            onClick = if (refused || stalled) onRetry else onStart,
            enabled = refused || stalled || state.done,
            onDark = true,
        )

        if (!refused) {
            TextButton(onClick = onLeave, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.preparing_leave),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = SECONDARY_INK),
                )
            }
        }
    }
}

@Composable
private fun PreparingBody(
    state: PreparingUiState,
    runner: RunnerGameState,
    playing: Boolean,
    onPlaying: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
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

        Headline(state = state)

        Waiting(state = state, runner = runner, playing = playing, onPlaying = onPlaying)

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Waiting(
    state: PreparingUiState,
    runner: RunnerGameState,
    playing: Boolean,
    onPlaying: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing
    val waiting = modeOf(state) == PreparingMode.GAME

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        ReadyMeter(state = state)

        Spacer(modifier = Modifier.size(spacing.lg))
        StepList(steps = stepsFor(state.stage))

        if (playing && waiting) {
            Spacer(modifier = Modifier.size(spacing.lg))
            RunnerPanel(state = runner, onSkip = { onPlaying(false) }, running = true)
        } else if (waiting) {
            TextButton(onClick = { onPlaying(true) }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.runner_resume),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = SECONDARY_INK),
                )
            }
        }
    }
}

@Composable
private fun noteFor(stalled: Boolean): String = stringResource(if (stalled) R.string.preparing_stalled else R.string.preparing_note)

@Composable
private fun actionFor(
    state: PreparingUiState,
    refused: Boolean,
    stalled: Boolean,
): String =
    when {
        refused -> stringResource(R.string.preparing_try_again)
        stalled -> stringResource(R.string.preparing_retry)
        state.done -> stringResource(R.string.preparing_start_count, state.readyQuestions)
        else -> stringResource(R.string.preparing_start)
    }

@Composable
private fun Headline(
    state: PreparingUiState,
    modifier: Modifier = Modifier,
) {
    val ink by animateColorAsState(
        targetValue = if (state.done) Tokens.Mint else Color.White,
        label = "headline",
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.xs)) {
        Text(
            text =
                stringResource(
                    when {
                        state.rejected -> R.string.preparing_headline_refused
                        state.done -> R.string.preparing_headline_ready
                        else -> R.string.preparing_headline_working
                    },
                ),
            style = MaterialTheme.typography.headlineSmall,
            color = ink,
        )
        if (!state.rejected) {
            Text(
                text =
                    if (state.done) {
                        stringResource(R.string.preparing_sub_ready, state.readyQuestions)
                    } else {
                        stringResource(R.string.preparing_sub_working)
                    },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = SECONDARY_INK),
            )
        }
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

@Preview(name = "Preparing refused", showBackground = true, heightDp = 820)
@Composable
private fun PreparingRefusedPreview() {
    BrainXPTheme {
        PreparingScreen(
            state =
                PreparingUiState(
                    materialName = "Foto buram.jpg",
                    stage = PreparingStage.REJECTED,
                    rejected = true,
                    reasonCode = "not_study_material",
                    materialId = "m-9",
                ),
            onStart = {},
            onLeave = {},
        )
    }
}

@Preview(name = "Preparing game skipped", showBackground = true, heightDp = 820)
@Composable
private fun PreparingSkippedPreview() {
    BrainXPTheme {
        PreparingScreen(
            state =
                PreparingUiState(
                    materialName = "LK-01 Review Aplikasi.pdf",
                    stage = PreparingStage.VALIDATING,
                    totalQuestions = 10,
                    materialId = "m-8",
                ),
            onStart = {},
            onLeave = {},
        )
    }
}

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

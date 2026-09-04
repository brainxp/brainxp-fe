package com.example.brainxp.feature.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.LightSystemBars
import com.example.brainxp.core.ui.LoadingStep
import com.example.brainxp.core.ui.LoadingStepState
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.core.ui.StatusPill
import com.example.brainxp.core.ui.StepList
import com.example.brainxp.core.ui.Tokens

@Composable
fun PreparingScreen(
    materialName: String,
    stage: PreparingStage,
    readyQuestions: Int,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LightSystemBars()

    BrainXPTheme(darkTheme = true) {
        PreparingContent(
            materialName = materialName,
            stage = stage,
            readyQuestions = readyQuestions,
            onStart = onStart,
            modifier = modifier,
        )
    }
}

@Composable
private fun PreparingContent(
    materialName: String,
    stage: PreparingStage,
    readyQuestions: Int,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing

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
            if (readyQuestions > 0) {
                StatusPill(
                    text = stringResource(R.string.preparing_ready_count, readyQuestions),
                    tone = PillTone.ON_DARK,
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = materialName,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = SECONDARY_INK),
        )
        Text(
            text =
                stringResource(
                    if (readyQuestions > 0) {
                        R.string.preparing_first_ready
                    } else {
                        R.string.preparing_reading
                    },
                ),
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
        )

        Spacer(modifier = Modifier.size(spacing.lg))
        StepList(steps = stepsFor(stage))
        Spacer(modifier = Modifier.weight(1f))

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = Color.White.copy(alpha = NOTE_FILL),
        ) {
            Text(
                text = stringResource(R.string.preparing_note),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = SECONDARY_INK),
                modifier = Modifier.padding(spacing.md),
            )
        }

        PrimaryButton(
            text =
                stringResource(
                    if (readyQuestions > 0) R.string.preparing_start else R.string.preparing_wait,
                ),
            onClick = onStart,
            enabled = readyQuestions > 0,
            onDark = true,
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
    val current = PreparingStage.entries.indexOf(stage)

    return labels.mapIndexed { index, label ->
        LoadingStep(
            label = stringResource(label),
            state =
                when {
                    index < current -> LoadingStepState.Done
                    index == current -> LoadingStepState.Active
                    else -> LoadingStepState.Pending
                },
        )
    }
}

private const val SECONDARY_INK = 0.68f
private const val NOTE_FILL = 0.09f

@Preview(name = "Preparing reading", showBackground = true, heightDp = 820)
@Composable
private fun PreparingPreview() {
    BrainXPTheme {
        PreparingScreen(
            materialName = "Bab 4 — Gerak Lurus.pdf",
            stage = PreparingStage.READING,
            readyQuestions = 0,
            onStart = {},
        )
    }
}

@Preview(name = "Preparing partial", showBackground = true, heightDp = 820)
@Composable
private fun PreparingPartialPreview() {
    BrainXPTheme {
        PreparingScreen(
            materialName = "Bab 4 — Gerak Lurus.pdf",
            stage = PreparingStage.PARTIAL,
            readyQuestions = 3,
            onStart = {},
        )
    }
}

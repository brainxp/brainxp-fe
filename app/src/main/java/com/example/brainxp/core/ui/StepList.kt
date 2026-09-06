package com.example.brainxp.core.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class LoadingStepState {
    Done,
    Active,
    Pending,
}

data class LoadingStep(
    val label: String,
    val state: LoadingStepState,
)

@Composable
fun StepList(
    steps: List<LoadingStep>,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        steps.forEachIndexed { index, step ->
            StepRow(step = step, ordinal = index + 1)
        }
    }
}

private fun borderFor(
    state: LoadingStepState,
    scheme: ColorScheme,
): Color = if (state == LoadingStepState.Pending) scheme.outline else Color.Transparent

private val HAIRLINE = 1.dp

@Composable
private fun StepRow(
    step: LoadingStep,
    ordinal: Int,
) {
    val spacing = BrainXPTheme.spacing
    val scheme = MaterialTheme.colorScheme

    val dotBackground: Color
    val dotContent: Color
    val labelColor: Color
    when (step.state) {
        LoadingStepState.Done -> {
            dotBackground = scheme.primary.copy(alpha = DONE_FILL)
            dotContent = scheme.onPrimary
            labelColor = scheme.onSurfaceVariant
        }

        LoadingStepState.Active -> {
            dotBackground = scheme.primary
            dotContent = scheme.onPrimary
            labelColor = scheme.onSurface
        }

        LoadingStepState.Pending -> {
            dotBackground = Color.Transparent
            dotContent = scheme.onSurfaceVariant
            labelColor = scheme.onSurfaceVariant
        }
    }

    val pulse = if (step.state == LoadingStepState.Active) activePulse() else 1f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        modifier = Modifier.padding(vertical = spacing.sm),
    ) {
        Box(
            modifier =
                Modifier
                    .alpha(pulse)
                    .size(DOT_SIZE)
                    .background(dotBackground, CircleShape)
                    .border(HAIRLINE, borderFor(step.state, scheme), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (step.state == LoadingStepState.Done) DONE_MARK else ordinal.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = dotContent,
            )
        }
        Text(
            text = step.label,
            style = MaterialTheme.typography.bodyMedium,
            color = labelColor,
        )
    }
}

@Composable
private fun activePulse(): Float {
    val transition = rememberInfiniteTransition(label = "step-pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = PULSE_MIN_ALPHA,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = PULSE_DURATION_MS),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "step-pulse-alpha",
    )
    return alpha
}

private const val DONE_FILL = 0.55f
private const val DONE_MARK = "✓"
private val DOT_SIZE = 20.dp
private const val PULSE_MIN_ALPHA = 0.45f
private const val PULSE_DURATION_MS = 600

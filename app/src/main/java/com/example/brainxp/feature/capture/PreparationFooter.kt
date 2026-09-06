package com.example.brainxp.feature.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.PreviewColumn
import com.example.brainxp.core.upload.PreparingStage

@Composable
fun PreparationFooter(
    state: PreparingUiState,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (state.done) BrainXPTheme.extendedColors.okSurface else scheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(
                text =
                    when {
                        state.done -> stringResource(R.string.tour_prep_ready)
                        state.ready -> stringResource(R.string.tour_prep_working, state.readyQuestions)
                        else -> stringResource(R.string.tour_prep_reading)
                    },
                style = MaterialTheme.typography.labelMedium,
                color = if (state.done) BrainXPTheme.extendedColors.okInk else scheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (state.ready) {
                TextButton(onClick = onStart) {
                    Text(text = stringResource(R.string.preparing_start))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreparationFooterPreview() {
    BrainXPTheme {
        PreviewColumn {
            PreparationFooter(state = PreparingUiState(), onStart = {})
            PreparationFooter(
                state = PreparingUiState(stage = PreparingStage.PARTIAL, readyQuestions = 3, totalQuestions = 10),
                onStart = {},
            )
            PreparationFooter(
                state = PreparingUiState(stage = PreparingStage.READY, readyQuestions = 10, totalQuestions = 10),
                onStart = {},
            )
        }
    }
}

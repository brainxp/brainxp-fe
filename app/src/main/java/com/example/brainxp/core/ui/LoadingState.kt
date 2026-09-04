package com.example.brainxp.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    message: String? = null,
    steps: List<LoadingStep> = emptyList(),
) {
    val spacing = BrainXPTheme.spacing

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (steps.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.size(SPINNER_SIZE),
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            StepList(steps = steps, modifier = Modifier.fillMaxWidth())
        }

        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = spacing.lg),
            )
        }
    }
}

private val SPINNER_SIZE = 36.dp

@Preview(name = "LoadingState light", showBackground = true)
@Composable
private fun LoadingStateLightPreview() {
    BrainXPTheme {
        LoadingState(message = "Menyiapkan pertanyaan")
    }
}

@Preview(name = "LoadingState steps", showBackground = true)
@Composable
private fun LoadingStateStepsPreview() {
    BrainXPTheme {
        LoadingState(
            message = "Biasanya di bawah satu menit",
            steps =
                listOf(
                    LoadingStep("Mengunggah materi", LoadingStepState.Done),
                    LoadingStep("Membaca teks", LoadingStepState.Active),
                    LoadingStep("Menyusun pertanyaan", LoadingStepState.Pending),
                ),
        )
    }
}

@Preview(name = "LoadingState dark", showBackground = true)
@Composable
private fun LoadingStateDarkPreview() {
    BrainXPTheme(darkTheme = true) {
        LoadingState(
            message = "Biasanya di bawah satu menit",
            steps =
                listOf(
                    LoadingStep("Mengunggah materi", LoadingStepState.Done),
                    LoadingStep("Membaca teks", LoadingStepState.Active),
                    LoadingStep("Menyusun pertanyaan", LoadingStepState.Pending),
                ),
        )
    }
}

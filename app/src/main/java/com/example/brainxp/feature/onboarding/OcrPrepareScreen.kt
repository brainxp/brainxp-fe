package com.example.brainxp.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.brainxp.R
import com.example.brainxp.core.ocr.OcrModelState
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.LoadingStep
import com.example.brainxp.core.ui.LoadingStepState
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.core.ui.StepList

@Composable
fun OcrPrepareScreen(
    state: OcrModelState,
    onRetry: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing
    val ready = state == OcrModelState.READY
    val unavailable = state == OcrModelState.UNAVAILABLE

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = spacing.screenHorizontal)
                .padding(bottom = spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        ScreenNav(title = stringResource(R.string.ocr_prepare_title))

        Text(
            text =
                stringResource(
                    when {
                        ready -> R.string.ocr_prepare_ready_headline
                        unavailable -> R.string.ocr_prepare_failed_headline
                        else -> R.string.ocr_prepare_headline
                    },
                ),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text =
                stringResource(
                    when {
                        ready -> R.string.ocr_prepare_ready_body
                        unavailable -> R.string.ocr_prepare_failed_body
                        else -> R.string.ocr_prepare_body
                    },
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.size(spacing.sm))
        StepList(steps = stepsFor(state))
        Spacer(modifier = Modifier.weight(1f))

        if (unavailable) {
            Note(text = stringResource(R.string.ocr_prepare_non_blocking))
        }

        PrimaryButton(
            text =
                stringResource(
                    if (ready) R.string.ocr_prepare_continue else R.string.ocr_prepare_skip,
                ),
            onClick = onDone,
        )

        if (unavailable) {
            TextButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.action_retry),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun stepsFor(state: OcrModelState): List<LoadingStep> {
    val downloading =
        when (state) {
            OcrModelState.READY -> LoadingStepState.Done
            OcrModelState.PREPARING -> LoadingStepState.Active
            else -> LoadingStepState.Pending
        }
    val usable =
        when (state) {
            OcrModelState.READY -> LoadingStepState.Done
            else -> LoadingStepState.Pending
        }

    return listOf(
        LoadingStep(stringResource(R.string.ocr_step_download), downloading),
        LoadingStep(stringResource(R.string.ocr_step_ready), usable),
    )
}

@Preview(name = "OCR preparing", showBackground = true, heightDp = 780)
@Composable
private fun OcrPreparePreview() {
    BrainXPTheme {
        OcrPrepareScreen(state = OcrModelState.PREPARING, onRetry = {}, onDone = {})
    }
}

@Preview(name = "OCR ready", showBackground = true, heightDp = 780)
@Composable
private fun OcrReadyPreview() {
    BrainXPTheme { OcrPrepareScreen(state = OcrModelState.READY, onRetry = {}, onDone = {}) }
}

@Preview(name = "OCR unavailable", showBackground = true, heightDp = 780)
@Composable
private fun OcrUnavailablePreview() {
    BrainXPTheme {
        OcrPrepareScreen(state = OcrModelState.UNAVAILABLE, onRetry = {}, onDone = {})
    }
}

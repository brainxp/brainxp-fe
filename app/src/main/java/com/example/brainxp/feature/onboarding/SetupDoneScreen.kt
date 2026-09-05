package com.example.brainxp.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.LoadingStep
import com.example.brainxp.core.ui.LoadingStepState
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.StepList

@Composable
fun SetupDoneScreen(
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    protectionReady: Boolean = true,
) {
    val spacing = BrainXPTheme.spacing

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
        Spacer(modifier = Modifier.size(spacing.xxl))

        Text(
            text = stringResource(R.string.done_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.done_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.size(spacing.lg))

        StepList(
            steps =
                listOf(
                    LoadingStep(stringResource(R.string.done_step_account), LoadingStepState.Done),
                    LoadingStep(stringResource(R.string.done_step_level), LoadingStepState.Done),
                    LoadingStep(
                        stringResource(R.string.done_step_protection),
                        if (protectionReady) LoadingStepState.Done else LoadingStepState.Pending,
                    ),
                    LoadingStep(stringResource(R.string.done_step_material), LoadingStepState.Active),
                ),
        )

        Spacer(modifier = Modifier.weight(1f))

        if (!protectionReady) {
            Note(text = stringResource(R.string.done_protection_partial), alert = true)
        }

        Note(text = stringResource(R.string.done_next))

        PrimaryButton(text = stringResource(R.string.done_start), onClick = onStart)
    }
}

@Preview(heightDp = 800)
@Composable
private fun SetupDonePreview() {
    BrainXPTheme { SetupDoneScreen(onStart = {}) }
}

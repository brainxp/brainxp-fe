package com.example.brainxp.feature.capture

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
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.RowGroup
import com.example.brainxp.core.ui.ScreenNav

@Composable
fun RejectedScreen(
    assessedLevel: String,
    declaredLevel: String,
    reason: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    aboutLevel: Boolean = true,
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
        ScreenNav(title = stringResource(R.string.rejected_title), onBack = onBack)

        if (aboutLevel) {
            Note(
                text = stringResource(R.string.rejected_alert, assessedLevel, declaredLevel),
                alert = true,
            )
        }

        Text(
            text =
                stringResource(
                    if (aboutLevel) R.string.rejected_headline else R.string.rejected_headline_other,
                ),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = spacing.sm),
        )
        Text(
            text = reason,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (aboutLevel) {
            LevelRules()
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(R.string.rejected_deleted),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.size(spacing.xs))
        PrimaryButton(text = stringResource(R.string.rejected_action), onClick = onRetry)
    }
}

@Composable
private fun LevelRules(modifier: Modifier = Modifier) {
    val spacing = BrainXPTheme.spacing

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Text(
            text = stringResource(R.string.rejected_rules),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = spacing.sm),
        )

        RowGroup {
            item(
                title = stringResource(R.string.rejected_rule_at_level),
                value = stringResource(R.string.duration_multiplier, "1,00"),
                emphasiseValue = true,
            )
            item(
                title = stringResource(R.string.rejected_rule_one_below),
                value = stringResource(R.string.duration_multiplier, "0,60"),
            )
            item(
                title = stringResource(R.string.rejected_rule_two_below),
                value = stringResource(R.string.rejected_rule_refused),
                destructiveValue = true,
            )
        }
    }
}

@Preview(name = "Rejected", showBackground = true, heightDp = 860)
@Composable
private fun RejectedPreview() {
    BrainXPTheme {
        RejectedScreen(
            assessedLevel = "SD kelas 4",
            declaredLevel = "SMA kelas 11",
            reason = "Isinya ringkasan satu paragraf tanpa istilah yang bisa diuji.",
            onRetry = {},
            onBack = {},
        )
    }
}

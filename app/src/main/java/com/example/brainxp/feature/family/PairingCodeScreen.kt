package com.example.brainxp.feature.family

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Smartphone
import com.example.brainxp.R
import com.example.brainxp.core.time.clock
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.HeroCard
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.RowGroup
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.core.ui.StatusPill

@Composable
fun PairingCodeScreen(
    state: PairingCodeUiState,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
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
        ScreenNav(
            title = stringResource(R.string.code_title, state.subjectName),
            onBack = onBack,
        ) {
            StatusPill(text = stringResource(R.string.new_child_step, LAST_STEP), tone = PillTone.OUTLINE)
        }

        HeroCard(
            label = stringResource(R.string.code_hero, state.subjectName),
            value = spaced(state.code),
            footer = {
                Text(
                    text =
                        if (state.secondsLeft > 0) {
                            stringResource(R.string.code_valid, clock(state.secondsLeft))
                        } else {
                            stringResource(R.string.code_expired)
                        },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            },
        )

        Text(
            text = stringResource(R.string.code_device_section),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = spacing.sm),
        )

        val device = state.device
        if (device == null) {
            Note(text = stringResource(R.string.code_no_device))
        } else {
            RowGroup {
                item(
                    title = device.modelName,
                    subtitle = stringResource(R.string.code_device_sub, device.pairedAt),
                    leading = { RowIcon(Lucide.Smartphone) },
                    trailing = {
                        StatusPill(
                            text = stringResource(R.string.code_device_active),
                            tone = PillTone.BLUE,
                        )
                    },
                )
            }
            Note(text = stringResource(R.string.code_replace_warning), alert = true)
        }

        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(text = stringResource(R.string.code_done), onClick = onDone)
    }
}

private fun spaced(code: String): String = if (code.length < GROUP * 2) code else "${code.take(GROUP)} ${code.drop(GROUP)}"

private const val GROUP = 3
private const val LAST_STEP = 3

@Preview(name = "PairingCode fresh", showBackground = true, heightDp = 820)
@Composable
private fun PairingCodePreview() {
    BrainXPTheme {
        PairingCodeScreen(state = SAMPLE_PAIRING_CODE, onDone = {}, onBack = {})
    }
}

@Preview(name = "PairingCode bound", showBackground = true, heightDp = 820)
@Composable
private fun PairingCodeBoundPreview() {
    BrainXPTheme {
        PairingCodeScreen(
            state =
                SAMPLE_PAIRING_CODE.copy(
                    device = BoundDevice("Redmi Note 12", "3 Sep 2026"),
                ),
            onDone = {},
            onBack = {},
        )
    }
}

package com.example.brainxp.feature.family

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.Field
import com.example.brainxp.core.ui.HeroCard
import com.example.brainxp.core.ui.HeroTone
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.core.ui.SegmentedControl
import com.example.brainxp.core.ui.shortDuration

private const val MIN_NOTE_LENGTH = 3
private val AMOUNTS = listOf(900, 1_800, 3_600, 7_200)

@Composable
fun BalanceAdjustScreen(
    subjectName: String,
    balanceSeconds: Int,
    idleDaysAllowed: Int,
    onApply: (AdjustDirection, Int, String) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val spacing = BrainXPTheme.spacing
    var direction by rememberSaveable { mutableStateOf(AdjustDirection.GRANT) }
    var amount by rememberSaveable { mutableIntStateOf(AMOUNTS[1]) }
    var note by rememberSaveable { mutableStateOf("") }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = spacing.screenHorizontal)
                .padding(bottom = spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        ScreenNav(
            title = stringResource(R.string.adjust_title, subjectName),
            onBack = onBack,
        )

        HeroCard(
            label = stringResource(R.string.adjust_hero),
            value = shortDuration(balanceSeconds),
            tone = HeroTone.DARK,
            footer = {
                Text(
                    text = stringResource(R.string.adjust_hero_sub, idleDaysAllowed),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            },
        )

        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text(
                text = stringResource(R.string.adjust_kind),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SegmentedControl(
                options = AdjustDirection.entries,
                selected = direction,
                onSelect = { direction = it },
                label = { directionLabel(it) },
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text(
                text = stringResource(R.string.adjust_amount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SegmentedControl(
                options = AMOUNTS,
                selected = amount,
                onSelect = { amount = it },
                label = { shortDuration(it) },
            )
        }

        Field(
            label = stringResource(R.string.adjust_note, subjectName),
            value = note,
            onValueChange = { note = it },
            placeholder = stringResource(R.string.adjust_note_hint),
        )

        Note(text = stringResource(R.string.adjust_warning, subjectName))

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            text =
                stringResource(
                    if (direction == AdjustDirection.GRANT) {
                        R.string.adjust_apply_grant
                    } else {
                        R.string.adjust_apply_redeem
                    },
                    shortDuration(amount),
                ),
            onClick = { onApply(direction, amount, note.trim()) },
            enabled = note.trim().length >= MIN_NOTE_LENGTH,
        )
    }
}

@Composable
private fun directionLabel(direction: AdjustDirection): String =
    stringResource(
        when (direction) {
            AdjustDirection.GRANT -> R.string.adjust_grant
            AdjustDirection.REDEEM -> R.string.adjust_redeem
        },
    )

@Preview(name = "BalanceAdjust", showBackground = true, heightDp = 980)
@Composable
private fun BalanceAdjustPreview() {
    BrainXPTheme {
        BalanceAdjustScreen(
            subjectName = "Raka",
            balanceSeconds = 1_980,
            idleDaysAllowed = 2,
            onApply = { _, _, _ -> },
            onBack = {},
        )
    }
}

package com.example.brainxp.feature.family

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.MiniBarChart
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.RowGroup
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.core.ui.shortDuration

@Composable
fun ChildReportScreen(
    state: ChildReportUiState,
    onEditPolicy: () -> Unit,
    onIssueCode: () -> Unit,
    onAdjustBalance: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val spacing = BrainXPTheme.spacing
    var confirming by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal)
                .padding(bottom = spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        ScreenNav(
            title = stringResource(R.string.report_title, state.subjectName),
            onBack = onBack,
        )

        state.alert?.let { Note(text = it, alert = true) }

        MiniBarChart(
            values = state.earnedPerDay,
            title =
                stringResource(
                    if (state.mine) R.string.report_earned_self else R.string.report_earned,
                ),
            caption = stringResource(R.string.report_days, state.earnedPerDay.size),
        )

        RowGroup {
            item(
                title = stringResource(R.string.report_balance),
                value = shortDuration(state.balanceSeconds),
                emphasiseValue = true,
            )
            item(
                title = stringResource(R.string.report_materials),
                value = state.materialsStudied.toString(),
            )
            item(
                title = stringResource(R.string.report_correct),
                value = state.correctTotal.toString(),
            )
            item(
                title = stringResource(R.string.report_essays),
                value = state.essayPassed.toString(),
            )
        }

        Text(
            text = stringResource(R.string.report_recent),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = spacing.sm),
        )

        RowGroup {
            if (state.recent.isEmpty()) {
                item(title = stringResource(R.string.report_recent_empty))
            } else {
                state.recent.forEach { row ->
                    item(
                        title = row.label,
                        subtitle = row.note,
                        value = signed(row.deltaSeconds),
                        emphasiseValue = row.deltaSeconds > 0,
                    )
                }
            }
        }

        Text(
            text =
                stringResource(
                    if (state.mine) R.string.report_privacy_self else R.string.report_privacy,
                ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (confirming) {
            Note(
                text =
                    if (state.mine) {
                        stringResource(R.string.report_remove_warning_self)
                    } else {
                        stringResource(R.string.report_remove_warning, state.subjectName)
                    },
                alert = true,
            )
            PrimaryButton(
                text =
                    if (state.mine) {
                        stringResource(R.string.report_remove_confirm_self)
                    } else {
                        stringResource(R.string.report_remove_confirm, state.subjectName)
                    },
                onClick = onRemove,
            )
            TextButton(onClick = { confirming = false }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.report_cancel),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        } else {
            OutlinedButton(
                onClick = onEditPolicy,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = stringResource(R.string.report_edit_policy),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            OutlinedButton(
                onClick = onAdjustBalance,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = stringResource(R.string.report_adjust),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            if (!state.mine) {
                TextButton(onClick = onIssueCode, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.report_issue_code),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            TextButton(onClick = { confirming = true }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text =
                        if (state.mine) {
                            stringResource(R.string.report_remove_self)
                        } else {
                            stringResource(R.string.report_remove, state.subjectName)
                        },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun signed(seconds: Int): String {
    val magnitude = shortDuration(kotlin.math.abs(seconds))
    return if (seconds >= 0) "+$magnitude" else "−$magnitude"
}

@Preview(name = "ChildReport", showBackground = true, heightDp = 1400)
@Composable
private fun ChildReportPreview() {
    BrainXPTheme {
        ChildReportScreen(
            state = SAMPLE_REPORT,
            onEditPolicy = {},
            onIssueCode = {},
            onAdjustBalance = {},
            onRemove = {},
            onBack = {},
        )
    }
}

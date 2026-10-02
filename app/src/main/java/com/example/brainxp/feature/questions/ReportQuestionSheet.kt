package com.example.brainxp.feature.questions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.TriangleAlert
import com.example.brainxp.R
import com.example.brainxp.core.ui.AlertNote
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.Field
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.apiErrorBody
import com.example.brainxp.domain.model.ReportReason

@Composable
internal fun ReportButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val label = stringResource(R.string.quiz_report)

    Box(
        modifier = modifier.size(REPORT_TAP).clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Lucide.TriangleAlert,
            contentDescription = label,
            modifier = Modifier.size(REPORT_ICON),
            tint = scheme.error,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportQuestionSheet(
    state: ReportQuestionState,
    onPick: (ReportReason) -> Unit,
    onWrite: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
) {
    val form = state.form ?: return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        ReportForm(
            form = form,
            state = state,
            onPick = onPick,
            onWrite = onWrite,
            onSend = onSend,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun ReportForm(
    form: ReportForm,
    state: ReportQuestionState,
    onPick: (ReportReason) -> Unit,
    onWrite: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = BrainXPTheme.spacing

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = spacing.screenHorizontal)
                .padding(bottom = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text(
            text =
                if (form.number > 0) {
                    stringResource(R.string.quiz_report_title_numbered, form.number)
                } else {
                    stringResource(R.string.quiz_report_title)
                },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.quiz_report_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            ReportReason.entries.forEach { reason ->
                ReasonRow(
                    reason = reason,
                    selected = form.reason == reason,
                    onPick = { onPick(reason) },
                )
            }
        }

        Field(
            label = stringResource(R.string.quiz_report_note),
            value = form.note,
            onValueChange = onWrite,
            placeholder = stringResource(R.string.quiz_report_note_hint),
            multiline = true,
        )

        state.failure?.let { failure ->
            AlertNote(
                title = stringResource(R.string.quiz_report_failed),
                body = apiErrorBody(failure),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        PrimaryButton(
            text = stringResource(R.string.quiz_report_send),
            onClick = onSend,
            enabled = form.reason != null,
            loading = state.sending,
        )
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.detail_delete_cancel))
        }
    }
}

@Composable
private fun ReasonRow(
    reason: ReportReason,
    selected: Boolean,
    onPick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(selected = selected, role = Role.RadioButton, onClick = onPick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) scheme.primaryContainer else scheme.surface,
        border = BorderStroke(REASON_RING, if (selected) scheme.primary else scheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = BrainXPTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                modifier = Modifier.padding(end = BrainXPTheme.spacing.sm),
            )
            Text(
                text = stringResource(labelOf(reason)),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface,
                modifier = Modifier.padding(vertical = BrainXPTheme.spacing.md),
            )
        }
    }
}

private fun labelOf(reason: ReportReason): Int =
    when (reason) {
        ReportReason.WRONG_KEY -> R.string.quiz_report_reason_wrong_key
        ReportReason.UNCLEAR -> R.string.quiz_report_reason_unclear
        ReportReason.OFF_MATERIAL -> R.string.quiz_report_reason_off_material
        ReportReason.OTHER -> R.string.quiz_report_reason_other
    }

private val REPORT_TAP = 38.dp
private val REPORT_ICON = 22.dp
private val REASON_RING = 1.dp

@Preview
@Composable
private fun ReportFormPreview() {
    BrainXPTheme {
        Surface {
            ReportForm(
                form = ReportForm(sessionId = "s", questionId = "q", reason = ReportReason.WRONG_KEY),
                state = ReportQuestionState(),
                onPick = {},
                onWrite = {},
                onSend = {},
                onDismiss = {},
            )
        }
    }
}

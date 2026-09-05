package com.example.brainxp.feature.library

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.RowGroup
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.core.ui.StatusPill
import com.example.brainxp.core.ui.levelLabel
import com.example.brainxp.domain.model.AcademicLevel
import com.example.brainxp.domain.model.Material
import com.example.brainxp.domain.model.MaterialStatus
import com.example.brainxp.domain.model.MaterialType

@Composable
fun MaterialDetailScreen(
    material: Material,
    onStart: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    deleting: Boolean = false,
) {
    val spacing = BrainXPTheme.spacing
    var confirming by remember { mutableStateOf(false) }

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
        ScreenNav(title = material.title, onBack = onBack) {
            StatusPill(
                text = stringResource(statusRes(material.status)),
                tone =
                    when (material.status) {
                        MaterialStatus.READY -> PillTone.OK
                        MaterialStatus.FAILED -> PillTone.ALERT
                        else -> PillTone.NEUTRAL
                    },
            )
        }

        material.topicSummary?.let { summary ->
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.size(spacing.sm))

        RowGroup {
            item(
                title = stringResource(R.string.detail_questions),
                value = stringResource(R.string.detail_questions_value, material.questionCount),
            )
            item(
                title = stringResource(R.string.detail_studied),
                value = stringResource(R.string.detail_studied_value, material.sessionCount),
            )
            AcademicLevel.fromWire(material.assessedLevel)?.let { level ->
                item(
                    title = stringResource(R.string.detail_assessed),
                    value = levelLabel(level),
                )
            }
        }

        material.gateReason?.takeIf { it.contains(' ') }?.let { note -> Note(text = note) }

        Spacer(modifier = Modifier.weight(1f))

        if (confirming) {
            Note(text = stringResource(R.string.detail_delete_warning, material.questionCount), alert = true)
            PrimaryButton(
                text = stringResource(R.string.detail_delete_confirm),
                onClick = onDelete,
                loading = deleting,
            )
            TextButton(onClick = { confirming = false }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.detail_delete_cancel),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            PrimaryButton(
                text = stringResource(R.string.detail_start),
                onClick = onStart,
                enabled = material.status == MaterialStatus.READY && material.questionCount > 0,
            )
            TextButton(onClick = { confirming = true }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.detail_delete),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun statusRes(status: MaterialStatus): Int =
    when (status) {
        MaterialStatus.READY -> R.string.detail_status_ready
        MaterialStatus.FAILED -> R.string.detail_status_failed
        else -> R.string.detail_status_processing
    }

@Preview(heightDp = 800)
@Composable
private fun MaterialDetailPreview() {
    BrainXPTheme {
        MaterialDetailScreen(
            material =
                Material(
                    id = "m-1",
                    title = "newton.txt",
                    type = MaterialType.DOCUMENT,
                    status = MaterialStatus.READY,
                    createdAt = 0L,
                    sessionCount = 2,
                    questionCount = 10,
                    assessedLevel = "sma",
                    topicSummary = "Hukum Newton tentang gerak, inersia, dan aksi-reaksi.",
                ),
            onStart = {},
            onDelete = {},
            onBack = {},
        )
    }
}

package com.example.brainxp.feature.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.Field
import com.example.brainxp.core.ui.LoadingState
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.core.ui.SegmentedControl
import com.example.brainxp.core.ui.StatusPill

@Composable
fun OcrReviewScreen(
    state: OcrReviewUiState,
    onEdit: (String) -> Unit,
    onGoTo: (Int) -> Unit,
    onUpload: () -> Unit,
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
                .imePadding()
                .padding(horizontal = spacing.screenHorizontal)
                .padding(bottom = spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        ScreenNav(title = stringResource(R.string.review_title), onBack = onBack) {
            StatusPill(
                text = stringResource(R.string.review_chars, state.totalChars),
                tone = if (state.verdict == LengthVerdict.USABLE) PillTone.OK else PillTone.ALERT,
            )
        }

        if (state.reading) {
            LoadingState(modifier = Modifier.fillMaxWidth())
            return@Column
        }

        Text(
            text = stringResource(R.string.review_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.total > 1) {
            SegmentedControl(
                options = state.pages.map { it.pageIndex },
                selected = state.index,
                onSelect = onGoTo,
                label = { stringResource(R.string.review_page, it + 1) },
            )
        }

        state.current?.let { page ->
            page.failedReason?.let { reason ->
                Note(
                    text = stringResource(R.string.review_page_failed, page.pageIndex + 1, reason),
                    alert = true,
                )
            }
            Field(
                label = stringResource(R.string.review_field, page.pageIndex + 1),
                value = page.text,
                onValueChange = onEdit,
                placeholder = stringResource(R.string.review_placeholder),
                multiline = true,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text =
                    stringResource(
                        when {
                            state.saving -> R.string.review_saving
                            state.savedAtLeastOnce -> R.string.review_saved
                            else -> R.string.review_not_saved
                        },
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when (state.verdict) {
            LengthVerdict.TOO_SHORT -> {
                Note(text = stringResource(R.string.review_too_short, MIN_USABLE_CHARS), alert = true)
            }

            LengthVerdict.TOO_LONG -> {
                Note(text = stringResource(R.string.review_too_long, MAX_USABLE_CHARS), alert = true)
            }

            LengthVerdict.USABLE -> {
                Unit
            }
        }

        PrimaryButton(
            text = stringResource(R.string.review_upload),
            onClick = onUpload,
            enabled = state.canUpload,
        )

        if (state.failedPages.isNotEmpty()) {
            OutlinedButton(
                onClick = { onGoTo(state.failedPages.first().pageIndex) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text =
                        stringResource(R.string.review_jump_failed, state.failedPages.size),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

private val PREVIEW_STATE =
    OcrReviewUiState(
        draftId = "draft-1",
        pages =
            listOf(
                ReviewPage(0, "Gerak lurus beraturan adalah gerak dengan kecepatan tetap. ".repeat(6)),
                ReviewPage(1, "", "gambarnya terlalu gelap"),
            ),
        savedAtLeastOnce = true,
    )

@Preview(name = "OCR review", showBackground = true, heightDp = 900)
@Composable
private fun OcrReviewPreview() {
    BrainXPTheme {
        OcrReviewScreen(
            state = PREVIEW_STATE,
            onEdit = {},
            onGoTo = {},
            onUpload = {},
            onBack = {},
        )
    }
}

@Preview(name = "OCR review too short", showBackground = true, heightDp = 900)
@Composable
private fun OcrReviewShortPreview() {
    BrainXPTheme {
        OcrReviewScreen(
            state = OcrReviewUiState(draftId = "d", pages = listOf(ReviewPage(0, "terlalu pendek"))),
            onEdit = {},
            onGoTo = {},
            onUpload = {},
            onBack = {},
        )
    }
}

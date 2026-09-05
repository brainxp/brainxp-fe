package com.example.brainxp.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.EmptyState
import com.example.brainxp.core.ui.RowGroup
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.core.ui.shortDuration
import com.example.brainxp.domain.model.LedgerEntry

@Composable
fun HistoryScreen(
    entries: List<LedgerEntry>,
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
        ScreenNav(title = stringResource(R.string.history_title), onBack = onBack)

        if (entries.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.history_empty_title),
                body = stringResource(R.string.history_empty_body),
            )
            return@Column
        }

        Text(
            text = stringResource(R.string.history_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            items(entries.groupBy { it.occurredAt.take(DAY_LENGTH) }.entries.toList()) { (day, ofDay) ->
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    RowGroup {
                        ofDay.forEach { entry ->
                            item(
                                title = stringResource(entryLabel(entry.entryType)),
                                subtitle = entry.note,
                                value = signedDuration(entry.deltaSeconds),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun signedDuration(seconds: Int): String = if (seconds < 0) "−${shortDuration(-seconds)}" else "+${shortDuration(seconds)}"

private fun entryLabel(entryType: String): Int =
    when (entryType) {
        "earned", "earn" -> R.string.history_type_earn
        "spent", "spend" -> R.string.history_type_spend
        "granted", "grant" -> R.string.history_type_grant
        "redeemed", "redeem" -> R.string.history_type_redeem
        "expired", "expire" -> R.string.history_type_expire
        else -> R.string.history_type_other
    }

private const val DAY_LENGTH = 10
private const val PREVIEW_EARNED = 144
private const val PREVIEW_SPENT = 600
private const val PREVIEW_SMALL = 43

@Preview(heightDp = 700)
@Composable
private fun HistoryPreview() {
    BrainXPTheme {
        HistoryScreen(
            entries =
                listOf(
                    LedgerEntry("earned", PREVIEW_EARNED, "2026-09-05T09:26:57Z", "Hukum Newton"),
                    LedgerEntry("spent", -PREVIEW_SPENT, "2026-09-05T11:02:10Z", null),
                    LedgerEntry("earned", PREVIEW_SMALL, "2026-09-04T08:00:00Z", "Klasifikasi makhluk hidup"),
                ),
            onBack = {},
        )
    }
}

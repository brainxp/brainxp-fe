package com.example.brainxp.feature.activity

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
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.RowGroup
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.domain.model.ActivityEvent
import com.example.brainxp.domain.model.ActivityKind
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ActivityLogScreen(
    events: List<ActivityEvent>,
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
        ScreenNav(title = stringResource(R.string.activity_title), onBack = onBack)

        if (events.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.activity_empty_title),
                body = stringResource(R.string.activity_empty_body),
            )
            return@Column
        }

        Note(text = stringResource(R.string.activity_scope))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            items(events) { event ->
                RowGroup {
                    item(
                        title = stringResource(kindLabel(event.kind)),
                        subtitle = event.payload.values.firstOrNull(),
                        value = clockOf(event.timestamp),
                    )
                }
            }
        }
    }
}

private fun clockOf(timestamp: Long): String =
    runCatching {
        FORMATTER.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))
    }.getOrDefault("")

private fun kindLabel(kind: ActivityKind): Int =
    when (kind) {
        ActivityKind.MATERIAL_ADDED -> R.string.activity_material_added
        ActivityKind.SESSION_COMPLETED -> R.string.activity_session_completed
        ActivityKind.REWARD_EARNED -> R.string.activity_reward_earned
        ActivityKind.UNLOCK_STARTED -> R.string.activity_unlock_started
        ActivityKind.UNLOCK_ENDED -> R.string.activity_unlock_ended
        ActivityKind.PROTECTION_DISABLED -> R.string.activity_protection_disabled
        ActivityKind.PROTECTION_DEGRADED -> R.string.activity_protection_degraded
        ActivityKind.PROTECTION_ANOMALY -> R.string.activity_protection_anomaly
    }

private val FORMATTER = DateTimeFormatter.ofPattern("dd/MM HH:mm")
private const val PREVIEW_NOW = 1_788_600_000_000L
private const val PREVIEW_STEP = 1_000_000L

@Preview(heightDp = 700)
@Composable
private fun ActivityLogPreview() {
    BrainXPTheme {
        ActivityLogScreen(
            events =
                listOf(
                    ActivityEvent(ActivityKind.REWARD_EARNED, PREVIEW_NOW, mapOf("note" to "Hukum Newton")),
                    ActivityEvent(ActivityKind.UNLOCK_STARTED, PREVIEW_NOW - PREVIEW_STEP),
                    ActivityEvent(ActivityKind.PROTECTION_DEGRADED, PREVIEW_NOW - 2 * PREVIEW_STEP),
                ),
            onBack = {},
        )
    }
}

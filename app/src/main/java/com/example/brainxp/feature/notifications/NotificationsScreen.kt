package com.example.brainxp.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.TriangleAlert
import com.example.brainxp.R
import com.example.brainxp.core.time.AgoScale
import com.example.brainxp.core.time.agoOf
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.EmptyState
import com.example.brainxp.core.ui.LoadingState
import com.example.brainxp.core.ui.MainHeader
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.RowGroup
import com.example.brainxp.core.ui.StatusPill
import com.example.brainxp.domain.model.AppNotification
import com.example.brainxp.domain.model.NotificationKind
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NotificationsScreen(
    state: NotificationsUiState,
    onEvent: (NotificationsEvent) -> Unit,
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
        MainHeader(title = stringResource(R.string.notifications_title), onBack = onBack) {
            if (state.unreadCount > 0) {
                StatusPill(
                    text = stringResource(R.string.notification_new_count, state.unreadCount),
                    tone = PillTone.BLUE,
                )
            }
        }

        when {
            state.loading -> {
                LoadingState()
            }

            state.notifications.isEmpty() -> {
                EmptyState(
                    title = stringResource(R.string.notifications_empty_title),
                    body = stringResource(R.string.notifications_empty_body),
                )
            }

            else -> {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    RowGroup {
                        state.notifications.forEach { record ->
                            item(
                                title = titleOf(record),
                                subtitle = detailOf(record),
                                value = agoLabel(record.createdAt),
                                leading = { Outcome(record) },
                                onClick = { onEvent(NotificationsEvent.Open(record.id)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Outcome(
    record: AppNotification,
    modifier: Modifier = Modifier,
) {
    val ready = record.kind == NotificationKind.QUESTIONS_READY
    val ink = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val settled = ready && !record.unread
    val tint = if (settled) MaterialTheme.colorScheme.onSurfaceVariant else ink

    Box(
        modifier = modifier.size(BADGE),
        contentAlignment = Alignment.Center,
    ) {
        if (record.unread) {
            Box(
                modifier =
                    Modifier
                        .size(BADGE)
                        .background(ink.copy(alpha = HALO), CircleShape),
            )
        }
        Icon(
            imageVector = if (ready) Lucide.CircleCheck else Lucide.TriangleAlert,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(GLYPH),
        )
    }
}

@Composable
private fun titleOf(record: AppNotification): String =
    when (record.kind) {
        NotificationKind.QUESTIONS_READY -> {
            stringResource(R.string.notification_ready_row, record.materialTitle)
        }

        NotificationKind.MATERIAL_REJECTED -> {
            stringResource(R.string.notification_rejected_row, record.materialTitle)
        }
    }

@Composable
private fun detailOf(record: AppNotification): String =
    when (record.kind) {
        NotificationKind.QUESTIONS_READY -> {
            stringResource(R.string.notification_ready_detail, record.questionCount)
        }

        NotificationKind.MATERIAL_REJECTED -> {
            stringResource(R.string.notification_rejected_detail)
        }
    }

@Composable
private fun agoLabel(timestamp: Long): String {
    val ago = agoOf(then = timestamp, now = System.currentTimeMillis())
    return when (ago.scale) {
        AgoScale.NOW -> stringResource(R.string.ago_now)
        AgoScale.MINUTES -> stringResource(R.string.ago_minutes, ago.amount)
        AgoScale.HOURS -> stringResource(R.string.ago_hours, ago.amount)
        AgoScale.YESTERDAY -> stringResource(R.string.ago_yesterday)
        AgoScale.DATE -> dayOf(timestamp)
    }
}

private fun dayOf(timestamp: Long): String =
    runCatching {
        DAY.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))
    }.getOrDefault("")

private val DAY = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("id-ID"))
private val BADGE = 28.dp
private val GLYPH = 18.dp
private const val HALO = 0.12f

private const val PREVIEW_NOW = 1_788_600_000_000L
private const val PREVIEW_STEP = 3_600_000L
private const val PREVIEW_COUNT = 10

@Preview(heightDp = 700)
@Composable
private fun NotificationsPreview() {
    BrainXPTheme {
        NotificationsScreen(
            state =
                NotificationsUiState(
                    loading = false,
                    notifications =
                        listOf(
                            AppNotification(
                                id = "n1",
                                kind = NotificationKind.QUESTIONS_READY,
                                materialId = "m1",
                                materialTitle = "Hukum Newton",
                                questionCount = PREVIEW_COUNT,
                                createdAt = PREVIEW_NOW,
                            ),
                            AppNotification(
                                id = "n2",
                                kind = NotificationKind.MATERIAL_REJECTED,
                                materialId = "m2",
                                materialTitle = "Foto buram",
                                questionCount = 0,
                                createdAt = PREVIEW_NOW - PREVIEW_STEP,
                                readAt = PREVIEW_NOW,
                            ),
                        ),
                ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(heightDp = 500)
@Composable
private fun NotificationsEmptyPreview() {
    BrainXPTheme {
        NotificationsScreen(
            state = NotificationsUiState(loading = false),
            onEvent = {},
            onBack = {},
        )
    }
}

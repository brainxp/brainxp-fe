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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Lucide
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.EmptyState
import com.example.brainxp.core.ui.LoadingState
import com.example.brainxp.core.ui.MainHeader
import com.example.brainxp.core.ui.RowGroup
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.domain.model.AppNotification
import com.example.brainxp.domain.model.NotificationKind
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
        MainHeader(title = stringResource(R.string.notifications_title), onBack = onBack)

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
                LazyColumn(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    items(state.notifications, key = { it.id }) { record ->
                        RowGroup {
                            item(
                                title = titleOf(record),
                                subtitle = subtitleOf(record),
                                value = clockOf(record.createdAt),
                                emphasiseValue = record.unread,
                                leading = { ReadMark(unread = record.unread) },
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
private fun ReadMark(
    unread: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!unread) {
        Icon(
            imageVector = Lucide.Bell,
            contentDescription = stringResource(R.string.notifications_read),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.size(GLYPH),
        )
        return
    }

    Box(
        modifier =
            modifier
                .size(DOT)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
    )
}

@Composable
private fun titleOf(record: AppNotification): String =
    stringResource(
        when (record.kind) {
            NotificationKind.QUESTIONS_READY -> R.string.ready_notification_title
            NotificationKind.MATERIAL_REJECTED -> R.string.ready_notification_rejected_title
        },
    )

@Composable
private fun subtitleOf(record: AppNotification): String =
    when (record.kind) {
        NotificationKind.QUESTIONS_READY -> {
            stringResource(R.string.ready_notification_body, record.materialTitle, record.questionCount)
        }

        NotificationKind.MATERIAL_REJECTED -> {
            stringResource(R.string.ready_notification_rejected_body, record.materialTitle)
        }
    }

private fun clockOf(timestamp: Long): String =
    runCatching {
        FORMATTER.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))
    }.getOrDefault("")

private val FORMATTER = DateTimeFormatter.ofPattern("dd/MM HH:mm")
private val DOT = 10.dp
private val GLYPH = 17.dp

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

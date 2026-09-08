package com.example.brainxp.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.ChartNoAxesColumn
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Flame
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Sparkles
import com.example.brainxp.R
import com.example.brainxp.blocking.ProtectionStatus
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.time.fullClock
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.ChoiceRow
import com.example.brainxp.core.ui.ErrorState
import com.example.brainxp.core.ui.HeroCard
import com.example.brainxp.core.ui.HeroTone
import com.example.brainxp.core.ui.LoadingState
import com.example.brainxp.core.ui.MainHeader
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.RowGroup
import com.example.brainxp.core.ui.SegmentedControl
import com.example.brainxp.core.ui.StatusPill
import com.example.brainxp.core.ui.shortDuration
import com.example.brainxp.domain.model.UnlockState
import com.example.brainxp.feature.health.DegradedBanner
import java.time.LocalTime
import kotlin.time.Duration.Companion.minutes

@Composable
fun HomeScreen(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        TopBar(state = state, onEvent = onEvent)

        if (state.displayName.isNotBlank()) {
            Text(
                text = stringResource(greetingRes(LocalTime.now().hour), state.displayName),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.degraded) {
            DegradedBanner(onFixPermissions = { onEvent(HomeEvent.FixPermissions) })
        }

        when (val phase = state.phase) {
            HomeUiState.Phase.Loading -> {
                LoadingState(modifier = Modifier.fillMaxWidth())
            }

            is HomeUiState.Phase.Error -> {
                ErrorState(
                    error = phase.error,
                    onRetry = { onEvent(HomeEvent.Retry) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            HomeUiState.Phase.Ready -> {
                ReadyContent(state = state, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun TopBar(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing

    MainHeader(title = stringResource(R.string.home_title), modifier = modifier) {
        StatusPill(
            text =
                stringResource(
                    if (state.managed) R.string.home_mode_child else R.string.home_mode_self,
                ),
            tone = PillTone.OUTLINE,
        )
        if (state.streakDays > 0) {
            StatusPill(
                text = stringResource(R.string.home_streak, state.streakDays),
                tone = PillTone.BLUE,
                leading = {
                    Icon(
                        imageVector = Lucide.Flame,
                        contentDescription = null,
                        modifier = Modifier.size(PILL_ICON),
                    )
                },
            )
        }
        IconButton(
            onClick = { onEvent(HomeEvent.OpenProgress) },
            modifier = Modifier.size(NAV_TAP),
        ) {
            Icon(
                imageVector = Lucide.ChartNoAxesColumn,
                contentDescription = stringResource(R.string.home_progress_open),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        NotificationBell(
            unread = state.unreadNotifications,
            onClick = { onEvent(HomeEvent.OpenNotifications) },
        )
    }
}

@Composable
private fun NotificationBell(
    unread: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BadgedBox(
        modifier = modifier,
        badge = {
            if (unread > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ) {
                    Text(text = badgeCountOf(unread))
                }
            }
        },
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(NAV_TAP)) {
            Icon(
                imageVector = Lucide.Bell,
                contentDescription =
                    if (unread > 0) {
                        stringResource(R.string.notifications_unread, unread)
                    } else {
                        stringResource(R.string.notifications_open)
                    },
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun badgeCountOf(unread: Int): String = if (unread > BADGE_CEILING) "$BADGE_CEILING+" else unread.toString()

@Composable
private fun ReadyContent(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.xxl),
    ) {
        BalanceSection(state = state, onEvent = onEvent)
        TodaySection(state = state)
        EarnSection(state = state, onEvent = onEvent)
        LockedSection(state = state, onEvent = onEvent)
    }
}

@Composable
private fun Section(
    modifier: Modifier = Modifier,
    label: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.sm),
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}

@Composable
private fun BalanceSection(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Section(modifier = modifier) {
        Hero(state)

        if (state.balanceStale) {
            Caption(text = stringResource(R.string.home_stale_balance))
        }

        if (state.capReached) {
            Caption(
                text = stringResource(R.string.home_cap_reached, shortDuration(state.secondsUntilReset)),
            )
        }

        if (state.unlockRunning) {
            RunningSession(state = state, onEvent = onEvent)
        } else {
            Note(text = stringResource(R.string.home_session_explain))
        }
    }
}

@Composable
private fun TodaySection(
    state: HomeUiState,
    modifier: Modifier = Modifier,
) {
    Section(modifier = modifier, label = stringResource(R.string.home_section_today)) {
        RowGroup {
            item(
                title = stringResource(R.string.home_daily_cap),
                value = shortDuration(state.dailyCapSeconds),
            )
            item(
                title = stringResource(R.string.home_rest_days),
                subtitle = stringResource(R.string.home_rest_days_sub),
                value = stringResource(R.string.home_rest_days_value, state.idleDaysAllowed),
            )
        }
    }
}

@Composable
private fun EarnSection(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.preparing == null && state.pending == null) {
        return
    }

    Section(modifier = modifier, label = stringResource(R.string.home_section_earn)) {
        state.preparing?.let { preparing ->
            ChoiceRow(
                title = stringResource(R.string.home_preparing_title),
                subtitle =
                    if (preparing.total > 0) {
                        stringResource(R.string.home_preparing_sub)
                    } else {
                        stringResource(R.string.home_preparing_sub_waiting)
                    },
                icon = Lucide.Sparkles,
                onClick = { onEvent(HomeEvent.OpenPreparing) },
            )
        }

        state.pending?.let { pending ->
            ChoiceRow(
                title = stringResource(R.string.home_resume_title),
                subtitle =
                    stringResource(
                        R.string.home_resume_sub,
                        pending.title,
                        pending.answered,
                        pending.total,
                    ),
                icon = Lucide.FileText,
                highlight = true,
                onClick = { onEvent(HomeEvent.Resume(pending.materialId)) },
            )
        }
    }
}

@Composable
private fun LockedSection(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Section(
        modifier = modifier,
        label =
            stringResource(
                if (state.appsOpen) R.string.home_apps_open else R.string.home_apps_locked,
            ).takeIf { state.lockedApps.isNotEmpty() },
    ) {
        LockedApps(state = state, onEvent = onEvent)
    }
}

@Composable
private fun Caption(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun RunningSession(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Text(
            text = stringResource(R.string.home_session_spent, shortDuration(state.consumedSeconds)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Note(text = stringResource(R.string.home_session_note))
        OutlinedButton(
            onClick = { onEvent(HomeEvent.EndUnlockEarly) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                text = stringResource(R.string.home_end_early),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun Hero(state: HomeUiState) {
    val running = state.unlockRunning
    HeroCard(
        label =
            stringResource(
                if (running) R.string.home_countdown_label else R.string.home_balance_label,
            ),
        value = if (running) fullClock(state.remaining) else fullClock(state.balanceSeconds),
        progress = if (state.dailyCapSeconds > 0) state.spentFraction else null,
        tone = if (state.capReached || state.idleLocked) HeroTone.DARK else HeroTone.PRIMARY,
        footer = {
            Text(
                text =
                    when {
                        state.idleLocked -> {
                            stringResource(R.string.home_idle_locked, state.idleDays)
                        }

                        running -> {
                            stringResource(R.string.home_balance_left, shortDuration(state.balanceSeconds))
                        }

                        state.dailyCapSeconds > 0 -> {
                            stringResource(
                                R.string.home_spent_of_cap,
                                shortDuration(state.spentTodaySeconds),
                                shortDuration(state.dailyCapSeconds),
                            )
                        }

                        else -> {
                            stringResource(R.string.home_no_cap)
                        }
                    },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        },
    )
}

@Composable
private fun AppIcon(app: LockedApp) {
    val icon = app.icon
    if (icon == null) {
        Icon(imageVector = Lucide.Lock, contentDescription = null)
        return
    }
    Image(
        bitmap = icon,
        contentDescription = null,
        modifier = Modifier.size(APP_ICON),
    )
}

private fun greetingRes(hour: Int): Int =
    when (hour) {
        in MORNING -> R.string.home_greeting_morning
        in MIDDAY -> R.string.home_greeting_midday
        in AFTERNOON -> R.string.home_greeting_afternoon
        else -> R.string.home_greeting_evening
    }

private val MORNING = 5..10
private val MIDDAY = 11..14
private val AFTERNOON = 15..18

@Composable
private fun LockedApps(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
) {
    if (state.lockedApps.isEmpty()) {
        ChoiceRow(
            title =
                stringResource(
                    if (state.managed) R.string.home_no_apps_title else R.string.home_apps_pick_title,
                ),
            subtitle =
                stringResource(
                    if (state.managed) R.string.home_no_apps_managed else R.string.home_apps_pick_sub,
                ),
            icon = Lucide.Lock,
            onClick =
                if (state.managed) {
                    null
                } else {
                    { onEvent(HomeEvent.OpenApps) }
                },
        )
        return
    }

    RowGroup {
        state.lockedApps.forEach { app ->
            item(
                title = app.label,
                value =
                    stringResource(
                        if (state.appsOpen) R.string.home_app_open else R.string.home_app_locked,
                    ),
                emphasiseValue = state.appsOpen,
                leading = { AppIcon(app) },
                onClick = { onEvent(HomeEvent.OpenApp(app.packageName)) },
            )
        }
        if (!state.managed) {
            item(
                title = stringResource(R.string.home_apps_manage),
                leading = { Icon(imageVector = Lucide.Lock, contentDescription = null) },
                onClick = { onEvent(HomeEvent.OpenApps) },
            )
        }
    }

    if (state.managed) {
        Text(
            text = stringResource(R.string.home_apps_managed_note),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val PILL_ICON = 13.dp
private val NAV_TAP = 48.dp
private const val BADGE_CEILING = 9

private const val PREVIEW_BUDGET_MILLIS = 900_000L
private const val PREVIEW_CONSUMED_MILLIS = 60_000L

private val PREVIEW_APPS =
    listOf(
        LockedApp("com.mobile.legends", "Mobile Legends"),
        LockedApp("com.instagram.android", "Instagram"),
    )

@Preview(name = "Home locked", showBackground = true, heightDp = 940)
@Composable
private fun HomeLockedPreview() {
    BrainXPTheme {
        HomeScreen(
            state =
                HomeUiState(
                    phase = HomeUiState.Phase.Ready,
                    balanceSeconds = 1_500,
                    spentTodaySeconds = 900,
                    dailyCapSeconds = 5_400,
                    secondsUntilReset = 18_000,
                    streakDays = 3,
                    protection = ProtectionStatus.ACTIVE,
                    lockedApps = PREVIEW_APPS,
                ),
            onEvent = {},
        )
    }
}

@Preview(name = "Home running", showBackground = true, heightDp = 940)
@Composable
private fun HomeRunningPreview() {
    BrainXPTheme {
        HomeScreen(
            state =
                HomeUiState(
                    phase = HomeUiState.Phase.Ready,
                    balanceSeconds = 600,
                    dailyCapSeconds = 5_400,
                    streakDays = 5,
                    protection = ProtectionStatus.ACTIVE,
                    lockedApps = PREVIEW_APPS,
                    unlock =
                        UnlockState.Active(
                            unlockId = "unlock-1",
                            budgetMillis = PREVIEW_BUDGET_MILLIS,
                            consumedByPackage = mapOf("com.mobile.legends" to PREVIEW_CONSUMED_MILLIS),
                            allowedPackages = setOf("com.mobile.legends"),
                        ),
                    remaining = 14.minutes,
                ),
            onEvent = {},
        )
    }
}

@Preview(name = "Home off and stale", showBackground = true, heightDp = 940)
@Composable
private fun HomeOffPreview() {
    BrainXPTheme {
        HomeScreen(
            state =
                HomeUiState(
                    phase = HomeUiState.Phase.Ready,
                    balanceSeconds = 40,
                    balanceStale = true,
                    protection = ProtectionStatus.OFF,
                ),
            onEvent = {},
        )
    }
}

@Preview(name = "Home degraded", showBackground = true, heightDp = 940)
@Composable
private fun HomeDegradedPreview() {
    BrainXPTheme {
        HomeScreen(
            state =
                HomeUiState(
                    phase = HomeUiState.Phase.Ready,
                    balanceSeconds = 1_500,
                    dailyCapSeconds = 5_400,
                    protection = ProtectionStatus.DEGRADED,
                    lockedApps = PREVIEW_APPS,
                ),
            onEvent = {},
        )
    }
}

@Preview(name = "Home managed with nothing locked", showBackground = true, heightDp = 940)
@Composable
private fun HomeManagedPreview() {
    BrainXPTheme {
        HomeScreen(
            state =
                HomeUiState(
                    phase = HomeUiState.Phase.Ready,
                    balanceSeconds = 900,
                    dailyCapSeconds = 5_400,
                    protection = ProtectionStatus.ACTIVE,
                    managed = true,
                ),
            onEvent = {},
        )
    }
}

@Preview(name = "Home error", showBackground = true)
@Composable
private fun HomeErrorPreview() {
    BrainXPTheme {
        HomeScreen(
            state = HomeUiState(phase = HomeUiState.Phase.Error(ApiError.Network, retryable = true)),
            onEvent = {},
        )
    }
}

private val APP_ICON = 26.dp

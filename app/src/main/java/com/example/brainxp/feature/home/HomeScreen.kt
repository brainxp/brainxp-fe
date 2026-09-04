package com.example.brainxp.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Flame
import com.composables.icons.lucide.Library
import com.composables.icons.lucide.Lucide
import com.example.brainxp.R
import com.example.brainxp.blocking.ProtectionStatus
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.ErrorState
import com.example.brainxp.core.ui.HeroCard
import com.example.brainxp.core.ui.LoadingState
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.RowGroup
import com.example.brainxp.core.ui.StatusPill
import com.example.brainxp.core.ui.shortDuration
import com.example.brainxp.domain.model.UnlockState
import com.example.brainxp.feature.health.DegradedBanner
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        TopBar(state = state, onEvent = onEvent)

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

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
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
            ProtectionPill(state = state, onEvent = onEvent)
        }
    }
}

@Composable
private fun ProtectionPill(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
) {
    val label =
        when (state.protection) {
            ProtectionStatus.ACTIVE -> R.string.home_protection_active
            ProtectionStatus.DEGRADED -> R.string.home_protection_degraded
            ProtectionStatus.OFF -> R.string.home_protection_off
        }
    val tone =
        when (state.protection) {
            ProtectionStatus.ACTIVE -> PillTone.OK
            ProtectionStatus.DEGRADED -> PillTone.ALERT
            ProtectionStatus.OFF -> PillTone.NEUTRAL
        }

    StatusPill(
        text = stringResource(label),
        tone = tone,
        onClick = { onEvent(HomeEvent.ToggleProtection) },
    )
}

@Composable
private fun ReadyContent(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Hero(state)

        if (state.balanceStale) {
            Text(
                text = stringResource(R.string.home_stale_balance),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.capReached) {
            Text(
                text = stringResource(R.string.home_cap_reached, shortDuration(state.secondsUntilReset)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        PrimaryButton(
            text = stringResource(R.string.home_start_earning),
            onClick = { onEvent(HomeEvent.StartEarning) },
        )

        if (state.unlockRunning) {
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

        Text(
            text =
                stringResource(
                    if (state.appsOpen) R.string.home_apps_open else R.string.home_apps_locked,
                ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = spacing.sm),
        )

        LockedApps(state = state, onEvent = onEvent)

        RowGroup {
            item(
                title = stringResource(R.string.home_library_title),
                subtitle = stringResource(R.string.home_library_sub),
                leading = { Icon(imageVector = Lucide.Library, contentDescription = null) },
                onClick = { onEvent(HomeEvent.OpenLibrary) },
            )
            item(
                title = stringResource(R.string.home_daily_cap),
                value = shortDuration(state.dailyCapSeconds),
            )
            item(
                title = stringResource(R.string.home_reset_in),
                value = shortDuration(state.secondsUntilReset),
            )
        }

        TextButton(onClick = { onEvent(HomeEvent.OpenProgress) }) {
            Text(
                text = stringResource(R.string.home_progress),
                style = MaterialTheme.typography.labelMedium,
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
        value = if (running) shortDuration(state.remaining) else shortDuration(state.balanceSeconds),
        progress = if (state.dailyCapSeconds > 0) state.spentFraction else null,
        footer = {
            Text(
                text =
                    if (state.dailyCapSeconds > 0) {
                        stringResource(
                            R.string.home_spent_of_cap,
                            shortDuration(state.spentTodaySeconds),
                            shortDuration(state.dailyCapSeconds),
                        )
                    } else {
                        stringResource(R.string.home_no_cap)
                    },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        },
    )
}

@Composable
private fun LockedApps(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
) {
    RowGroup {
        if (state.lockedApps.isEmpty()) {
            item(
                title = stringResource(R.string.home_no_apps_title),
                subtitle = stringResource(R.string.home_no_apps_sub),
            )
        } else {
            state.lockedApps.forEach { app ->
                item(
                    title = app.label,
                    subtitle = app.packageName,
                    value =
                        stringResource(
                            if (state.appsOpen) R.string.home_app_open else R.string.home_app_locked,
                        ),
                    emphasiseValue = state.appsOpen,
                    onClick = { onEvent(HomeEvent.OpenApp(app.packageName)) },
                )
            }
        }
    }
}

private val PILL_ICON = 13.dp

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
                            endAtElapsed = 0L,
                            endAtWallClock = 0L,
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

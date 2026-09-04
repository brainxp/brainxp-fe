package com.example.brainxp.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.brainxp.R
import com.example.brainxp.blocking.ProtectionStatus
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.ErrorState
import com.example.brainxp.core.ui.HeroCard
import com.example.brainxp.core.ui.LoadingState
import com.example.brainxp.core.ui.PrimaryButton
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
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
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
private fun ReadyContent(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        if (state.unlockRunning) {
            HeroCard(
                label = stringResource(R.string.home_countdown_label),
                value = state.remaining.inWholeMinutes.toString(),
                unit = stringResource(R.string.home_unit_minutes),
                footer = {
                    Text(
                        text = stringResource(R.string.home_running_footer),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                },
            )
        } else {
            HeroCard(
                label = stringResource(R.string.home_balance_label),
                value = state.rewardMinutes.toString(),
                unit = stringResource(R.string.home_unit_minutes),
                footer = {
                    Text(
                        text = stringResource(R.string.home_locked_footer),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                },
            )
        }

        if (state.balanceStale) {
            Text(
                text = stringResource(R.string.home_stale_balance),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        PrimaryButton(
            text = stringResource(R.string.home_start_earning),
            onClick = { onEvent(HomeEvent.StartEarning) },
            modifier = Modifier.padding(top = spacing.lg),
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
    }
}

@Preview(name = "Home locked", showBackground = true)
@Composable
private fun HomeLockedPreview() {
    BrainXPTheme {
        HomeScreen(
            state = HomeUiState(phase = HomeUiState.Phase.Ready, rewardMinutes = 25),
            onEvent = {},
        )
    }
}

@Preview(name = "Home unlocked", showBackground = true)
@Composable
private fun HomeUnlockedPreview() {
    BrainXPTheme {
        HomeScreen(
            state =
                HomeUiState(
                    phase = HomeUiState.Phase.Ready,
                    rewardMinutes = 10,
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

@Preview(name = "Home degraded and stale", showBackground = true)
@Composable
private fun HomeDegradedPreview() {
    BrainXPTheme {
        HomeScreen(
            state =
                HomeUiState(
                    phase = HomeUiState.Phase.Ready,
                    rewardMinutes = 25,
                    balanceStale = true,
                    protection = ProtectionStatus.DEGRADED,
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

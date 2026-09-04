package com.example.brainxp.blocking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.HeroCard
import com.example.brainxp.core.ui.HeroTone
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.StatusPill
import com.example.brainxp.core.ui.Tokens
import com.example.brainxp.core.ui.shortDuration

@Composable
fun BlockScreen(
    appLabel: String,
    info: BlockedInfo,
    onEarnTime: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BrainXPTheme(darkTheme = false) {
        val spacing = BrainXPTheme.spacing

        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(Tokens.Blue900)
                    .padding(horizontal = spacing.xl, vertical = spacing.xxxl),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Spacer(modifier = Modifier.weight(1f))

            StatusPill(text = appLabel, tone = PillTone.ON_DARK)

            Text(
                text = stringResource(titleOf(info.state)),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                modifier = Modifier.padding(top = spacing.md),
            )

            Text(
                text = bodyOf(info),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = SECONDARY_INK),
            )

            HeroCard(
                label = stringResource(heroLabelOf(info.state)),
                value = shortDuration(info.balanceSeconds),
                tone = HeroTone.GHOST,
                progress = 1f,
                modifier = Modifier.padding(top = spacing.md),
                footer = {
                    Text(
                        text = stringResource(heroSubOf(info.state)),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = SECONDARY_INK),
                    )
                },
            )

            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = spacing.sm),
                shape = MaterialTheme.shapes.medium,
                color = Color.White.copy(alpha = NOTE_FILL),
            ) {
                Text(
                    text = stringResource(noteOf(info.state)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = SECONDARY_INK),
                    modifier = Modifier.padding(spacing.md),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            PrimaryButton(
                text = stringResource(actionOf(info.state)),
                onClick = onEarnTime,
                modifier = Modifier.fillMaxWidth(),
                onDark = info.state == BlockedState.NO_BALANCE || info.state == BlockedState.IDLE_HOLD,
            )
        }
    }
}

@Composable
private fun bodyOf(info: BlockedInfo): String =
    when (info.state) {
        BlockedState.NO_BALANCE -> {
            stringResource(R.string.block_screen_body)
        }

        BlockedState.NOT_STARTED -> {
            stringResource(R.string.block_screen_idle_body)
        }

        BlockedState.DAILY_CAP -> {
            stringResource(R.string.block_screen_cap_body, shortDuration(info.secondsUntilReset))
        }

        BlockedState.IDLE_HOLD -> {
            stringResource(R.string.block_screen_hold_body, info.idleDays, info.idleDaysAllowed)
        }

        BlockedState.GUARDIAN_STALE -> {
            stringResource(R.string.block_screen_stale_body)
        }
    }

private fun titleOf(state: BlockedState): Int =
    when (state) {
        BlockedState.NO_BALANCE -> R.string.block_screen_title
        BlockedState.NOT_STARTED -> R.string.block_screen_idle_title
        BlockedState.DAILY_CAP -> R.string.block_screen_cap_title
        BlockedState.IDLE_HOLD -> R.string.block_screen_hold_title
        BlockedState.GUARDIAN_STALE -> R.string.block_screen_stale_title
    }

private fun heroLabelOf(state: BlockedState): Int =
    when (state) {
        BlockedState.NO_BALANCE -> R.string.block_screen_hero_label
        BlockedState.NOT_STARTED -> R.string.block_screen_idle_hero
        BlockedState.DAILY_CAP -> R.string.block_screen_cap_hero
        BlockedState.IDLE_HOLD -> R.string.block_screen_hold_hero
        BlockedState.GUARDIAN_STALE -> R.string.block_screen_stale_hero
    }

private fun heroSubOf(state: BlockedState): Int =
    when (state) {
        BlockedState.NO_BALANCE -> R.string.block_screen_hero_sub
        BlockedState.NOT_STARTED -> R.string.block_screen_idle_hero_sub
        BlockedState.DAILY_CAP -> R.string.block_screen_cap_hero_sub
        BlockedState.IDLE_HOLD -> R.string.block_screen_hold_hero_sub
        BlockedState.GUARDIAN_STALE -> R.string.block_screen_stale_hero_sub
    }

private fun noteOf(state: BlockedState): Int =
    when (state) {
        BlockedState.NO_BALANCE -> R.string.block_screen_note
        BlockedState.NOT_STARTED -> R.string.block_screen_idle_note
        BlockedState.DAILY_CAP -> R.string.block_screen_cap_note
        BlockedState.IDLE_HOLD -> R.string.block_screen_hold_note
        BlockedState.GUARDIAN_STALE -> R.string.block_screen_stale_note
    }

private fun actionOf(state: BlockedState): Int =
    when (state) {
        BlockedState.NO_BALANCE -> R.string.block_screen_action
        BlockedState.NOT_STARTED -> R.string.block_screen_idle_action
        BlockedState.DAILY_CAP -> R.string.block_screen_cap_action
        BlockedState.IDLE_HOLD -> R.string.block_screen_hold_action
        BlockedState.GUARDIAN_STALE -> R.string.block_screen_stale_action
    }

private const val SECONDARY_INK = 0.68f
private const val NOTE_FILL = 0.09f

@Preview(name = "Blocked empty balance", showBackground = true, heightDp = 820)
@Composable
private fun BlockScreenPreview() {
    BlockScreen(
        appLabel = "Mobile Legends",
        info = BlockedInfo(BlockedState.NO_BALANCE),
        onEarnTime = {},
    )
}

@Preview(name = "Blocked not started", showBackground = true, heightDp = 820)
@Composable
private fun BlockScreenNotStartedPreview() {
    BlockScreen(
        appLabel = "Instagram",
        info = BlockedInfo(BlockedState.NOT_STARTED, balanceSeconds = 1_500),
        onEarnTime = {},
    )
}

@Preview(name = "Blocked daily cap", showBackground = true, heightDp = 820)
@Composable
private fun BlockScreenCapPreview() {
    BlockScreen(
        appLabel = "TikTok",
        info =
            BlockedInfo(
                BlockedState.DAILY_CAP,
                balanceSeconds = 1_500,
                secondsUntilReset = 18_000,
            ),
        onEarnTime = {},
    )
}

@Preview(name = "Blocked balance held", showBackground = true, heightDp = 820)
@Composable
private fun BlockScreenHoldPreview() {
    BlockScreen(
        appLabel = "YouTube",
        info =
            BlockedInfo(
                BlockedState.IDLE_HOLD,
                balanceSeconds = 1_500,
                idleDays = 4,
                idleDaysAllowed = 2,
            ),
        onEarnTime = {},
    )
}

@Preview(name = "Blocked guardian stale", showBackground = true, heightDp = 820)
@Composable
private fun BlockScreenStalePreview() {
    BlockScreen(
        appLabel = "Roblox",
        info = BlockedInfo(BlockedState.GUARDIAN_STALE, balanceSeconds = 1_500),
        onEarnTime = {},
    )
}

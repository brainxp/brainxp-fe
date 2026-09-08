package com.example.brainxp.blocking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTextStyles
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.Tokens
import com.example.brainxp.core.ui.longDuration
import com.example.brainxp.core.ui.shortDuration

private data class LedgerRow(
    val label: String,
    val value: String,
    val lead: Boolean = false,
)

@Composable
fun BlockScreen(
    appLabel: String,
    info: BlockedInfo,
    onAction: (BlockAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BrainXPTheme(darkTheme = false) {
        val spacing = BrainXPTheme.spacing

        BoxWithConstraints(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(Tokens.Blue900)
                    .safeDrawingPadding(),
        ) {
            val wide = maxWidth >= SIDE_BY_SIDE_WIDTH && maxHeight < STACKED_HEIGHT

            if (wide) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = spacing.xl, vertical = spacing.xl),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xxl),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(REASON_SHARE),
                        verticalArrangement = Arrangement.spacedBy(spacing.md),
                    ) {
                        Brand()
                        Reason(appLabel = appLabel, info = info)
                    }
                    Column(
                        modifier = Modifier.weight(FACTS_SHARE),
                        verticalArrangement = Arrangement.spacedBy(spacing.md),
                    ) {
                        Ledger(info = info)
                        Actions(info = info, onAction = onAction)
                    }
                }
            } else {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = spacing.xl)
                            .padding(top = spacing.xl, bottom = spacing.xxl),
                ) {
                    Brand(modifier = Modifier.widthIn(max = READING_WIDTH))
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .widthIn(max = READING_WIDTH),
                        verticalArrangement =
                            Arrangement.spacedBy(spacing.xl, alignment = Alignment.CenterVertically),
                    ) {
                        Reason(appLabel = appLabel, info = info)
                        Ledger(info = info)
                    }
                    Actions(
                        info = info,
                        onAction = onAction,
                        modifier =
                            Modifier
                                .widthIn(max = READING_WIDTH)
                                .padding(top = spacing.xxxl),
                    )
                }
            }
        }
    }
}

@Composable
private fun Reason(
    appLabel: String,
    info: BlockedInfo,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Text(
            text = stringResource(R.string.block_locked_label, appLabel),
            style = MaterialTheme.typography.labelMedium,
            color = accentOf(info.state),
        )
        Text(
            text = stringResource(titleOf(info.state)),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
        )
        Text(
            text = bodyOf(info),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = SECONDARY_INK),
        )
    }
}

@Composable
private fun Brand(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        modifier = modifier,
    )
}

@Composable
private fun Ledger(
    info: BlockedInfo,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(HAIRLINE)
                    .background(Color.White.copy(alpha = RULE_INK)),
        )

        ledgerOf(info).forEach { row -> LedgerLine(row = row, accent = accentOf(info.state)) }
    }
}

@Composable
private fun Actions(
    info: BlockedInfo,
    onAction: (BlockAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        PrimaryButton(
            text = stringResource(actionOf(info.state)),
            onClick = { onAction(actionKindOf(info.state)) },
            onDark = true,
        )

        TextButton(
            onClick = { onAction(BlockAction.CLOSE) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.block_screen_close),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = SECONDARY_INK),
            )
        }

        Text(
            text = stringResource(noteOf(info.state)),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = QUIET_INK),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LedgerLine(
    row: LedgerRow,
    accent: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.md),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = row.label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = QUIET_INK),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = row.value,
            style = if (row.lead) BrainXPTextStyles.numericSmall else MaterialTheme.typography.titleSmall,
            color = if (row.lead) accent else Color.White,
        )
    }
}

@Composable
private fun ledgerOf(info: BlockedInfo): List<LedgerRow> {
    val rows =
        mutableListOf(
            LedgerRow(
                label = stringResource(R.string.block_ledger_balance),
                value = shortDuration(info.balanceSeconds),
                lead = true,
            ),
        )

    if (info.dailyCapSeconds > 0) {
        rows +=
            LedgerRow(
                label = stringResource(R.string.block_ledger_today),
                value =
                    stringResource(
                        R.string.block_ledger_today_value,
                        shortDuration(info.spentTodaySeconds),
                        shortDuration(info.dailyCapSeconds),
                    ),
            )
    }

    when (info.state) {
        BlockedState.DAILY_CAP -> {
            rows +=
                LedgerRow(
                    label = stringResource(R.string.block_ledger_reset),
                    value = longDuration(info.secondsUntilReset),
                )
        }

        BlockedState.IDLE_HOLD -> {
            rows +=
                LedgerRow(
                    label = stringResource(R.string.block_ledger_idle),
                    value =
                        stringResource(
                            R.string.block_ledger_idle_value,
                            info.idleDays,
                            info.idleDaysAllowed,
                        ),
                )
        }

        else -> {
            Unit
        }
    }

    return rows
}

private fun accentOf(state: BlockedState): Color =
    when (state) {
        BlockedState.NOT_STARTED -> Tokens.Mint
        BlockedState.GUARDIAN_STALE -> Tokens.Amber400
        BlockedState.NO_BALANCE, BlockedState.DAILY_CAP, BlockedState.IDLE_HOLD -> Tokens.Blue300
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

private const val SECONDARY_INK = 0.74f
private const val QUIET_INK = 0.6f
private const val RULE_INK = 0.16f
private const val REASON_SHARE = 1.1f
private const val FACTS_SHARE = 1f
private val HAIRLINE = 1.dp
private val READING_WIDTH = 480.dp
private val SIDE_BY_SIDE_WIDTH = 560.dp
private val STACKED_HEIGHT = 520.dp

@Preview(name = "Blocked portrait", showBackground = true, widthDp = 411, heightDp = 880)
@Composable
private fun BlockScreenPreview() {
    BlockScreen(
        appLabel = "Brawlhalla",
        info = BlockedInfo(BlockedState.NO_BALANCE, dailyCapSeconds = 3_600),
        onAction = {},
    )
}

@Preview(name = "Blocked landscape", showBackground = true, widthDp = 880, heightDp = 411)
@Composable
private fun BlockScreenLandscapePreview() {
    BlockScreen(
        appLabel = "Brawlhalla",
        info =
            BlockedInfo(
                BlockedState.NOT_STARTED,
                balanceSeconds = 801,
                spentTodaySeconds = 0,
                dailyCapSeconds = 3_600,
            ),
        onAction = {},
    )
}

@Preview(name = "Blocked daily cap", showBackground = true, widthDp = 411, heightDp = 880)
@Composable
private fun BlockScreenCapPreview() {
    BlockScreen(
        appLabel = "TikTok",
        info =
            BlockedInfo(
                BlockedState.DAILY_CAP,
                balanceSeconds = 1_500,
                secondsUntilReset = 18_000,
                spentTodaySeconds = 3_600,
                dailyCapSeconds = 3_600,
            ),
        onAction = {},
    )
}

@Preview(name = "Blocked balance held", showBackground = true, widthDp = 880, heightDp = 411)
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
                dailyCapSeconds = 3_600,
            ),
        onAction = {},
    )
}

@Preview(name = "Blocked guardian stale", showBackground = true, widthDp = 411, heightDp = 880)
@Composable
private fun BlockScreenStalePreview() {
    BlockScreen(
        appLabel = "Roblox",
        info = BlockedInfo(BlockedState.GUARDIAN_STALE, balanceSeconds = 1_500),
        onAction = {},
    )
}

package com.example.brainxp.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.brainxp.R
import java.time.LocalDate

private val SHORT_DAYS =
    listOf(
        R.string.week_mon_short,
        R.string.week_tue_short,
        R.string.week_wed_short,
        R.string.week_thu_short,
        R.string.week_fri_short,
        R.string.week_sat_short,
        R.string.week_sun_short,
    )

private val FULL_DAYS =
    listOf(
        R.string.week_mon,
        R.string.week_tue,
        R.string.week_wed,
        R.string.week_thu,
        R.string.week_fri,
        R.string.week_sat,
        R.string.week_sun,
    )

private val WEEKDAYS = 0..4
private val WEEKEND = 5..6
private val WHOLE_WEEK = 0..6

private fun todayIndex(): Int = runCatching { LocalDate.now().dayOfWeek.value - 1 }.getOrDefault(0)

@Composable
fun WeekEditor(
    minutes: List<Int>,
    onChange: (List<Int>) -> Unit,
    maxMinutes: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var picked by remember { mutableIntStateOf(todayIndex()) }
    val safe = picked.coerceIn(minutes.indices)
    val tallest = (minutes.maxOrNull() ?: 0).coerceAtLeast(1)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(HAIRLINE, MaterialTheme.colorScheme.outline),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(BrainXPTheme.spacing.md),
                horizontalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.xs),
            ) {
                minutes.forEachIndexed { index, value ->
                    DayBar(
                        index = index,
                        value = value,
                        tallest = tallest,
                        active = index == safe,
                        enabled = enabled,
                        onPick = { picked = index },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            HorizontalDivider(thickness = HAIRLINE, color = MaterialTheme.colorScheme.outline)

            Row(
                modifier = Modifier.fillMaxWidth().padding(BrainXPTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(FULL_DAYS[safe]),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                val setDay = { typed: Int ->
                    onChange(minutes.mapIndexed { at, old -> if (at == safe) typed.coerceIn(0, maxMinutes) else old })
                }

                Stepper(
                    value = stringResource(R.string.week_minutes_value, minutes[safe]),
                    onDecrease = { setDay(minutes[safe] - STEP_MINUTES) },
                    onIncrease = { setDay(minutes[safe] + STEP_MINUTES) },
                    canDecrease = enabled && minutes[safe] > 0,
                    canIncrease = enabled && minutes[safe] < maxMinutes,
                    entry =
                        StepperEntry(
                            value = minutes[safe],
                            range = 0..maxMinutes,
                            label = stringResource(FULL_DAYS[safe]),
                            onCommit = setDay,
                        ).takeIf { enabled },
                )
            }

            HorizontalDivider(thickness = HAIRLINE, color = MaterialTheme.colorScheme.outline)

            MatchRow(
                enabled = enabled,
                onSpread = { span ->
                    onChange(minutes.mapIndexed { at, old -> if (at in span) minutes[safe] else old })
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MatchRow(
    enabled: Boolean,
    onSpread: (IntRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth().padding(BrainXPTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.sm),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.week_same),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MatchChip(label = R.string.week_weekdays, enabled = enabled, onClick = { onSpread(WEEKDAYS) })
        MatchChip(label = R.string.week_weekend, enabled = enabled, onClick = { onSpread(WEEKEND) })
        MatchChip(label = R.string.week_all, enabled = enabled, onClick = { onSpread(WHOLE_WEEK) })
    }
}

@Composable
private fun MatchChip(
    label: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier,
        shape = PillShape,
        color = scheme.surface,
        border = BorderStroke(HAIRLINE, scheme.outline),
        enabled = enabled,
        onClick = onClick,
    ) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) scheme.onSurface else scheme.onSurfaceVariant,
            modifier =
                Modifier.padding(
                    horizontal = BrainXPTheme.spacing.md,
                    vertical = BrainXPTheme.spacing.sm,
                ),
        )
    }
}

@Composable
private fun DayBar(
    index: Int,
    value: Int,
    tallest: Int,
    active: Boolean,
    enabled: Boolean,
    onPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val share by animateFloatAsState(
        targetValue = (value.toFloat() / tallest).coerceIn(0f, 1f),
        label = "day",
    )
    val ink = if (active) scheme.primary else scheme.onSurfaceVariant
    val fill =
        when {
            value == 0 -> scheme.surfaceContainerHigh
            active -> scheme.primary
            else -> scheme.primaryContainer
        }

    Column(
        modifier =
            modifier.selectable(
                selected = active,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onPick,
            ),
        verticalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = value.toString(), style = MaterialTheme.typography.labelSmall, color = ink)
        Box(
            modifier = Modifier.fillMaxWidth().height(TRACK),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(share.coerceAtLeast(FLOOR))
                        .background(fill, MaterialTheme.shapes.small),
            )
        }
        Text(text = stringResource(SHORT_DAYS[index]), style = MaterialTheme.typography.labelSmall, color = ink)
    }
}

private val HAIRLINE = 1.dp
private val TRACK = 58.dp
private const val FLOOR = 0.05f
private const val STEP_MINUTES = 15
private const val SAMPLE_MAX = 1_440
private val SAMPLE_WEEK = listOf(60, 60, 60, 60, 60, 90, 90)

@Composable
private fun WeekEditorSample(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(BrainXPTheme.spacing.lg)) {
        WeekEditor(minutes = SAMPLE_WEEK, onChange = {}, maxMinutes = SAMPLE_MAX)
    }
}

@Preview(name = "WeekEditor light", showBackground = true)
@Composable
private fun WeekEditorPreview() {
    BrainXPTheme { WeekEditorSample() }
}

@Preview(name = "WeekEditor dark", showBackground = true)
@Composable
private fun WeekEditorDarkPreview() {
    BrainXPTheme(darkTheme = true) { WeekEditorSample() }
}

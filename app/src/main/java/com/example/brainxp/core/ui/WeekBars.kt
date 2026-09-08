package com.example.brainxp.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun WeekBars(
    values: List<Int>,
    maxValue: Int,
    label: @Composable (Int) -> String,
    modifier: Modifier = Modifier,
    onStep: ((Int) -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val spacing = BrainXPTheme.spacing

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        values.forEachIndexed { index, value ->
            val share = if (maxValue <= 0) 0f else (value.toFloat() / maxValue).coerceIn(0f, 1f)

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .let { if (onStep == null) it else it.clickable { onStep(index) } },
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(TRACK)
                            .background(scheme.surfaceContainerHigh, MaterialTheme.shapes.small),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(share.coerceAtLeast(FLOOR))
                                .background(
                                    if (value > 0) scheme.primary else scheme.outline,
                                    MaterialTheme.shapes.small,
                                ),
                    )
                }
                Text(
                    text = DAY_LABELS[index],
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    text = label(value),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (value > 0) scheme.primary else scheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val DAY_LABELS = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
private val TRACK = 86.dp
private const val FLOOR = 0.04f

private val SAMPLE_WEEK = listOf(60, 60, 60, 60, 90, 120, 120)
private const val SAMPLE_MAX = 180

@Composable
private fun WeekBarsSample(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(BrainXPTheme.spacing.lg)) {
        WeekBars(
            values = SAMPLE_WEEK,
            maxValue = SAMPLE_MAX,
            label = { "$it" },
            onStep = {},
        )
    }
}

@Preview(name = "WeekBars light", showBackground = true)
@Composable
private fun WeekBarsPreview() {
    BrainXPTheme { WeekBarsSample() }
}

@Preview(name = "WeekBars dark", showBackground = true)
@Composable
private fun WeekBarsDarkPreview() {
    BrainXPTheme(darkTheme = true) { WeekBarsSample() }
}

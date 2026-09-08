package com.example.brainxp.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun MiniBarChart(
    values: List<Int>,
    title: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val spacing = BrainXPTheme.spacing
    val ceiling = maxOf(FLOOR_CEILING, values.maxOrNull() ?: 0)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = scheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface,
                )
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().height(TRACK),
                horizontalArrangement = Arrangement.spacedBy(GAP),
                verticalAlignment = Alignment.Bottom,
            ) {
                values.forEach { value ->
                    val share = (value.toFloat() / ceiling).coerceIn(MIN_SHARE, 1f)
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight(share)
                                .background(
                                    if (value > 0) scheme.primary else scheme.surfaceContainerHigh,
                                    MaterialTheme.shapes.extraSmall,
                                ),
                    )
                }
            }
        }
    }
}

private val TRACK = 96.dp
private val GAP = 5.dp
private const val FLOOR_CEILING = 600
private const val MIN_SHARE = 0.04f

private val SAMPLE_EARNED = listOf(0, 420, 780, 300, 0, 960, 540)

@Composable
private fun MiniBarChartSample(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(BrainXPTheme.spacing.lg)) {
        MiniBarChart(
            values = SAMPLE_EARNED,
            title = "Menit yang dia hasilkan",
            caption = "7 hari",
        )
    }
}

@Preview(name = "MiniBarChart light", showBackground = true)
@Composable
private fun MiniBarChartPreview() {
    BrainXPTheme { MiniBarChartSample() }
}

@Preview(name = "MiniBarChart dark", showBackground = true)
@Composable
private fun MiniBarChartDarkPreview() {
    BrainXPTheme(darkTheme = true) { MiniBarChartSample() }
}

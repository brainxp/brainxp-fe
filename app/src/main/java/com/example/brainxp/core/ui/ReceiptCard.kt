package com.example.brainxp.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ReceiptCard(
    lines: List<ReceiptLine>,
    totalLabel: String,
    totalValue: String,
    modifier: Modifier = Modifier,
    header: String? = null,
) {
    val spacing = BrainXPTheme.spacing

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(HAIRLINE, MaterialTheme.colorScheme.outline),
    ) {
        Column {
            if (header != null) {
                Text(
                    text = header,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.md),
                )
                DashedDivider()
            }

            Column(modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.md)) {
                lines.forEachIndexed { index, line ->
                    if (index > 0) {
                        DashedDivider(modifier = Modifier.padding(vertical = spacing.xs))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val ink =
                            when {
                                line.voided -> MaterialTheme.colorScheme.onSurfaceVariant
                                line.heading -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        val decoration = if (line.voided) TextDecoration.LineThrough else null
                        Text(
                            text = line.label,
                            style = BrainXPTextStyles.mono,
                            color = ink,
                            textDecoration = decoration,
                        )
                        Text(
                            text = line.value,
                            style = BrainXPTextStyles.mono,
                            color =
                                if (line.voided) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            textDecoration = decoration,
                        )
                    }
                }
            }

            Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.lg, vertical = spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = totalLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = totalValue,
                        style = BrainXPTextStyles.numericSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun DashedDivider(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier.fillMaxWidth().height(HAIRLINE)) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = size.height,
            cap = StrokeCap.Butt,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH_ON, DASH_OFF)),
        )
    }
}

private val HAIRLINE = 1.dp
private const val DASH_ON = 6f
private const val DASH_OFF = 6f

@Composable
private fun ReceiptSample(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(BrainXPTheme.spacing.lg)) {
        ReceiptCard(
            header = "SESI 12 MEI, 19.40",
            lines =
                listOf(
                    ReceiptLine("Benar 7 dari 8", "+21 menit"),
                    ReceiptLine("Materi baru", "x1.2"),
                    ReceiptLine("Batas harian tersisa", "48 menit"),
                ),
            totalLabel = "Waktu diperoleh",
            totalValue = "25 menit",
        )
    }
}

@Preview(name = "Receipt light", showBackground = true)
@Composable
private fun ReceiptCardPreview() {
    BrainXPTheme { ReceiptSample() }
}

@Preview(name = "Receipt dark", showBackground = true)
@Composable
private fun ReceiptCardDarkPreview() {
    BrainXPTheme(darkTheme = true) { ReceiptSample() }
}

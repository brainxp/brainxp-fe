package com.example.brainxp.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
    tone: PillTone = PillTone.NEUTRAL,
) {
    val extended = BrainXPTheme.extendedColors
    val scheme = MaterialTheme.colorScheme

    val container =
        when (tone) {
            PillTone.NEUTRAL -> scheme.surfaceContainerHigh
            PillTone.BLUE -> scheme.secondaryContainer
            PillTone.ALERT -> scheme.errorContainer
            PillTone.OK -> extended.okSurface
            PillTone.ON_DARK -> Color.White.copy(alpha = ON_DARK_FILL)
            PillTone.OUTLINE -> Color.Transparent
        }
    val ink =
        when (tone) {
            PillTone.NEUTRAL -> scheme.onSurfaceVariant
            PillTone.BLUE -> scheme.onSecondaryContainer
            PillTone.ALERT -> scheme.error
            PillTone.OK -> extended.okInk
            PillTone.ON_DARK -> Color.White
            PillTone.OUTLINE -> scheme.onSurfaceVariant
        }

    Surface(
        modifier = modifier,
        shape = PillShape,
        color = container,
        border = if (tone == PillTone.OUTLINE) BorderStroke(HAIRLINE, scheme.outline) else null,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = ink,
            modifier =
                Modifier.padding(
                    horizontal = PILL_HORIZONTAL,
                    vertical = PILL_VERTICAL,
                ),
        )
    }
}

private val PILL_HORIZONTAL = 10.dp
private val PILL_VERTICAL = 5.dp
private val HAIRLINE = 1.dp
private const val ON_DARK_FILL = 0.14f

@Composable
private fun PillRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(BrainXPTheme.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.sm),
    ) {
        StatusPill("Netral", tone = PillTone.NEUTRAL)
        StatusPill("Aktif", tone = PillTone.BLUE)
        StatusPill("Gagal", tone = PillTone.ALERT)
        StatusPill("Benar", tone = PillTone.OK)
        StatusPill("Opsional", tone = PillTone.OUTLINE)
    }
}

@Preview(name = "StatusPill tones", showBackground = true)
@Composable
private fun StatusPillPreview() {
    BrainXPTheme {
        PillRow()
    }
}

@Preview(name = "StatusPill dark", showBackground = true)
@Composable
private fun StatusPillDarkPreview() {
    BrainXPTheme(darkTheme = true) {
        PillRow()
    }
}

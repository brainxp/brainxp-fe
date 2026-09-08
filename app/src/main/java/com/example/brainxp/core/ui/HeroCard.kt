package com.example.brainxp.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun HeroCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    tone: HeroTone = HeroTone.PRIMARY,
    progress: Float? = null,
    badge: @Composable (() -> Unit)? = null,
    footer: @Composable (() -> Unit)? = null,
) {
    val spacing = BrainXPTheme.spacing
    val container =
        when (tone) {
            HeroTone.PRIMARY -> MaterialTheme.colorScheme.primary
            HeroTone.DARK -> Tokens.Blue800
            HeroTone.GHOST -> Color.White.copy(alpha = GHOST_FILL)
        }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = container,
        border = if (tone == HeroTone.GHOST) BorderStroke(HAIRLINE, Color.White.copy(alpha = GHOST_BORDER)) else null,
    ) {
        Box(
            modifier =
                Modifier.background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = HIGHLIGHT), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset.Unspecified,
                        radius = HIGHLIGHT_RADIUS,
                    ),
                ),
        ) {
            Column(
                modifier = Modifier.padding(spacing.xl),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = LABEL_ALPHA),
                        modifier = Modifier.weight(1f),
                    )
                    badge?.invoke()
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = value,
                        style = BrainXPTextStyles.numeric,
                        color = Color.White,
                    )
                    if (unit != null) {
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = LABEL_ALPHA),
                            modifier = Modifier.padding(start = spacing.xs, bottom = spacing.sm),
                        )
                    }
                }

                if (progress != null) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(TRACK)
                                .clip(PillShape)
                                .background(Color.White.copy(alpha = TRACK_ALPHA)),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                                    .height(TRACK)
                                    .clip(PillShape)
                                    .background(Color.White),
                        )
                    }
                }

                if (footer != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = spacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) { footer() }
                }
            }
        }
    }
}

private val HAIRLINE = 1.dp
private val TRACK = 5.dp
private const val HIGHLIGHT = 0.16f
private const val HIGHLIGHT_RADIUS = 520f
private const val LABEL_ALPHA = 0.72f
private const val TRACK_ALPHA = 0.24f
private const val GHOST_FILL = 0.09f
private const val GHOST_BORDER = 0.14f

@Composable
private fun HeroSample(
    tone: HeroTone,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(BrainXPTheme.spacing.lg)) {
        HeroCard(
            label = "Sisa waktu bermain",
            value = "25",
            unit = "menit",
            tone = tone,
            progress = 0.45f,
            footer = {
                Text(
                    text = "Batas harian 90 menit",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = LABEL_ALPHA),
                )
                StatusPill("Aktif", tone = PillTone.ON_DARK)
            },
        )
    }
}

@Preview(name = "Hero primary", showBackground = true)
@Composable
private fun HeroPrimaryPreview() {
    BrainXPTheme { HeroSample(HeroTone.PRIMARY) }
}

@Preview(name = "Hero dark tone", showBackground = true)
@Composable
private fun HeroDarkTonePreview() {
    BrainXPTheme { HeroSample(HeroTone.DARK) }
}

@Preview(name = "Hero in dark theme", showBackground = true)
@Composable
private fun HeroDarkThemePreview() {
    BrainXPTheme(darkTheme = true) { HeroSample(HeroTone.PRIMARY) }
}

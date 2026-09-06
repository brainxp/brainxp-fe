package com.example.brainxp.feature.questions

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.PreviewColumn
import com.example.brainxp.core.ui.Tokens

@Composable
internal fun SwipeHint(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val sweep by
        rememberInfiniteTransition(label = "swipe")
            .animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = SWEEP_MILLIS),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "sweep",
            )

    Canvas(modifier = modifier.fillMaxWidth().height(STRIP)) {
        val inset = size.height * SIDE_SHARE
        val card = Size(width = size.width - inset * 2, height = size.height)
        val middle = size.height / 2f

        drawRoundRect(
            color = scheme.surfaceContainerHigh,
            topLeft = Offset(inset, 0f),
            size = card,
            cornerRadius = CornerRadius(CARD_RADIUS.toPx()),
        )

        chevron(at = Offset(inset / 2f, middle), pointsLeft = true, tint = scheme.outline)
        chevron(at = Offset(size.width - inset / 2f, middle), pointsLeft = false, tint = scheme.outline)

        val from = inset + card.width * TRACK_START
        val to = inset + card.width * TRACK_END
        val finger = Offset(x = from + (to - from) * sweep, y = middle)

        drawLine(
            color = Tokens.Amber400.copy(alpha = TRAIL_ALPHA),
            start = Offset(from, middle),
            end = finger,
            strokeWidth = TRAIL.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(color = Tokens.Amber500, radius = FINGER.toPx(), center = finger)
    }
}

private fun DrawScope.chevron(
    at: Offset,
    pointsLeft: Boolean,
    tint: androidx.compose.ui.graphics.Color,
) {
    val reach = CHEVRON.toPx()
    val direction = if (pointsLeft) -1f else 1f
    val path =
        Path().apply {
            moveTo(at.x - reach * direction, at.y - reach)
            lineTo(at.x + reach * direction, at.y)
            lineTo(at.x - reach * direction, at.y + reach)
        }

    drawPath(path = path, color = tint, style = Stroke(width = EDGE.toPx(), cap = StrokeCap.Round))
}

private val STRIP = 46.dp
private val CARD_RADIUS = 12.dp
private val EDGE = 2.dp
private val TRAIL = 4.dp
private val FINGER = 7.dp
private val CHEVRON = 5.dp
private const val SIDE_SHARE = 0.42f
private const val TRACK_START = 0.18f
private const val TRACK_END = 0.82f
private const val TRAIL_ALPHA = 0.45f
private const val SWEEP_MILLIS = 1_000

@Preview(showBackground = true)
@Composable
private fun SwipeHintPreview() {
    BrainXPTheme {
        PreviewColumn { SwipeHint() }
    }
}

package com.example.brainxp.feature.questions

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.PreviewColumn
import com.example.brainxp.core.ui.Tokens

@Composable
internal fun SwipeHint(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val turn by
        rememberInfiniteTransition(label = "swipe")
            .animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation =
                            keyframes {
                                durationMillis = CYCLE_MILLIS
                                0f at 0
                                0f at HOLD_MILLIS
                                1f at HOLD_MILLIS + TURN_MILLIS
                                1f at CYCLE_MILLIS using LinearEasing
                            },
                    ),
                label = "turn",
            )

    Canvas(modifier = modifier.fillMaxWidth().height(STRIP)) {
        val pageWidth = size.width * PAGE_SHARE
        val gap = GAP.toPx()
        val centred = (size.width - pageWidth) / 2f
        val shift = (pageWidth + gap) * turn
        val ahead = turn > HALF

        val quiet = CardInk(edge = scheme.outline, quiet = scheme.surfaceContainerHigh, paper = scheme.surface)
        val live = quiet.copy(edge = scheme.primary)
        val span = Size(pageWidth, size.height)

        clipRect {
            card(at = Offset(centred - shift, 0f), span = span, ink = if (ahead) quiet else live, answered = !ahead)
            card(
                at = Offset(centred + pageWidth + gap - shift, 0f),
                span = span,
                ink = if (ahead) live else quiet,
                answered = ahead,
            )
        }

        thumb(at = Offset(size.width * (THUMB_FROM + (THUMB_TO - THUMB_FROM) * turn), size.height / 2f))
    }
}

private data class CardInk(
    val edge: Color,
    val quiet: Color,
    val paper: Color,
)

private fun DrawScope.card(
    at: Offset,
    span: Size,
    ink: CardInk,
    answered: Boolean,
) {
    val radius = CornerRadius(CARD_RADIUS.toPx())
    drawRoundRect(color = ink.paper, topLeft = at, size = span, cornerRadius = radius)
    drawRoundRect(
        color = ink.edge,
        topLeft = at,
        size = span,
        cornerRadius = radius,
        style = Stroke(width = EDGE.toPx()),
    )

    val pad = CARD_PADDING.toPx()
    val inner = span.width - pad * 2
    val stroke = LINE.toPx()

    drawLine(
        color = ink.quiet,
        start = Offset(at.x + pad, at.y + pad + stroke),
        end = Offset(at.x + pad + inner * STEM_SHARE, at.y + pad + stroke),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )

    repeat(ROWS) { row ->
        val y = at.y + span.height * (ROW_FIRST + row * ROW_STEP)
        drawRoundRect(
            color = if (answered && row == PICKED_ROW) Tokens.Blue300 else ink.quiet,
            topLeft = Offset(at.x + pad, y),
            size = Size(inner, ROW_HEIGHT.toPx()),
            cornerRadius = CornerRadius(ROW_HEIGHT.toPx()),
        )
    }
}

private fun DrawScope.thumb(at: Offset) {
    val width = THUMB_WIDTH.toPx()
    val height = THUMB_HEIGHT.toPx()

    repeat(TRAILS) { index ->
        val gap = THUMB_WIDTH.toPx() + index * TRAIL_STEP.toPx()
        drawLine(
            color = Tokens.Amber400.copy(alpha = TRAIL_ALPHA / (index + 1)),
            start = Offset(at.x + gap, at.y),
            end = Offset(at.x + gap + TRAIL_LENGTH.toPx(), at.y),
            strokeWidth = LINE.toPx(),
            cap = StrokeCap.Round,
        )
    }

    drawRoundRect(
        color = Tokens.Amber500,
        topLeft = Offset(at.x - width / 2f, at.y - height / 2f),
        size = Size(width, height),
        cornerRadius = CornerRadius(width / 2f),
    )
}

private val STRIP = 66.dp
private val GAP = 10.dp
private val CARD_RADIUS = 10.dp
private val EDGE = 1.5.dp
private val LINE = 3.dp
private val CARD_PADDING = 8.dp
private val ROW_HEIGHT = 5.dp
private val THUMB_WIDTH = 11.dp
private val THUMB_HEIGHT = 20.dp
private val TRAIL_STEP = 7.dp
private val TRAIL_LENGTH = 7.dp
private const val PAGE_SHARE = 0.46f
private const val STEM_SHARE = 0.78f
private const val ROWS = 3
private const val ROW_FIRST = 0.42f
private const val ROW_STEP = 0.18f
private const val PICKED_ROW = 1
private const val TRAILS = 3
private const val TRAIL_ALPHA = 0.55f
private const val HALF = 0.5f
private const val THUMB_FROM = 0.74f
private const val THUMB_TO = 0.26f
private const val HOLD_MILLIS = 450
private const val TURN_MILLIS = 650
private const val CYCLE_MILLIS = 1_750

@Preview(showBackground = true)
@Composable
private fun SwipeHintPreview() {
    BrainXPTheme {
        PreviewColumn { SwipeHint() }
    }
}

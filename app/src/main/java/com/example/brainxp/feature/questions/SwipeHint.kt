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
import androidx.compose.ui.geometry.Rect
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
                                0f at 0 using LinearEasing
                                0f at HOLD_MILLIS using LinearEasing
                                1f at HOLD_MILLIS + TURN_MILLIS using LinearEasing
                                1f at CYCLE_MILLIS
                            },
                    ),
                label = "turn",
            )

    Canvas(modifier = modifier.fillMaxWidth().height(STRIP)) {
        val pageWidth = size.width * PAGE_SHARE
        val gap = GAP.toPx()
        val pageHeight = size.height - THUMB_ROOM.toPx()
        val start = (size.width - pageWidth) / 2f
        val shift = (pageWidth + gap) * turn

        clipRect(left = 0f, top = 0f, right = size.width, bottom = size.height) {
            val leaving =
                PageInk(
                    lead = if (turn < HALF) scheme.primary else scheme.outline,
                    quiet = scheme.outline,
                    paper = scheme.surface,
                    filled = scheme.surfaceContainerHigh,
                )
            val arriving = leaving.copy(lead = if (turn < HALF) scheme.outline else scheme.primary)

            page(origin = Offset(start - shift, 0f), size = Size(pageWidth, pageHeight), ink = leaving)
            page(
                origin = Offset(start + pageWidth + gap - shift, 0f),
                size = Size(pageWidth, pageHeight),
                ink = arriving,
            )
        }

        thumb(
            centre =
                Offset(
                    x = size.width * (THUMB_FROM + (THUMB_TO - THUMB_FROM) * turn),
                    y = size.height - THUMB_ROOM.toPx() / 2f,
                ),
        )
    }
}

private data class PageInk(
    val lead: Color,
    val quiet: Color,
    val paper: Color,
    val filled: Color,
)

private fun DrawScope.page(
    origin: Offset,
    size: Size,
    ink: PageInk,
) {
    drawRoundRect(
        color = ink.paper,
        topLeft = origin,
        size = size,
        cornerRadius = CornerRadius(PAGE_RADIUS.toPx()),
    )
    drawRoundRect(
        color = ink.lead,
        topLeft = origin,
        size = size,
        cornerRadius = CornerRadius(PAGE_RADIUS.toPx()),
        style = Stroke(width = EDGE.toPx()),
    )

    val pad = PAGE_PADDING.toPx()
    val line = LINE.toPx()
    drawLine(
        color = ink.lead,
        start = Offset(origin.x + pad, origin.y + pad + line),
        end = Offset(origin.x + size.width * STEM_SHARE, origin.y + pad + line),
        strokeWidth = line,
        cap = StrokeCap.Round,
    )
    repeat(ROWS) { row ->
        val y = origin.y + pad * ROW_GAP + (row + 1) * (pad * ROW_STEP)
        drawLine(
            color = if (row == 0) ink.quiet else ink.filled,
            start = Offset(origin.x + pad, y),
            end = Offset(origin.x + size.width - pad, y),
            strokeWidth = line,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.thumb(centre: Offset) {
    val width = THUMB_WIDTH.toPx()
    val height = THUMB_HEIGHT.toPx()

    repeat(TRAILS) { index ->
        val back = (index + 1) * TRAIL_STEP.toPx()
        drawLine(
            color = Tokens.Amber400.copy(alpha = TRAIL_ALPHA / (index + 1)),
            start = Offset(centre.x + back, centre.y),
            end = Offset(centre.x + back + TRAIL_LENGTH.toPx(), centre.y),
            strokeWidth = LINE.toPx(),
            cap = StrokeCap.Round,
        )
    }

    drawRoundRect(
        color = Tokens.Amber500,
        topLeft = Offset(centre.x - width / 2f, centre.y - height / 2f),
        size = Size(width, height),
        cornerRadius = CornerRadius(width / 2f),
    )
}

private val STRIP = 62.dp
private val GAP = 8.dp
private val PAGE_RADIUS = 8.dp
private val EDGE = 1.5.dp
private val LINE = 2.dp
private val PAGE_PADDING = 7.dp
private val THUMB_ROOM = 18.dp
private val THUMB_WIDTH = 9.dp
private val THUMB_HEIGHT = 16.dp
private val TRAIL_STEP = 6.dp
private val TRAIL_LENGTH = 5.dp
private const val PAGE_SHARE = 0.52f
private const val THUMB_FROM = 0.68f
private const val THUMB_TO = 0.3f
private const val STEM_SHARE = 0.62f
private const val ROWS = 2
private const val ROW_GAP = 2.6f
private const val ROW_STEP = 1.5f
private const val TRAILS = 2
private const val TRAIL_ALPHA = 0.5f
private const val HALF = 0.5f
private const val HOLD_MILLIS = 420
private const val TURN_MILLIS = 620
private const val CYCLE_MILLIS = 1_700

@Preview(showBackground = true)
@Composable
private fun SwipeHintPreview() {
    BrainXPTheme {
        PreviewColumn { SwipeHint() }
    }
}

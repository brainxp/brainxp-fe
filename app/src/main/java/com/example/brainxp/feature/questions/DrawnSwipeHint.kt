package com.example.brainxp.feature.questions

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.PreviewColumn
import com.example.brainxp.core.ui.Tokens

private data class CardInk(
    val edge: Color,
    val quiet: Color,
    val paper: Color,
)

private data class Gesture(
    val hand: Float,
    val deck: Float,
    val touch: Float,
    val alive: Float,
)

@Composable
internal fun DrawnSwipeHint(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val clock by
        rememberInfiniteTransition(label = "swipe")
            .animateFloat(
                initialValue = 0f,
                targetValue = CYCLE.toFloat(),
                animationSpec =
                    infiniteRepeatable(
                        animation =
                            keyframes {
                                durationMillis = CYCLE
                                0f at 0 using LinearEasing
                                CYCLE.toFloat() at CYCLE using LinearEasing
                            },
                    ),
                label = "clock",
            )

    val gesture = gestureAt(clock)

    Canvas(modifier = modifier.fillMaxWidth().height(STRIP)) {
        val deckHeight = size.height - HAND_ROOM.toPx()
        val cardWidth = size.width * CARD_SHARE
        val gap = GAP.toPx()
        val rest = (size.width - cardWidth) / 2f
        val slide = (cardWidth + gap) * gesture.deck
        val span = Size(cardWidth, deckHeight)
        val quiet = CardInk(scheme.outline, scheme.surfaceContainerHigh, scheme.surface)
        val live = quiet.copy(edge = scheme.primary)

        clipRect {
            card(
                at = Offset(rest - slide, 0f),
                span = span,
                ink = if (gesture.deck > HALF) quiet else live,
                answered = true,
                shown = gesture.alive,
            )
            card(
                at = Offset(rest + cardWidth + gap - slide, 0f),
                span = span,
                ink = if (gesture.deck > HALF) live else quiet,
                answered = false,
                shown = gesture.alive,
            )

            if (gesture.touch > 0f) {
                hand(
                    tip =
                        Offset(
                            x = size.width * (FROM + (TO - FROM) * gesture.hand),
                            y = deckHeight * TRACK,
                        ),
                    shown = gesture.touch,
                    moving = gesture.hand > 0f && gesture.hand < FULL,
                )
            }
        }
    }
}

private fun gestureAt(at: Float): Gesture {
    val hand =
        when {
            at < PRESS_END -> 0f
            at < HAND_END -> LinearOutSlowInEasing.transform((at - PRESS_END) / (HAND_END - PRESS_END))
            else -> FULL
        }
    val deck =
        when {
            at < PRESS_END -> 0f
            at < SNAP_END -> FastOutSlowInEasing.transform((at - PRESS_END) / (SNAP_END - PRESS_END))
            else -> FULL
        }
    val touch =
        when {
            at < RISE -> at / RISE
            at < HAND_END -> FULL
            at < HAND_END + FADE -> FULL - (at - HAND_END) / FADE
            else -> 0f
        }
    val alive =
        when {
            at < FADE -> at / FADE
            at > CYCLE - FADE -> (CYCLE - at) / FADE
            else -> FULL
        }

    return Gesture(
        hand = hand,
        deck = deck,
        touch = touch.coerceIn(0f, FULL),
        alive = alive.coerceIn(0f, FULL),
    )
}

private fun DrawScope.card(
    at: Offset,
    span: Size,
    ink: CardInk,
    answered: Boolean,
    shown: Float,
) {
    val radius = CornerRadius(CARD_RADIUS.toPx())
    drawRoundRect(color = ink.paper.copy(alpha = shown), topLeft = at, size = span, cornerRadius = radius)
    drawRoundRect(
        color = ink.edge.copy(alpha = shown),
        topLeft = at,
        size = span,
        cornerRadius = radius,
        style = Stroke(width = EDGE.toPx()),
    )

    val pad = CARD_PADDING.toPx()
    val inner = span.width - pad * 2
    drawRoundRect(
        color = ink.quiet.copy(alpha = shown),
        topLeft = Offset(at.x + pad, at.y + pad),
        size = Size(inner * STEM_SHARE, STEM_HEIGHT.toPx()),
        cornerRadius = CornerRadius(STEM_HEIGHT.toPx()),
    )

    repeat(ROWS) { row ->
        val picked = answered && row == PICKED_ROW
        drawRoundRect(
            color = (if (picked) Tokens.Blue300 else ink.quiet).copy(alpha = shown),
            topLeft = Offset(at.x + pad, at.y + span.height * (ROW_FIRST + row * ROW_STEP)),
            size = Size(inner, ROW_HEIGHT.toPx()),
            cornerRadius = CornerRadius(ROW_HEIGHT.toPx()),
        )
    }
}

private fun DrawScope.hand(
    tip: Offset,
    shown: Float,
    moving: Boolean,
) {
    if (moving) {
        repeat(WAKE) { line ->
            val from = tip.x + WAKE_GAP.toPx() * (line + 1)
            drawLine(
                color = Tokens.Amber400.copy(alpha = WAKE_ALPHA * shown / (line + 1)),
                start = Offset(from, tip.y - WAKE_RISE.toPx() * line),
                end = Offset(from + WAKE_LENGTH.toPx(), tip.y - WAKE_RISE.toPx() * line),
                strokeWidth = WAKE_EDGE.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }

    drawCircle(
        color = Tokens.Amber400.copy(alpha = TOUCH_ALPHA * shown),
        radius = TOUCH_RADIUS.toPx(),
        center = tip,
    )

    rotate(degrees = TILT, pivot = tip) {
        val finger = FINGER_WIDTH.toPx()
        val reach = FINGER_LENGTH.toPx()
        val palm = Size(PALM_WIDTH.toPx(), PALM_HEIGHT.toPx())
        val skin = Tokens.Amber500.copy(alpha = shown)

        drawRoundRect(
            color = skin,
            topLeft = Offset(tip.x - finger / 2f, tip.y),
            size = Size(finger, reach),
            cornerRadius = CornerRadius(finger / 2f),
        )
        drawRoundRect(
            color = skin,
            topLeft = Offset(tip.x - finger / 2f - PALM_INSET.toPx(), tip.y + reach - PALM_LIFT.toPx()),
            size = palm,
            cornerRadius = CornerRadius(PALM_RADIUS.toPx()),
        )
        drawRoundRect(
            color = skin,
            topLeft =
                Offset(
                    tip.x - finger / 2f - PALM_INSET.toPx() - THUMB_WIDTH.toPx() * THUMB_OUT,
                    tip.y + reach + THUMB_DROP.toPx(),
                ),
            size = Size(THUMB_WIDTH.toPx(), THUMB_LENGTH.toPx()),
            cornerRadius = CornerRadius(THUMB_WIDTH.toPx() / 2f),
        )
        drawLine(
            color = Tokens.Amber700.copy(alpha = CREASE_ALPHA * shown),
            start = Offset(tip.x - finger / 2f + CREASE_INSET.toPx(), tip.y + reach - PALM_LIFT.toPx()),
            end = Offset(tip.x + finger / 2f - CREASE_INSET.toPx(), tip.y + reach - PALM_LIFT.toPx()),
            strokeWidth = CREASE_EDGE.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private val STRIP = 104.dp
private val HAND_ROOM = 40.dp
private val GAP = 12.dp
private val CARD_RADIUS = 12.dp
private val EDGE = 1.5.dp
private val CARD_PADDING = 10.dp
private val STEM_HEIGHT = 5.dp
private val ROW_HEIGHT = 7.dp
private val FINGER_WIDTH = 15.dp
private val FINGER_LENGTH = 30.dp
private val PALM_WIDTH = 29.dp
private val PALM_HEIGHT = 28.dp
private val PALM_RADIUS = 12.dp
private val PALM_INSET = 6.dp
private val PALM_LIFT = 9.dp
private val THUMB_WIDTH = 11.dp
private val THUMB_LENGTH = 19.dp
private val THUMB_DROP = 2.dp
private val TOUCH_RADIUS = 13.dp
private val CREASE_INSET = 2.dp
private val CREASE_EDGE = 1.5.dp
private val WAKE_GAP = 10.dp
private val WAKE_LENGTH = 7.dp
private val WAKE_EDGE = 4.dp
private val WAKE_RISE = 0.dp

private const val CARD_SHARE = 0.5f
private const val STEM_SHARE = 0.66f
private const val ROWS = 3
private const val ROW_FIRST = 0.34f
private const val ROW_STEP = 0.2f
private const val PICKED_ROW = 1
private const val FROM = 0.6f
private const val TO = 0.34f
private const val TRACK = 0.66f
private const val TILT = -14f
private const val THUMB_OUT = 0.62f
private const val WAKE = 3
private const val WAKE_ALPHA = 0.8f
private const val TOUCH_ALPHA = 0.24f
private const val CREASE_ALPHA = 0.35f
private const val FULL = 1f
private const val HALF = 0.5f
private const val RISE = 260f
private const val PRESS_END = 320f
private const val HAND_END = 960f
private const val SNAP_END = 1_180f
private const val FADE = 180f
private const val CYCLE = 2_300

@Preview(showBackground = true)
@Composable
private fun DrawnSwipeHintPreview() {
    BrainXPTheme {
        PreviewColumn { DrawnSwipeHint() }
    }
}

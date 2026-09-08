package com.example.brainxp.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun RevealRow(
    open: Boolean,
    onReveal: (Boolean) -> Unit,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    val reveal = with(LocalDensity.current) { ACTION_WIDTH.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(open) {
        if (!open && offset.value != 0f) {
            offset.animateTo(0f)
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.matchParentSize(),
            shape = shape,
            color = MaterialTheme.colorScheme.errorContainer,
        ) {
            Box(contentAlignment = Alignment.CenterEnd) {
                RevealAction(
                    label = actionLabel,
                    onClick = onAction,
                    modifier = Modifier.fillMaxHeight(),
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .offset { IntOffset(offset.value.roundToInt(), 0) }
                    .clip(shape)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state =
                            rememberDraggableState { delta ->
                                scope.launch {
                                    offset.snapTo((offset.value + delta).coerceIn(-reveal, 0f))
                                }
                            },
                        onDragStopped = {
                            val stay = offset.value <= -reveal / 2
                            offset.animateTo(if (stay) -reveal else 0f)
                            onReveal(stay)
                        },
                    ),
        ) {
            content()
        }
    }
}

@Composable
private fun RevealAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.width(ACTION_WIDTH),
        color = MaterialTheme.colorScheme.errorContainer,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = BrainXPTheme.spacing.md),
            )
        }
    }
}

private val ACTION_WIDTH = 96.dp

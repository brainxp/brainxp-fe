package com.example.brainxp.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(CONTROL_HEIGHT)
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(TRACK_INSET),
        horizontalArrangement = Arrangement.spacedBy(TRACK_INSET),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { option ->
            val active = option == selected
            val container by
                animateColorAsState(
                    targetValue =
                        if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    label = "segmentContainer",
                )
            val ink by
                animateColorAsState(
                    targetValue =
                        if (active) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    label = "segmentInk",
                )

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .height(SEGMENT_HEIGHT)
                        .clip(PillShape)
                        .background(container)
                        .selectable(
                            selected = active,
                            role = Role.Tab,
                            onClick = { onSelect(option) },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelMedium,
                    color = ink,
                    modifier = Modifier.padding(horizontal = spacing.sm),
                )
            }
        }
    }
}

private val CONTROL_HEIGHT = 48.dp
private val SEGMENT_HEIGHT = 40.dp
private val TRACK_INSET = 4.dp

private enum class SampleMode {
    SELF,
    FAMILY,
}

@Composable
private fun SegmentedSample(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(BrainXPTheme.spacing.lg)) {
        SegmentedControl(
            options = SampleMode.entries,
            selected = SampleMode.SELF,
            onSelect = {},
            label = { if (it == SampleMode.SELF) "Mode sendiri" else "Mode keluarga" },
        )
    }
}

@Preview(name = "SegmentedControl light", showBackground = true)
@Composable
private fun SegmentedControlPreview() {
    BrainXPTheme { SegmentedSample() }
}

@Preview(name = "SegmentedControl dark", showBackground = true)
@Composable
private fun SegmentedControlDarkPreview() {
    BrainXPTheme(darkTheme = true) { SegmentedSample() }
}

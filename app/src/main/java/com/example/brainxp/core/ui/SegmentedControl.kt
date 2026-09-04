package com.example.brainxp.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(GAP),
        verticalArrangement = Arrangement.spacedBy(GAP),
    ) {
        options.forEach { option ->
            Segment(
                text = label(option),
                active = option == selected,
                onClick = { onSelect(option) },
            )
        }
    }
}

@Composable
private fun Segment(
    text: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container by
        animateColorAsState(
            targetValue =
                if (active) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
            label = "segmentContainer",
        )
    val border by
        animateColorAsState(
            targetValue =
                if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            label = "segmentBorder",
        )
    val ink by
        animateColorAsState(
            targetValue =
                if (active) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            label = "segmentInk",
        )

    Surface(
        modifier = modifier.selectable(selected = active, role = Role.Tab, onClick = onClick),
        shape = SegmentShape,
        color = container,
        border = BorderStroke(BORDER, border),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = ink,
            modifier = Modifier.padding(horizontal = HORIZONTAL, vertical = VERTICAL),
        )
    }
}

private val GAP = 6.dp
private val BORDER = 1.5.dp
private val HORIZONTAL = 13.dp
private val VERTICAL = 9.dp

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

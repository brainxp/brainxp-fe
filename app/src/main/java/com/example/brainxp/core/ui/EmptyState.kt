package com.example.brainxp.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val spacing = BrainXPTheme.spacing

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Plate(modifier = Modifier.fillMaxWidth())

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.xl),
        )

        body?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = spacing.sm),
            )
        }

        if (actionText != null && onAction != null) {
            Column(modifier = Modifier.padding(top = spacing.xxl)) {
                PrimaryButton(text = actionText, onClick = onAction)
            }
        }
    }
}

@Composable
private fun Plate(modifier: Modifier = Modifier) {
    val ruleColor = MaterialTheme.colorScheme.outlineVariant
    val lines =
        Brush.linearGradient(
            colorStops =
                arrayOf(
                    0f to ruleColor.copy(alpha = 0f),
                    RULE_STOP to ruleColor.copy(alpha = 0f),
                    RULE_STOP to ruleColor,
                    1f to ruleColor,
                ),
            start = Offset.Zero,
            end = Offset(0f, RULE_SPACING_PX),
            tileMode = androidx.compose.ui.graphics.TileMode.Repeated,
        )

    Surface(
        modifier = modifier.height(PLATE_HEIGHT),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(BORDER_WIDTH, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.fillMaxSize().background(lines)) {}
    }
}

private val PLATE_HEIGHT = 150.dp
private val BORDER_WIDTH = 1.dp
private const val RULE_SPACING_PX = 16f
private const val RULE_STOP = 0.94f

@Preview(name = "EmptyState light", showBackground = true)
@Composable
private fun EmptyStateLightPreview() {
    BrainXPTheme {
        EmptyState(
            title = "Belum ada materi",
            body = "Tambahkan catatan atau foto halaman buku untuk mulai mengumpulkan waktu.",
            actionText = "Tambah materi",
            onAction = {},
        )
    }
}

@Preview(name = "EmptyState no action", showBackground = true)
@Composable
private fun EmptyStateNoActionPreview() {
    BrainXPTheme {
        EmptyState(
            title = "Belum cukup data",
            body = "Selesaikan beberapa sesi dulu supaya grafiknya berarti.",
        )
    }
}

@Preview(name = "EmptyState dark", showBackground = true)
@Composable
private fun EmptyStateDarkPreview() {
    BrainXPTheme(darkTheme = true) {
        EmptyState(
            title = "Belum ada materi",
            body = "Tambahkan catatan atau foto halaman buku untuk mulai mengumpulkan waktu.",
            actionText = "Tambah materi",
            onAction = {},
        )
    }
}

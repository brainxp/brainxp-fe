package com.example.brainxp.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun Note(
    text: String,
    modifier: Modifier = Modifier,
    alert: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val extended = BrainXPTheme.extendedColors
    val container = if (alert) scheme.errorContainer else scheme.primaryContainer
    val edge = if (alert) extended.alertBorder else scheme.primary.copy(alpha = BORDER_ALPHA)
    val ink = if (alert) scheme.onErrorContainer else scheme.onSurfaceVariant
    val badge = if (alert) scheme.error else scheme.primary

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = container,
        border = BorderStroke(HAIRLINE, edge),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = HORIZONTAL, vertical = VERTICAL),
            horizontalArrangement = Arrangement.spacedBy(GAP),
        ) {
            Surface(
                modifier = Modifier.size(BADGE).padding(top = BADGE_NUDGE),
                shape = PillShape,
                color = badge,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (alert) "!" else "i",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onPrimary,
                    )
                }
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = ink,
            )
        }
    }
}

private val HAIRLINE = 1.dp
private val HORIZONTAL = 14.dp
private val VERTICAL = 13.dp
private val GAP = 11.dp
private val BADGE = 20.dp
private val BADGE_NUDGE = 1.dp
private const val BORDER_ALPHA = 0.25f

@Composable
private fun NoteSample(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(BrainXPTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.sm),
    ) {
        Note(text = "HP ini belum terdaftar di keluarga mana pun.")
        Note(text = "Kode itu sudah kedaluwarsa. Minta yang baru.", alert = true)
    }
}

@Preview(name = "Note light", showBackground = true)
@Composable
private fun NotePreview() {
    BrainXPTheme { NoteSample() }
}

@Preview(name = "Note dark", showBackground = true)
@Composable
private fun NoteDarkPreview() {
    BrainXPTheme(darkTheme = true) { NoteSample() }
}

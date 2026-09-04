package com.example.brainxp.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.User
import com.composables.icons.lucide.Users

@Composable
fun ChoiceRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = scheme.surface,
        border = BorderStroke(HAIRLINE, scheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = HORIZONTAL, vertical = VERTICAL),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GAP),
        ) {
            Surface(
                modifier = Modifier.size(TILE),
                shape = MaterialTheme.shapes.small,
                color = if (highlight) scheme.primaryContainer else scheme.surfaceContainerHigh,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (highlight) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                        modifier = Modifier.size(GLYPH),
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(BODY_SHARE),
                verticalArrangement = Arrangement.spacedBy(SUB_GAP),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
            }

            Icon(
                imageVector = Lucide.ChevronRight,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(GLYPH),
            )
        }
    }
}

private val HAIRLINE = 1.dp
private val HORIZONTAL = 14.dp
private val VERTICAL = 12.dp
private val GAP = 12.dp
private val TILE = 38.dp
private val GLYPH = 18.dp
private val SUB_GAP = 1.dp
private const val BODY_SHARE = 0.86f

@Composable
private fun ChoiceRowSample(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(BrainXPTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.sm),
    ) {
        ChoiceRow(
            title = "Mode Keluarga",
            subtitle = "Orang tua yang atur dari HP-nya. Anak tinggal ikut, nggak bisa ubah-ubah.",
            icon = Lucide.Users,
            highlight = true,
            onClick = {},
        )
        ChoiceRow(
            title = "Mode Pribadi",
            subtitle = "Kamu atur sendiri buat kamu sendiri. Kalau dilonggarkan, jalannya besok.",
            icon = Lucide.User,
            onClick = {},
        )
    }
}

@Preview(name = "ChoiceRow light", showBackground = true)
@Composable
private fun ChoiceRowPreview() {
    BrainXPTheme { ChoiceRowSample() }
}

@Preview(name = "ChoiceRow dark", showBackground = true)
@Composable
private fun ChoiceRowDarkPreview() {
    BrainXPTheme(darkTheme = true) { ChoiceRowSample() }
}

package com.example.brainxp.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun RowGroup(
    modifier: Modifier = Modifier,
    content: @Composable RowGroupScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(HAIRLINE, MaterialTheme.colorScheme.outline),
    ) {
        Column {
            RowGroupScope().content()
        }
    }
}

class RowGroupScope internal constructor() {
    private var rendered = 0

    @Composable
    fun item(
        title: String,
        modifier: Modifier = Modifier,
        subtitle: String? = null,
        value: String? = null,
        emphasiseValue: Boolean = false,
        onClick: (() -> Unit)? = null,
        leading: @Composable (() -> Unit)? = null,
    ) {
        if (rendered > 0) {
            HorizontalDivider(thickness = HAIRLINE, color = MaterialTheme.colorScheme.outline)
        }
        rendered++
        RowGroupItem(
            title = title,
            subtitle = subtitle,
            value = value,
            emphasiseValue = emphasiseValue,
            leading = leading,
            modifier = if (onClick == null) modifier else modifier.clickable(onClick = onClick),
        )
    }
}

@Composable
private fun RowGroupItem(
    title: String,
    subtitle: String?,
    value: String?,
    emphasiseValue: Boolean,
    leading: @Composable (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        if (leading != null) {
            Surface(
                modifier = Modifier.size(AVATAR),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) { leading() }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(if (value == null) 1f else TITLE_WIDTH),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color =
                    if (emphasiseValue) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
        }
    }
}

private val HAIRLINE = 1.dp
private val AVATAR = 38.dp
private const val TITLE_WIDTH = 0.72f

@Composable
private fun RowGroupSample(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(BrainXPTheme.spacing.lg)) {
        RowGroup {
            item(title = "Aplikasi dibatasi", subtitle = "7 aplikasi", value = "Atur")
            item(title = "Saldo waktu", value = "25 menit", emphasiseValue = true)
            item(title = "Batas harian", subtitle = "Berlaku sampai tengah malam", value = "90 menit")
        }
    }
}

@Preview(name = "RowGroup light", showBackground = true)
@Composable
private fun RowGroupPreview() {
    BrainXPTheme { RowGroupSample() }
}

@Preview(name = "RowGroup dark", showBackground = true)
@Composable
private fun RowGroupDarkPreview() {
    BrainXPTheme(darkTheme = true) { RowGroupSample() }
}

package com.example.brainxp.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import com.example.brainxp.R

@Composable
fun ScreenNav(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(BAR),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.sm),
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(TAP)) {
                Icon(
                    imageVector = Lucide.ChevronLeft,
                    contentDescription = stringResource(R.string.action_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(if (trailing == null) 1f else TITLE_SHARE),
        )
        trailing?.invoke()
    }
}

private val BAR = 46.dp
private val TAP = 34.dp
private const val TITLE_SHARE = 0.7f

@Composable
private fun ScreenNavSample(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(BrainXPTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.md),
    ) {
        ScreenNav(title = "Pengaturan awal")
        ScreenNav(title = "Mode Keluarga", onBack = {})
        ScreenNav(title = "Gabung ke keluarga", onBack = {}) {
            StatusPill(text = "Langkah 2", tone = PillTone.BLUE)
        }
    }
}

@Preview(name = "ScreenNav light", showBackground = true)
@Composable
private fun ScreenNavPreview() {
    BrainXPTheme { ScreenNavSample() }
}

@Preview(name = "ScreenNav dark", showBackground = true)
@Composable
private fun ScreenNavDarkPreview() {
    BrainXPTheme(darkTheme = true) { ScreenNavSample() }
}

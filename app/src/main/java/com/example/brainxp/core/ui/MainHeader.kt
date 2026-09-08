package com.example.brainxp.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
fun MainHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().height(BAR),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.xs),
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(TAP)) {
                Icon(
                    imageVector = Lucide.ChevronLeft,
                    contentDescription = stringResource(R.string.action_back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.sm),
            content = actions,
        )
    }
}

private val BAR = 56.dp
private val TAP = 40.dp

@Composable
private fun MainHeaderSample(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(BrainXPTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.md),
    ) {
        MainHeader(title = "Beranda") {
            StatusPill(text = "3 hari", tone = PillTone.BLUE)
        }
        MainHeader(title = "Pustaka materi", onBack = {}) {
            StatusPill(text = "12", tone = PillTone.OUTLINE)
        }
        MainHeader(title = "Judul yang sangat panjang sekali sampai terpotong", onBack = {})
    }
}

@Preview(name = "MainHeader light", showBackground = true)
@Composable
private fun MainHeaderPreview() {
    BrainXPTheme { MainHeaderSample() }
}

@Preview(name = "MainHeader dark", showBackground = true)
@Composable
private fun MainHeaderDarkPreview() {
    BrainXPTheme(darkTheme = true) { MainHeaderSample() }
}

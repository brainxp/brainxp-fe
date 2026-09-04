package com.example.brainxp.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun PlaceholderScreen(
    name: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    actions: List<PlaceholderAction> = emptyList(),
) {
    val spacing = BrainXPTheme.spacing

    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Text(text = name, style = MaterialTheme.typography.headlineSmall)
                detail?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        itemsIndexed(actions) { index, action ->
            if (index == 0) {
                PrimaryButton(text = action.label, onClick = action.onClick)
            } else {
                OutlinedButton(
                    onClick = action.onClick,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(text = action.label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Preview(name = "Placeholder light", showBackground = true)
@Composable
private fun PlaceholderScreenPreview() {
    BrainXPTheme {
        PlaceholderScreen(
            name = "Home",
            detail = "MainRoute.Home",
            actions = listOf(PlaceholderAction("Open settings") {}),
        )
    }
}

@Preview(name = "Placeholder dark", showBackground = true)
@Composable
private fun PlaceholderScreenDarkPreview() {
    BrainXPTheme(darkTheme = true) {
        PlaceholderScreen(
            name = "Home",
            detail = "MainRoute.Home",
            actions = listOf(PlaceholderAction("Open settings") {}),
        )
    }
}

package com.example.brainxp.feature.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme

@Composable
fun ProtectionHealthBanner(
    onFixPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ProtectionHealthViewModel = hiltViewModel()
    val degraded by viewModel.degraded.collectAsStateWithLifecycle()

    if (degraded) {
        DegradedBanner(onFixPermissions = onFixPermissions, modifier = modifier)
    }
}

@Composable
fun DegradedBanner(
    onFixPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Column(
            modifier = Modifier.padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(
                text = stringResource(R.string.degraded_banner_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = stringResource(R.string.degraded_banner_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            TextButton(onClick = onFixPermissions) {
                Text(
                    text = stringResource(R.string.degraded_banner_action),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Preview(name = "Degraded banner", showBackground = true)
@Composable
private fun DegradedBannerPreview() {
    BrainXPTheme { DegradedBanner(onFixPermissions = {}) }
}

@Preview(name = "Degraded banner dark", showBackground = true)
@Composable
private fun DegradedBannerDarkPreview() {
    BrainXPTheme(darkTheme = true) { DegradedBanner(onFixPermissions = {}) }
}

package com.example.brainxp.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    onDark: Boolean = false,
) {
    val spacing = BrainXPTheme.spacing
    val container = if (onDark) Tokens.Neutral0 else MaterialTheme.colorScheme.primary
    val ink = if (onDark) Tokens.Blue900 else MaterialTheme.colorScheme.onPrimary

    Button(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .height(BUTTON_HEIGHT),
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.medium,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = container,
                contentColor = ink,
                disabledContainerColor = container.copy(alpha = DISABLED_ALPHA),
                disabledContentColor = ink.copy(alpha = DISABLED_ALPHA),
            ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = ELEVATION),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(SPINNER_SIZE),
                    strokeWidth = SPINNER_STROKE,
                    color = ink,
                )
            }
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private val BUTTON_HEIGHT = 52.dp
private val ELEVATION = 4.dp
private val SPINNER_SIZE = 18.dp
private val SPINNER_STROKE = 2.dp
private const val DISABLED_ALPHA = 0.42f

@Preview(name = "PrimaryButton light", showBackground = true)
@Composable
private fun PrimaryButtonLightPreview() {
    BrainXPTheme {
        PreviewColumn {
            PrimaryButton(text = "Earn time", onClick = {})
            PrimaryButton(text = "Submitting", onClick = {}, loading = true)
            PrimaryButton(text = "Not enough balance", onClick = {}, enabled = false)
        }
    }
}

@Preview(name = "PrimaryButton dark", showBackground = true)
@Composable
private fun PrimaryButtonDarkPreview() {
    BrainXPTheme(darkTheme = true) {
        PreviewColumn {
            PrimaryButton(text = "Earn time", onClick = {})
            PrimaryButton(text = "Submitting", onClick = {}, loading = true)
            PrimaryButton(text = "Not enough balance", onClick = {}, enabled = false)
        }
    }
}

@Composable
internal fun PreviewColumn(content: @Composable () -> Unit) {
    val spacing = BrainXPTheme.spacing
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
        content = { content() },
    )
}

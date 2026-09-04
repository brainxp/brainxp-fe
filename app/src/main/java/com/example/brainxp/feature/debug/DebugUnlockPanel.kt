package com.example.brainxp.feature.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.RowGroup

@Composable
fun DebugUnlockPanel(
    onGrant: (Int) -> Unit,
    onSetWarningLead: (Int) -> Unit,
    onEndUnlock: () -> Unit,
    modifier: Modifier = Modifier,
    currentWarningLead: Int = 60,
) {
    val spacing = BrainXPTheme.spacing
    var duration by remember { mutableStateOf("30") }
    var lead by remember { mutableStateOf(currentWarningLead.toString()) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text(
            text = stringResource(R.string.debug_unlock_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.debug_unlock_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SecondsField(
            label = stringResource(R.string.debug_duration_label),
            value = duration,
            onChange = { duration = it },
        )

        SecondsField(
            label = stringResource(R.string.debug_warning_label),
            value = lead,
            onChange = { lead = it },
        )

        PrimaryButton(
            text = stringResource(R.string.debug_start_session),
            onClick = {
                onSetWarningLead(lead.toIntOrNull() ?: currentWarningLead)
                onGrant(duration.toIntOrNull() ?: DEFAULT_DURATION)
            },
            enabled = duration.toIntOrNull() != null && lead.toIntOrNull() != null,
            modifier = Modifier.padding(top = spacing.sm),
        )

        OutlinedButton(
            onClick = onEndUnlock,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                text = stringResource(R.string.debug_end_session),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        RowGroup(modifier = Modifier.padding(top = spacing.lg)) {
            item(
                title = stringResource(R.string.debug_hint_title),
                subtitle = stringResource(R.string.debug_hint_body),
            )
        }
    }
}

@Composable
private fun SecondsField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { entered -> onChange(entered.filter(Char::isDigit).take(MAX_DIGITS)) },
        label = { Text(label) },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        suffix = { Text(stringResource(R.string.debug_seconds_suffix)) },
        modifier = modifier.fillMaxWidth(),
    )
}

private const val DEFAULT_DURATION = 30
private const val MAX_DIGITS = 5

@Preview(name = "Debug unlock panel", showBackground = true, heightDp = 800)
@Composable
private fun DebugUnlockPanelPreview() {
    BrainXPTheme {
        DebugUnlockPanel(onGrant = {}, onSetWarningLead = {}, onEndUnlock = {})
    }
}

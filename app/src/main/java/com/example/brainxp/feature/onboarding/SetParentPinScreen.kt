package com.example.brainxp.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.ScreenNav

private const val PIN_LENGTH = 4

@Composable
fun SetParentPinScreen(
    onSet: (String) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
) {
    val spacing = BrainXPTheme.spacing
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    val matched = pin.length == PIN_LENGTH && pin == confirm

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = spacing.screenHorizontal)
                .padding(bottom = spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        ScreenNav(title = stringResource(R.string.setpin_title))

        Text(
            text = stringResource(R.string.setpin_heading),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.setpin_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.size(spacing.lg))

        OutlinedTextField(
            value = pin,
            onValueChange = { typed -> pin = typed.filter { it.isDigit() }.take(PIN_LENGTH) },
            label = { Text(text = stringResource(R.string.setpin_field)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = confirm,
            onValueChange = { typed -> confirm = typed.filter { it.isDigit() }.take(PIN_LENGTH) },
            label = { Text(text = stringResource(R.string.setpin_confirm)) },
            singleLine = true,
            isError = confirm.length == PIN_LENGTH && !matched,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.weight(1f))

        Note(text = stringResource(R.string.setpin_note))

        PrimaryButton(
            text = stringResource(R.string.setpin_save),
            onClick = { onSet(pin) },
            enabled = matched && !busy,
            loading = busy,
        )
        TextButton(onClick = onSkip, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.setpin_skip), textAlign = TextAlign.Center)
        }
    }
}

@Preview(heightDp = 800)
@Composable
private fun SetParentPinPreview() {
    BrainXPTheme { SetParentPinScreen(onSet = {}, onSkip = {}) }
}

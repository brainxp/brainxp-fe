package com.example.brainxp.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    secret: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LABEL_GAP),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(INPUT),
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder =
                placeholder?.let {
                    {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurfaceVariant.copy(alpha = HINT_ALPHA),
                        )
                    }
                },
            visualTransformation =
                if (secret) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            shape = MaterialTheme.shapes.medium,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = scheme.surface,
                    unfocusedContainerColor = scheme.surface,
                    focusedBorderColor = scheme.primary,
                    unfocusedBorderColor = scheme.outline,
                    disabledContainerColor = scheme.surfaceContainerHigh,
                ),
        )
    }
}

private val LABEL_GAP = 6.dp
private val INPUT = 56.dp
private const val HINT_ALPHA = 0.7f

@Composable
private fun FieldSample(modifier: Modifier = Modifier) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("rahasia") }

    Column(
        modifier = modifier.padding(BrainXPTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.md),
    ) {
        Field(
            label = "Email",
            value = email,
            onValueChange = { email = it },
            placeholder = "kamu@contoh.id",
            keyboardType = KeyboardType.Email,
        )
        Field(
            label = "Kata sandi",
            value = password,
            onValueChange = { password = it },
            secret = true,
            keyboardType = KeyboardType.Password,
        )
    }
}

@Preview(name = "Field light", showBackground = true)
@Composable
private fun FieldPreview() {
    BrainXPTheme { FieldSample() }
}

@Preview(name = "Field dark", showBackground = true)
@Composable
private fun FieldDarkPreview() {
    BrainXPTheme(darkTheme = true) { FieldSample() }
}

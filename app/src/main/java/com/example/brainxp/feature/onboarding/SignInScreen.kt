package com.example.brainxp.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.Field
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.ScreenNav

private const val MIN_NEW_PASSWORD = 10

@Composable
fun SignInScreen(
    mode: SetupMode,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    busy: Boolean = false,
) {
    val spacing = BrainXPTheme.spacing
    var creating by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val ready =
        email.contains('@') &&
            password.length >= if (creating) MIN_NEW_PASSWORD else 1

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = spacing.xl)
                .padding(top = spacing.sm, bottom = spacing.screenBottom),
    ) {
        ScreenNav(
            title =
                stringResource(
                    if (creating) R.string.signin_nav_register else R.string.signin_nav_login,
                ),
            onBack = onBack,
        )
        Text(
            text =
                stringResource(
                    if (creating) R.string.signin_title_register else R.string.signin_title_login,
                ),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.size(spacing.sm))
        Text(
            text =
                stringResource(
                    if (mode == SetupMode.FAMILY) {
                        R.string.signin_body_family
                    } else {
                        R.string.signin_body_personal
                    },
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.size(spacing.xxl))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            if (creating) {
                Field(
                    label = stringResource(R.string.signin_name),
                    value = name,
                    onValueChange = { name = it },
                    placeholder = stringResource(R.string.signin_name_hint),
                    enabled = !busy,
                )
            }
            Field(
                label = stringResource(R.string.signin_email),
                value = email,
                onValueChange = { email = it },
                placeholder = stringResource(R.string.signin_email_hint),
                enabled = !busy,
                keyboardType = KeyboardType.Email,
            )
            Field(
                label = stringResource(R.string.signin_password),
                value = password,
                onValueChange = { password = it },
                placeholder = if (creating) stringResource(R.string.signin_password_hint) else null,
                enabled = !busy,
                secret = true,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            PrimaryButton(
                text =
                    stringResource(
                        if (creating) R.string.signin_submit_register else R.string.signin_submit_login,
                    ),
                onClick = onDone,
                enabled = ready,
                loading = busy,
            )
            TextButton(
                onClick = { creating = !creating },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text =
                        stringResource(
                            if (creating) R.string.signin_to_login else R.string.signin_to_register,
                        ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Preview(name = "SignIn family", showBackground = true, heightDp = 780)
@Composable
private fun SignInPreview() {
    BrainXPTheme { SignInScreen(mode = SetupMode.FAMILY, onDone = {}, onBack = {}) }
}

@Preview(name = "SignIn personal", showBackground = true, heightDp = 780)
@Composable
private fun SignInPersonalPreview() {
    BrainXPTheme { SignInScreen(mode = SetupMode.PERSONAL, onDone = {}) }
}

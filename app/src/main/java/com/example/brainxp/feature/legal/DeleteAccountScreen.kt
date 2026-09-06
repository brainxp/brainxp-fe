package com.example.brainxp.feature.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.RowGroup
import com.example.brainxp.core.ui.ScreenNav

@Composable
fun DeleteAccountScreen(
    onSendRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onPrivacyPolicy: (() -> Unit)? = null,
    noMailApp: Boolean = false,
) {
    val spacing = BrainXPTheme.spacing
    var confirming by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = spacing.screenHorizontal)
                .padding(bottom = spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        ScreenNav(title = stringResource(R.string.delete_account_title), onBack = onBack)

        Text(
            text = stringResource(R.string.delete_account_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = stringResource(R.string.delete_account_removed_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = spacing.sm),
        )
        RowGroup {
            item(title = stringResource(R.string.delete_account_removed_materials))
            item(title = stringResource(R.string.delete_account_removed_sessions))
            item(title = stringResource(R.string.delete_account_removed_balance))
            item(title = stringResource(R.string.delete_account_removed_profile))
        }

        Note(text = stringResource(R.string.delete_account_timeline))

        if (noMailApp) {
            Note(text = stringResource(R.string.delete_account_no_mail), alert = true)
        }

        onPrivacyPolicy?.let { open ->
            TextButton(onClick = open, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.privacy_open),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (confirming) {
            Note(text = stringResource(R.string.delete_account_warning), alert = true)
            PrimaryButton(
                text = stringResource(R.string.delete_account_confirm),
                onClick = onSendRequest,
            )
            TextButton(onClick = { confirming = false }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.detail_delete_cancel),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            PrimaryButton(
                text = stringResource(R.string.delete_account_start),
                onClick = { confirming = true },
            )
        }
    }
}

@Preview(heightDp = 1000)
@Composable
private fun DeleteAccountPreview() {
    BrainXPTheme { DeleteAccountScreen(onSendRequest = {}, onBack = {}, onPrivacyPolicy = {}) }
}

@Preview(heightDp = 1000)
@Composable
private fun DeleteAccountDarkPreview() {
    BrainXPTheme(darkTheme = true) {
        DeleteAccountScreen(onSendRequest = {}, onBack = {}, noMailApp = true)
    }
}

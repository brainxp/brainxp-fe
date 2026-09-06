package com.example.brainxp.feature.legal

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.ScreenNav

@Composable
fun PrivacyPolicyScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val spacing = BrainXPTheme.spacing

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
        ScreenNav(title = stringResource(R.string.privacy_title), onBack = onBack)

        Text(
            text = stringResource(R.string.privacy_updated),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Note(text = stringResource(R.string.privacy_summary))

        SECTIONS.forEach { section ->
            Section(heading = section.heading, body = section.body)
        }
    }
}

@Composable
private fun Section(
    @StringRes heading: Int,
    @StringRes body: Int,
) {
    val spacing = BrainXPTheme.spacing
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
        modifier = Modifier.padding(top = spacing.sm),
    ) {
        Text(
            text = stringResource(heading),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class PolicySection(
    @StringRes val heading: Int,
    @StringRes val body: Int,
)

private val SECTIONS =
    listOf(
        PolicySection(R.string.privacy_collect_heading, R.string.privacy_collect_body),
        PolicySection(R.string.privacy_apps_heading, R.string.privacy_apps_body),
        PolicySection(R.string.privacy_material_heading, R.string.privacy_material_body),
        PolicySection(R.string.privacy_accessibility_heading, R.string.privacy_accessibility_body),
        PolicySection(R.string.privacy_family_heading, R.string.privacy_family_body),
        PolicySection(R.string.privacy_share_heading, R.string.privacy_share_body),
        PolicySection(R.string.privacy_keep_heading, R.string.privacy_keep_body),
        PolicySection(R.string.privacy_delete_heading, R.string.privacy_delete_body),
        PolicySection(R.string.privacy_children_heading, R.string.privacy_children_body),
        PolicySection(R.string.privacy_contact_heading, R.string.privacy_contact_body),
    )

@Preview(heightDp = 1600)
@Composable
private fun PrivacyPolicyPreview() {
    BrainXPTheme { PrivacyPolicyScreen(onBack = {}) }
}

@Preview(heightDp = 1600)
@Composable
private fun PrivacyPolicyDarkPreview() {
    BrainXPTheme(darkTheme = true) { PrivacyPolicyScreen(onBack = {}) }
}

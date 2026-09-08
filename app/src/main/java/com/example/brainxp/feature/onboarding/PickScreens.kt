package com.example.brainxp.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.User
import com.composables.icons.lucide.Users
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.ChoiceRow
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.domain.model.DeviceRole

@Composable
fun PickModeScreen(
    onPick: (SetupMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingChoiceScaffold(
        navTitle = stringResource(R.string.mode_nav),
        title = stringResource(R.string.mode_title),
        body = stringResource(R.string.mode_body),
        note = stringResource(R.string.mode_note),
        modifier = modifier,
    ) {
        ChoiceRow(
            title = stringResource(R.string.mode_family_title),
            subtitle = stringResource(R.string.mode_family_sub),
            icon = Lucide.Users,
            highlight = true,
            onClick = { onPick(SetupMode.FAMILY) },
        )
        ChoiceRow(
            title = stringResource(R.string.mode_personal_title),
            subtitle = stringResource(R.string.mode_personal_sub),
            icon = Lucide.User,
            onClick = { onPick(SetupMode.PERSONAL) },
        )
    }
}

@Composable
fun PickRoleScreen(
    onPick: (DeviceRole) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    OnboardingChoiceScaffold(
        navTitle = stringResource(R.string.role_nav),
        title = stringResource(R.string.role_title),
        body = stringResource(R.string.role_body),
        note = stringResource(R.string.role_note),
        onBack = onBack,
        modifier = modifier,
    ) {
        ChoiceRow(
            title = stringResource(R.string.role_parent_title),
            subtitle = stringResource(R.string.role_parent_sub),
            icon = Lucide.Users,
            highlight = true,
            onClick = { onPick(DeviceRole.PARENT) },
        )
        ChoiceRow(
            title = stringResource(R.string.role_child_title),
            subtitle = stringResource(R.string.role_child_sub),
            icon = Lucide.User,
            onClick = { onPick(DeviceRole.CHILD) },
        )
    }
}

@Composable
private fun OnboardingChoiceScaffold(
    navTitle: String,
    title: String,
    body: String,
    note: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    choices: @Composable () -> Unit,
) {
    val spacing = BrainXPTheme.spacing

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = spacing.xl)
                .padding(top = spacing.sm, bottom = spacing.screenBottom),
    ) {
        ScreenNav(title = navTitle, onBack = onBack)
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.size(spacing.sm))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.size(spacing.xl))
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            choices()
        }
        Spacer(modifier = Modifier.weight(1f))
        Note(text = note)
    }
}

@Preview(name = "PickMode", showBackground = true, heightDp = 780)
@Composable
private fun PickModePreview() {
    BrainXPTheme { PickModeScreen(onPick = {}) }
}

@Preview(name = "PickRole", showBackground = true, heightDp = 780)
@Composable
private fun PickRolePreview() {
    BrainXPTheme { PickRoleScreen(onPick = {}, onBack = {}) }
}

package com.example.brainxp.feature.settings

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
import com.example.brainxp.core.ui.SegmentedControl
import com.example.brainxp.core.ui.levelLabel
import com.example.brainxp.data.repo.SubjectPolicy
import com.example.brainxp.domain.model.AcademicLevel

@Composable
fun SettingsScreen(
    policy: SubjectPolicy,
    onLevel: (AcademicLevel) -> Unit,
    onLanguage: (String) -> Unit,
    onApps: () -> Unit,
    onPermissions: () -> Unit,
    onSignOut: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    saving: Boolean = false,
    notice: String? = null,
) {
    val spacing = BrainXPTheme.spacing
    var leaving by remember { mutableStateOf(false) }

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
        ScreenNav(title = stringResource(R.string.settings_title), onBack = onBack)

        notice?.let { Note(text = it, alert = true) }

        Text(
            text = stringResource(R.string.settings_level),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SegmentedControl(
            options = AcademicLevel.entries,
            selected = policy.level ?: AcademicLevel.SMA,
            onSelect = onLevel,
            label = { levelLabel(it) },
        )

        Text(
            text = stringResource(R.string.settings_language),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SegmentedControl(
            options = listOf(LANGUAGE_ID, LANGUAGE_EN),
            selected = policy.language.ifBlank { LANGUAGE_ID },
            onSelect = onLanguage,
            label = { code -> stringResource(if (code == LANGUAGE_EN) R.string.lang_en else R.string.lang_id) },
        )

        RowGroup {
            item(
                title = stringResource(R.string.settings_questions),
                value = policy.questionsPerSession.toString(),
            )
            item(
                title = stringResource(R.string.settings_reset_hour),
                value = stringResource(R.string.settings_reset_hour_value, policy.dayResetHour),
            )
            item(
                title = stringResource(R.string.settings_idle_allowed),
                value = stringResource(R.string.home_rest_days_value, policy.idleDaysAllowed),
            )
        }

        policy.pendingWeakenAt?.let { at ->
            Note(text = stringResource(R.string.settings_pending, at), alert = true)
        }

        RowGroup {
            item(title = stringResource(R.string.settings_apps), onClick = onApps)
            item(title = stringResource(R.string.settings_permissions), onClick = onPermissions)
        }

        RowGroup {
            item(title = stringResource(R.string.settings_privacy), onClick = onPrivacyPolicy)
            item(title = stringResource(R.string.settings_delete_account), onClick = onDeleteAccount)
        }

        if (leaving) {
            Note(text = stringResource(R.string.settings_sign_out_warning), alert = true)
            PrimaryButton(
                text = stringResource(R.string.settings_sign_out_confirm),
                onClick = onSignOut,
                loading = saving,
            )
            TextButton(onClick = { leaving = false }, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.detail_delete_cancel), textAlign = TextAlign.Center)
            }
        } else {
            TextButton(onClick = { leaving = true }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.settings_sign_out),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(heightDp = 900)
@Composable
private fun SettingsPreview() {
    BrainXPTheme {
        SettingsScreen(
            policy =
                SubjectPolicy(
                    level = AcademicLevel.SMA,
                    language = LANGUAGE_ID,
                    questionsPerSession = 10,
                    dayResetHour = 4,
                    idleDaysAllowed = 2,
                    pendingWeakenAt = null,
                    essayCount = PREVIEW_ESSAYS,
                    dailyCapSeconds = List(DAYS_IN_WEEK) { PREVIEW_CAP_SECONDS },
                    dailyGrantSeconds = List(DAYS_IN_WEEK) { 0 },
                    lockedApps = emptyList(),
                ),
            onLevel = {},
            onLanguage = {},
            onApps = {},
            onPermissions = {},
            onSignOut = {},
            onPrivacyPolicy = {},
            onDeleteAccount = {},
            onBack = {},
        )
    }
}

private const val DAYS_IN_WEEK = 7
private const val PREVIEW_ESSAYS = 2
private const val PREVIEW_CAP_SECONDS = 3_600

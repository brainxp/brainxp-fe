package com.example.brainxp.feature.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.example.brainxp.R
import com.example.brainxp.core.time.localMoment
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.ConfirmDialog
import com.example.brainxp.core.ui.MainHeader
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.RowGroup
import com.example.brainxp.core.ui.RowGroupScope
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.core.ui.SegmentedControl
import com.example.brainxp.core.ui.StatusPill
import com.example.brainxp.core.ui.Stepper
import com.example.brainxp.core.ui.StepperEntry
import com.example.brainxp.core.ui.WeekEditor
import com.example.brainxp.core.ui.brainxpSwitchColors
import com.example.brainxp.core.ui.levelLabel
import com.example.brainxp.core.ui.shortDuration
import com.example.brainxp.data.repo.SubjectPolicy
import com.example.brainxp.domain.ProtectionControl
import com.example.brainxp.domain.model.AcademicLevel
import com.example.brainxp.domain.model.PolicyDraft
import com.example.brainxp.domain.model.PolicyLimits
import com.example.brainxp.domain.model.PolicyStep
import com.example.brainxp.domain.model.StepDirection
import com.example.brainxp.domain.model.UploadMethod
import com.example.brainxp.domain.model.canNudge
import com.example.brainxp.feature.home.LockedApp

private const val MAX_DAILY_MINUTES = 1_440

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onEdit: (SettingsPolicyEdit) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onApps: () -> Unit,
    onPermissions: () -> Unit,
    onSignOut: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
    protection: @Composable () -> Unit = {},
) {
    val spacing = BrainXPTheme.spacing
    val draft = state.draft ?: return
    val policy = state.policy ?: return
    var leaving by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal)
                .padding(bottom = spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        MainHeader(title = stringResource(R.string.settings_title))

        state.notice?.let { Note(text = it, alert = true) }
        policy.pendingWeakenAt?.let { at ->
            Note(text = stringResource(R.string.settings_pending, localMoment(at)), alert = true)
        }

        if (state.rulesLocked) {
            Note(text = stringResource(R.string.settings_managed))

            RowGroup {
                item(title = stringResource(R.string.settings_permissions), onClick = onPermissions)
                item(title = stringResource(R.string.settings_privacy), onClick = onPrivacyPolicy)
            }

            return@Column
        }

        Label(stringResource(R.string.settings_level))
        SegmentedControl(
            options = AcademicLevel.entries,
            selected = policy.level ?: AcademicLevel.SMA,
            onSelect = { level -> onEdit(SettingsPolicyEdit.Level(level)) },
            label = { levelLabel(it) },
        )

        Label(stringResource(R.string.settings_language))
        SegmentedControl(
            options = listOf(LANGUAGE_ID, LANGUAGE_EN),
            selected = policy.language.ifBlank { LANGUAGE_ID },
            onSelect = { code -> onEdit(SettingsPolicyEdit.Language(code)) },
            label = { code -> stringResource(if (code == LANGUAGE_EN) R.string.lang_en else R.string.lang_id) },
        )

        SessionSection(draft = draft, locked = false, onEdit = onEdit)
        CapSection(draft = draft, locked = false, onEdit = onEdit)
        GrantSection(draft = draft, locked = false, onEdit = onEdit)
        OtherSection(draft = draft, locked = false, onEdit = onEdit)
        UploadSection(draft = draft, locked = false, onEdit = onEdit)
        LockedAppSection(apps = state.lockedApps, onApps = onApps)

        PrimaryButton(
            text = stringResource(R.string.policy_save),
            onClick = onSave,
            enabled = state.canSave,
            loading = state.saving,
            modifier = Modifier.padding(top = spacing.xs),
        )

        if (state.dirty) {
            TextButton(onClick = onDiscard, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.policy_discard), textAlign = TextAlign.Center)
            }
        }

        RowGroup {
            item(title = stringResource(R.string.settings_permissions), onClick = onPermissions)
        }

        if (state.protection == ProtectionControl.OWNED) {
            protection()
        }

        RowGroup {
            item(title = stringResource(R.string.settings_privacy), onClick = onPrivacyPolicy)
            item(title = stringResource(R.string.settings_delete_account), onClick = onDeleteAccount)
        }

        TextButton(onClick = { leaving = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.settings_sign_out),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
    }

    if (leaving) {
        ConfirmDialog(
            title = stringResource(R.string.settings_sign_out),
            body = stringResource(R.string.settings_sign_out_warning),
            confirm = stringResource(R.string.settings_sign_out_confirm),
            onConfirm = {
                leaving = false
                onSignOut()
            },
            onDismiss = { leaving = false },
        )
    }
}

@Composable
private fun SessionSection(
    draft: PolicyDraft,
    locked: Boolean,
    onEdit: (SettingsPolicyEdit) -> Unit,
) {
    Label(stringResource(R.string.policy_session))
    RowGroup {
        stepperRow(
            title = stringResource(R.string.settings_questions),
            subtitle = stringResource(R.string.policy_questions_sub, PolicyLimits.QUESTIONS_PER_SESSION.last),
            value = draft.questionsPerSession.toString(),
            step = PolicyStep.Questions,
            entry =
                StepperEntry(
                    value = draft.questionsPerSession,
                    range = PolicyLimits.QUESTIONS_PER_SESSION,
                    label = stringResource(R.string.settings_questions),
                    onCommit = { onEdit(draftOf(DraftChange.Questions(it))) },
                ),
            draft = draft,
            locked = locked,
            onEdit = onEdit,
        )
        stepperRow(
            title = stringResource(R.string.settings_essay),
            subtitle =
                stringResource(
                    R.string.policy_essay_sub,
                    draft.essayCount,
                    draft.questionsPerSession,
                    draft.questionsPerSession - draft.essayCount,
                ),
            value = "${essayPercentOf(draft)}%",
            step = PolicyStep.Essays,
            entry =
                StepperEntry(
                    value = essayPercentOf(draft),
                    range = 0..PERCENT,
                    label = stringResource(R.string.settings_essay),
                    onCommit = { onEdit(draftOf(DraftChange.EssayPercent(it))) },
                ),
            draft = draft,
            locked = locked,
            onEdit = onEdit,
        )
        stepperRow(
            title = stringResource(R.string.settings_reward),
            subtitle = stringResource(R.string.policy_reward_sub),
            value = shortDuration(draft.baseRewardSeconds),
            step = PolicyStep.BaseReward,
            entry = null,
            draft = draft,
            locked = locked,
            onEdit = onEdit,
            emphasise = true,
        )
    }
}

@Composable
private fun CapSection(
    draft: PolicyDraft,
    locked: Boolean,
    onEdit: (SettingsPolicyEdit) -> Unit,
) {
    Label(stringResource(R.string.policy_caps))
    WeekEditor(
        minutes = draft.dailyCapSeconds.toMinutes(),
        onChange = { onEdit(draftOf(DraftChange.Caps(it))) },
        maxMinutes = MAX_DAILY_MINUTES,
        enabled = !locked,
    )
}

@Composable
private fun GrantSection(
    draft: PolicyDraft,
    locked: Boolean,
    onEdit: (SettingsPolicyEdit) -> Unit,
) {
    Label(stringResource(R.string.policy_grants))
    Text(
        text = stringResource(R.string.policy_grants_sub),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    WeekEditor(
        minutes = draft.dailyGrantSeconds.toMinutes(),
        onChange = { onEdit(draftOf(DraftChange.Grants(it))) },
        maxMinutes = MAX_DAILY_MINUTES,
        enabled = !locked,
    )
}

@Composable
private fun OtherSection(
    draft: PolicyDraft,
    locked: Boolean,
    onEdit: (SettingsPolicyEdit) -> Unit,
) {
    Label(stringResource(R.string.policy_other))
    RowGroup {
        stepperRow(
            title = stringResource(R.string.settings_idle_allowed),
            subtitle = stringResource(R.string.policy_idle_days_sub),
            value = stringResource(R.string.policy_days, draft.idleDaysAllowed),
            step = PolicyStep.IdleDays,
            entry =
                StepperEntry(
                    value = draft.idleDaysAllowed,
                    range = PolicyLimits.IDLE_DAYS_ALLOWED,
                    label = stringResource(R.string.settings_idle_allowed),
                    onCommit = { onEdit(draftOf(DraftChange.IdleDays(it))) },
                ),
            draft = draft,
            locked = locked,
            onEdit = onEdit,
        )
        stepperRow(
            title = stringResource(R.string.policy_reset_hour),
            subtitle = stringResource(R.string.policy_reset_hour_sub, draft.dayResetHour),
            value = stringResource(R.string.policy_hour, draft.dayResetHour),
            step = PolicyStep.ResetHour,
            entry =
                StepperEntry(
                    value = draft.dayResetHour,
                    range = PolicyLimits.DAY_RESET_HOUR,
                    label = stringResource(R.string.policy_reset_hour),
                    onCommit = { onEdit(draftOf(DraftChange.ResetHour(it))) },
                ),
            draft = draft,
            locked = locked,
            onEdit = onEdit,
        )
    }
}

@Composable
private fun UploadSection(
    draft: PolicyDraft,
    locked: Boolean,
    onEdit: (SettingsPolicyEdit) -> Unit,
) {
    Label(stringResource(R.string.settings_upload))
    RowGroup {
        UploadMethod.entries.forEach { method ->
            val on = method in draft.uploadMethods
            val last = on && draft.uploadMethods.size == 1
            item(
                title = stringResource(uploadLabelOf(method)),
                subtitle = stringResource(R.string.policy_upload_sub),
                onClick =
                    if (locked || last) {
                        null
                    } else {
                        { onEdit(draftOf(DraftChange.Upload(method))) }
                    },
                trailing = {
                    Switch(
                        checked = on,
                        onCheckedChange = { onEdit(draftOf(DraftChange.Upload(method))) },
                        enabled = !locked && !last,
                        colors = brainxpSwitchColors(),
                    )
                },
            )
        }
    }
}

@Composable
private fun LockedAppSection(
    apps: List<LockedApp>,
    onApps: () -> Unit,
) {
    Label(stringResource(R.string.policy_apps))
    RowGroup {
        apps.forEach { app ->
            item(
                title = app.label,
                subtitle = stringResource(R.string.policy_app_locked),
                leading = { AppMark(app) },
                trailing = {
                    StatusPill(text = stringResource(R.string.policy_locked), tone = PillTone.BLUE)
                },
            )
        }
        item(
            title = stringResource(R.string.home_apps_manage),
            subtitle = stringResource(R.string.settings_apps_sub),
            leading = { Icon(imageVector = Lucide.Lock, contentDescription = null) },
            onClick = onApps,
        )
    }
}

@Composable
private fun AppMark(app: LockedApp) {
    val icon = app.icon
    if (icon == null) {
        Icon(imageVector = Lucide.Lock, contentDescription = null)
        return
    }
    Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(APP_ICON))
}

@Composable
private fun RowGroupScope.stepperRow(
    title: String,
    subtitle: String,
    value: String,
    step: PolicyStep,
    entry: StepperEntry?,
    draft: PolicyDraft,
    locked: Boolean,
    onEdit: (SettingsPolicyEdit) -> Unit,
    emphasise: Boolean = false,
) {
    item(
        title = title,
        subtitle = subtitle,
        trailing = {
            Stepper(
                value = value,
                onDecrease = { onEdit(draftOf(DraftChange.Step(step, StepDirection.DOWN))) },
                onIncrease = { onEdit(draftOf(DraftChange.Step(step, StepDirection.UP))) },
                canDecrease = !locked && draft.canNudge(step, StepDirection.DOWN),
                canIncrease = !locked && draft.canNudge(step, StepDirection.UP),
                emphasise = emphasise,
                entry = entry.takeUnless { locked },
            )
        },
    )
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = BrainXPTheme.spacing.sm),
    )
}

private fun draftOf(change: DraftChange): SettingsPolicyEdit = SettingsPolicyEdit.Draft(change)

private fun essayPercentOf(draft: PolicyDraft): Int =
    if (draft.questionsPerSession <= 0) {
        0
    } else {
        (draft.essayCount * PERCENT / draft.questionsPerSession)
    }

private const val PERCENT = 100
private val APP_ICON = 26.dp

private fun uploadLabelOf(method: UploadMethod): Int =
    when (method) {
        UploadMethod.PHOTO -> R.string.settings_upload_photo
        UploadMethod.DOCUMENT -> R.string.settings_upload_document
    }

private fun previewPolicy() =
    SubjectPolicy(
        level = AcademicLevel.SMA,
        language = LANGUAGE_ID,
        questionsPerSession = PREVIEW_DRAFT.questionsPerSession,
        dayResetHour = PREVIEW_DRAFT.dayResetHour,
        idleDaysAllowed = PREVIEW_DRAFT.idleDaysAllowed,
        pendingWeakenAt = null,
        essayCount = PREVIEW_DRAFT.essayCount,
        dailyCapSeconds = PREVIEW_DRAFT.dailyCapSeconds,
        dailyGrantSeconds = PREVIEW_DRAFT.dailyGrantSeconds,
        lockedApps = emptyList(),
        baseRewardSeconds = PREVIEW_DRAFT.baseRewardSeconds,
        uploadMethods = PREVIEW_DRAFT.uploadMethods,
    )

private val PREVIEW_DRAFT =
    PolicyDraft(
        questionsPerSession = 10,
        essayCount = 2,
        baseRewardSeconds = 120,
        dailyCapSeconds = List(PolicyLimits.WEEK_DAYS) { 3_600 },
        dailyGrantSeconds = List(PolicyLimits.WEEK_DAYS) { 0 },
        idleDaysAllowed = 2,
        dayResetHour = 5,
        uploadMethods = UploadMethod.entries.toSet(),
    )

@Preview(heightDp = 1_700)
@Composable
private fun SettingsPreview() {
    BrainXPTheme {
        SettingsScreen(
            state =
                SettingsUiState(
                    loading = false,
                    policy = previewPolicy(),
                    draft = PREVIEW_DRAFT,
                    lockedApps = listOf(LockedApp("com.mobile.legends", "Mobile Legends")),
                ),
            onEdit = {},
            onSave = {},
            onDiscard = {},
            onApps = {},
            onPermissions = {},
            onSignOut = {},
            onPrivacyPolicy = {},
            onDeleteAccount = {},
        )
    }
}

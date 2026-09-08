package com.example.brainxp.feature.family

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.brainxp.R
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.ui.AlertNote
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.RowGroup
import com.example.brainxp.core.ui.RowGroupScope
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.core.ui.StatusPill
import com.example.brainxp.core.ui.Stepper
import com.example.brainxp.core.ui.WeekBars
import com.example.brainxp.core.ui.WeekEditor
import com.example.brainxp.core.ui.apiErrorBody
import com.example.brainxp.core.ui.brainxpSwitchColors
import com.example.brainxp.core.ui.shortDuration
import com.example.brainxp.domain.model.PolicyStep
import com.example.brainxp.domain.model.StepDirection
import com.example.brainxp.domain.model.UploadMethod

@Composable
fun PolicyEditorScreen(
    state: PolicyUiState,
    onEvent: (PolicyEvent) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    setup: Boolean = false,
    saving: Boolean = false,
    saved: Boolean = false,
    notice: String? = null,
    failure: ApiError? = null,
) {
    val spacing = BrainXPTheme.spacing

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
        ScreenNav(
            title = stringResource(R.string.policy_title, state.subjectName),
            onBack = onBack,
            trailing = {
                if (setup) {
                    StatusPill(text = stringResource(R.string.policy_step), tone = PillTone.OUTLINE)
                }
            },
        )

        SectionLabel(stringResource(R.string.policy_session))
        RowGroup {
            stepRow(
                title = stringResource(R.string.policy_questions),
                subtitle = stringResource(R.string.policy_questions_sub, MAX_QUESTIONS_PER_SESSION),
                value = state.questionsPerSession.toString(),
                step = PolicyStep.Questions,
                state = state,
                onEvent = onEvent,
            )
            stepRow(
                title = stringResource(R.string.policy_essay),
                subtitle =
                    stringResource(
                        R.string.policy_essay_sub,
                        state.essayCount,
                        state.questionsPerSession,
                        state.mcqCount,
                    ),
                value = "${state.essayPercent}%",
                step = PolicyStep.Essays,
                state = state,
                onEvent = onEvent,
            )
            stepRow(
                title = stringResource(R.string.policy_reward),
                subtitle = stringResource(R.string.policy_reward_sub),
                value = shortDuration(state.baseRewardSeconds),
                step = PolicyStep.BaseReward,
                state = state,
                onEvent = onEvent,
                emphasise = true,
            )
        }

        SectionLabel(stringResource(R.string.policy_caps))
        WeekEditor(
            minutes = state.dailyCapMinutes,
            onChange = { onEvent(PolicyEvent.SetCaps(it)) },
            maxMinutes = MAX_DAILY_MINUTES,
        )

        SectionLabel(stringResource(R.string.policy_grants))
        Text(
            text = stringResource(R.string.policy_grants_sub),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        WeekEditor(
            minutes = state.dailyGrantMinutes,
            onChange = { onEvent(PolicyEvent.SetGrants(it)) },
            maxMinutes = MAX_DAILY_MINUTES,
        )

        SectionLabel(stringResource(R.string.policy_other))
        RowGroup {
            stepRow(
                title = stringResource(R.string.policy_idle_days),
                subtitle = stringResource(R.string.policy_idle_days_sub),
                value = stringResource(R.string.policy_days, state.idleDaysAllowed),
                step = PolicyStep.IdleDays,
                state = state,
                onEvent = onEvent,
            )
            stepRow(
                title = stringResource(R.string.policy_reset_hour),
                subtitle = stringResource(R.string.policy_reset_hour_sub, state.dayResetHour),
                value = stringResource(R.string.policy_hour, state.dayResetHour),
                step = PolicyStep.ResetHour,
                state = state,
                onEvent = onEvent,
            )
        }

        SectionLabel(stringResource(R.string.policy_upload))
        RowGroup {
            UploadMethod.entries.forEach { method ->
                val on = method in state.uploadMethods
                item(
                    title = stringResource(uploadLabelOf(method)),
                    subtitle = stringResource(uploadHintOf(method)),
                    trailing = {
                        Switch(
                            checked = on,
                            onCheckedChange = { onEvent(PolicyEvent.ToggleUpload(method)) },
                            colors = brainxpSwitchColors(),
                        )
                    },
                )
            }
        }
        Text(
            text = stringResource(R.string.policy_upload_sub),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionLabel(stringResource(R.string.policy_apps))
        if (state.apps.isEmpty()) {
            Note(text = stringResource(R.string.policy_apps_waiting))
        } else {
            RowGroup {
                state.apps.forEach { app ->
                    item(
                        title = app.label,
                        subtitle =
                            stringResource(
                                if (app.locked) R.string.policy_app_locked else R.string.policy_app_free,
                            ),
                        onClick = { onEvent(PolicyEvent.ToggleApp(app.packageName)) },
                        trailing = {
                            StatusPill(
                                text =
                                    stringResource(
                                        if (app.locked) R.string.policy_locked else R.string.policy_free,
                                    ),
                                tone = if (app.locked) PillTone.BLUE else PillTone.NEUTRAL,
                            )
                        },
                    )
                }
            }
            Text(
                text = stringResource(R.string.policy_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when {
            failure != null -> {
                AlertNote(
                    title = stringResource(R.string.policy_save_failed),
                    body = apiErrorBody(failure),
                )
            }

            notice != null -> {
                Note(text = notice, alert = true)
            }

            saved -> {
                Note(text = stringResource(R.string.policy_saved))
            }
        }

        PrimaryButton(
            text = stringResource(if (setup) R.string.policy_issue_code else R.string.policy_save),
            onClick = { onEvent(PolicyEvent.Save) },
            loading = saving,
            modifier = Modifier.padding(top = spacing.xs),
        )
    }
}

@Composable
private fun RowGroupScope.stepRow(
    title: String,
    subtitle: String,
    value: String,
    step: PolicyStep,
    state: PolicyUiState,
    onEvent: (PolicyEvent) -> Unit,
    emphasise: Boolean = false,
) {
    item(
        title = title,
        subtitle = subtitle,
        trailing = {
            Stepper(
                value = value,
                onDecrease = { onEvent(PolicyEvent.Nudge(step, StepDirection.DOWN)) },
                onIncrease = { onEvent(PolicyEvent.Nudge(step, StepDirection.UP)) },
                canDecrease = state.canNudge(step, StepDirection.DOWN),
                canIncrease = state.canNudge(step, StepDirection.UP),
                emphasise = emphasise,
            )
        },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = BrainXPTheme.spacing.sm),
    )
}

private const val MAX_DAILY_MINUTES = 180

@Preview(name = "PolicyEditor", showBackground = true, heightDp = 1500)
@Composable
private fun PolicyEditorPreview() {
    BrainXPTheme { PolicyEditorScreen(state = SAMPLE_POLICY, onEvent = {}, onBack = {}) }
}

private fun uploadLabelOf(method: UploadMethod): Int =
    when (method) {
        UploadMethod.PHOTO -> R.string.settings_upload_photo
        UploadMethod.DOCUMENT -> R.string.settings_upload_document
    }

private fun uploadHintOf(method: UploadMethod): Int =
    when (method) {
        UploadMethod.PHOTO -> R.string.settings_upload_photo_sub
        UploadMethod.DOCUMENT -> R.string.settings_upload_document_sub
    }

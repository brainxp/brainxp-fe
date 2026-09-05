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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.RowGroup
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.core.ui.StatusPill
import com.example.brainxp.core.ui.WeekBars

@Composable
fun PolicyEditorScreen(
    state: PolicyUiState,
    onEvent: (PolicyEvent) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
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
        )

        SectionLabel(stringResource(R.string.policy_session))
        RowGroup {
            item(
                title = stringResource(R.string.policy_questions),
                subtitle =
                    stringResource(R.string.policy_questions_sub, MAX_QUESTIONS_PER_SESSION),
                value = state.questionsPerSession.toString(),
                emphasiseValue = true,
                onClick = { onEvent(PolicyEvent.StepQuestions) },
            )
            item(
                title = stringResource(R.string.policy_essay),
                subtitle =
                    stringResource(
                        R.string.policy_essay_sub,
                        state.essayCount,
                        state.questionsPerSession,
                        state.mcqCount,
                    ),
                value = "${state.essayPercent}%",
                emphasiseValue = true,
                onClick = { onEvent(PolicyEvent.StepEssay) },
            )
        }

        SectionLabel(stringResource(R.string.policy_caps))
        WeekBars(
            values = state.dailyCapMinutes,
            maxValue = MAX_DAILY_MINUTES,
            label = { stringResource(R.string.policy_minutes_short, it) },
            onStep = { onEvent(PolicyEvent.StepCap(it)) },
        )

        SectionLabel(stringResource(R.string.policy_grants))
        Text(
            text = stringResource(R.string.policy_grants_sub),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        WeekBars(
            values = state.dailyGrantMinutes,
            maxValue = MAX_DAILY_MINUTES,
            label = { stringResource(R.string.policy_minutes_short, it) },
            onStep = { onEvent(PolicyEvent.StepGrant(it)) },
        )

        SectionLabel(stringResource(R.string.policy_other))
        RowGroup {
            item(
                title = stringResource(R.string.policy_idle_days),
                subtitle = stringResource(R.string.policy_idle_days_sub),
                value = stringResource(R.string.policy_days, state.idleDaysAllowed),
                emphasiseValue = true,
                onClick = { onEvent(PolicyEvent.StepIdleDays) },
            )
            item(
                title = stringResource(R.string.policy_reset_hour),
                subtitle = stringResource(R.string.policy_reset_hour_sub, state.dayResetHour),
                value = stringResource(R.string.policy_hour, state.dayResetHour),
                emphasiseValue = true,
                onClick = { onEvent(PolicyEvent.StepResetHour) },
            )
        }

        SectionLabel(stringResource(R.string.policy_apps))
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
        PrimaryButton(
            text = stringResource(R.string.policy_save),
            onClick = { onEvent(PolicyEvent.Save) },
            modifier = Modifier.padding(top = spacing.xs),
        )
    }
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

package com.example.brainxp.feature.family

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.Field
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.core.ui.SegmentedControl
import com.example.brainxp.core.ui.StatusPill
import com.example.brainxp.core.ui.levelLabel
import com.example.brainxp.domain.model.AcademicLevel

@Composable
fun NewChildScreen(
    onCreated: (String, AcademicLevel, QuestionLanguage) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val spacing = BrainXPTheme.spacing
    var name by rememberSaveable { mutableStateOf("") }
    var level by rememberSaveable { mutableStateOf(AcademicLevel.SMP) }
    var language by rememberSaveable { mutableStateOf(QuestionLanguage.ID) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = spacing.screenHorizontal)
                .padding(bottom = spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        ScreenNav(title = stringResource(R.string.new_child_title), onBack = onBack) {
            StatusPill(text = stringResource(R.string.new_child_step, 1), tone = PillTone.OUTLINE)
        }

        Text(
            text = stringResource(R.string.new_child_headline),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.new_child_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.size(spacing.sm))

        Field(
            label = stringResource(R.string.new_child_name),
            value = name,
            onValueChange = { name = it },
            placeholder = stringResource(R.string.new_child_name_hint),
        )

        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text(
                text = stringResource(R.string.new_child_level),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SegmentedControl(
                options = AcademicLevel.entries,
                selected = level,
                onSelect = { level = it },
                label = { levelLabel(it) },
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text(
                text = stringResource(R.string.new_child_language),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SegmentedControl(
                options = QuestionLanguage.entries,
                selected = language,
                onSelect = { language = it },
                label = { languageLabel(it) },
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        Note(text = stringResource(R.string.new_child_note))
        PrimaryButton(
            text = stringResource(R.string.new_child_next),
            onClick = { onCreated(name.trim(), level, language) },
            enabled = name.isNotBlank(),
        )
    }
}

@Composable
private fun languageLabel(language: QuestionLanguage): String =
    stringResource(
        when (language) {
            QuestionLanguage.ID -> R.string.lang_id
            QuestionLanguage.EN -> R.string.lang_en
        },
    )

@Preview(name = "NewChild", showBackground = true, heightDp = 900)
@Composable
private fun NewChildPreview() {
    BrainXPTheme { NewChildScreen(onCreated = { _, _, _ -> }, onBack = {}) }
}

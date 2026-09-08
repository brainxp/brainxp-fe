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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.brainxp.R
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.ui.AlertNote
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.ErrorState
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.core.ui.SegmentedControl
import com.example.brainxp.core.ui.levelLabel
import com.example.brainxp.domain.model.AcademicLevel

@Composable
fun LevelScreen(
    onSubmit: (AcademicLevel) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    busy: Boolean = false,
    error: ApiError? = null,
) {
    val spacing = BrainXPTheme.spacing
    var level by rememberSaveable { mutableStateOf(AcademicLevel.SMA) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = spacing.screenHorizontal)
                .padding(bottom = spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        ScreenNav(title = stringResource(R.string.level_title), onBack = onBack)

        Text(
            text = stringResource(R.string.level_heading),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.level_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.size(spacing.lg))

        SegmentedControl(
            options = AcademicLevel.entries,
            selected = level,
            onSelect = { level = it },
            label = { levelLabel(it) },
        )

        if (error != null) {
            Spacer(modifier = Modifier.size(spacing.lg))
            if (error == ApiError.Unauthorized) {
                AlertNote(
                    title = stringResource(R.string.level_error_session_title),
                    body = stringResource(R.string.level_error_session_body),
                )
            } else {
                ErrorState(error = error)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Note(text = stringResource(R.string.level_note))

        PrimaryButton(
            text = stringResource(R.string.level_submit),
            onClick = { onSubmit(level) },
            loading = busy,
            enabled = !busy,
        )
    }
}

@Preview
@Composable
private fun LevelPreview() {
    BrainXPTheme { LevelScreen(onSubmit = {}, onBack = {}) }
}

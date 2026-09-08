package com.example.brainxp.feature.capture

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Lucide
import com.example.brainxp.R
import com.example.brainxp.core.capture.DOCUMENT_MIME_TYPES
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.ChoiceRow
import com.example.brainxp.core.ui.HeroCard
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.core.ui.shortDuration
import com.example.brainxp.domain.model.UploadMethod

@Composable
fun PickSourceScreen(
    questionCount: Int,
    estimatedRewardSeconds: Int,
    onPick: (CaptureMethod) -> Unit,
    onPicked: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    rejection: String? = null,
    methods: Set<UploadMethod> = UploadMethod.entries.toSet(),
) {
    val spacing = BrainXPTheme.spacing

    val pickDocument =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri -> uri?.let(onPicked) }

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
        ScreenNav(title = stringResource(R.string.source_title), onBack = onBack)

        HeroCard(
            label = stringResource(R.string.source_hero_label),
            value = questionCount.toString(),
            unit = stringResource(R.string.source_unit),
            footer = {
                Text(
                    text =
                        stringResource(
                            R.string.source_hero_sub,
                            shortDuration(estimatedRewardSeconds),
                        ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            },
        )

        Text(
            text = stringResource(R.string.source_pick),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = spacing.sm),
        )

        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            if (UploadMethod.PHOTO in methods) {
                ChoiceRow(
                    title = stringResource(R.string.source_photo),
                    subtitle = stringResource(R.string.source_photo_sub),
                    icon = Lucide.Camera,
                    highlight = true,
                    onClick = { onPick(CaptureMethod.PHOTO) },
                )
            }
            if (UploadMethod.DOCUMENT in methods) {
                ChoiceRow(
                    title = stringResource(R.string.source_document),
                    subtitle = stringResource(R.string.source_document_sub),
                    icon = Lucide.FileText,
                    highlight = UploadMethod.PHOTO !in methods,
                    onClick = { pickDocument.launch(DOCUMENT_MIME_TYPES) },
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        rejection?.let { Note(text = it, alert = true) }
        Note(text = stringResource(R.string.source_note))
        Spacer(modifier = Modifier.size(spacing.xs))
    }
}

@Preview(name = "PickSource", showBackground = true, heightDp = 860)
@Composable
private fun PickSourcePreview() {
    BrainXPTheme {
        PickSourceScreen(
            questionCount = 6,
            estimatedRewardSeconds = 1_140,
            onPick = {},
            onPicked = {},
            onBack = {},
        )
    }
}

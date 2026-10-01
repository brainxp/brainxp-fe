package com.example.brainxp.feature.capture

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.ArrowRight
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Lucide
import com.example.brainxp.R
import com.example.brainxp.core.capture.DOCUMENT_MIME_TYPES
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.ChoiceRow
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.domain.model.UploadMethod

@Composable
fun PickSourceScreen(
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
    val documentAction = { pickDocument.launch(DOCUMENT_MIME_TYPES) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal)
                .padding(bottom = spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        ScreenNav(title = stringResource(R.string.source_title), onBack = onBack)

        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text(
                text = stringResource(R.string.source_heading),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.source_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        rejection?.let { Note(text = it, alert = true) }

        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            when {
                UploadMethod.PHOTO in methods -> {
                    PrimarySourceCard(
                        title = stringResource(R.string.source_photo),
                        subtitle = stringResource(R.string.source_photo_sub),
                        action = stringResource(R.string.source_photo_action),
                        icon = Lucide.Camera,
                        onClick = { onPick(CaptureMethod.PHOTO) },
                    )
                }

                UploadMethod.DOCUMENT in methods -> {
                    PrimarySourceCard(
                        title = stringResource(R.string.source_document),
                        subtitle = stringResource(R.string.source_document_sub),
                        action = stringResource(R.string.source_document_action),
                        icon = Lucide.FileText,
                        onClick = documentAction,
                    )
                }
            }

            if (UploadMethod.PHOTO in methods && UploadMethod.DOCUMENT in methods) {
                ChoiceRow(
                    title = stringResource(R.string.source_document),
                    subtitle = stringResource(R.string.source_document_sub),
                    icon = Lucide.FileText,
                    onClick = documentAction,
                )
            }
        }

        Note(text = stringResource(R.string.source_note))
    }
}

@Composable
private fun PrimarySourceCard(
    title: String,
    subtitle: String,
    action: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClickLabel = action, onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        color = scheme.primary,
    ) {
        Column(
            modifier = Modifier.padding(spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.xl),
        ) {
            Surface(
                modifier = Modifier.size(spacing.xxxl + spacing.lg),
                shape = MaterialTheme.shapes.medium,
                color = scheme.onPrimary.copy(alpha = ICON_TILE_ALPHA),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(spacing.xxl),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = scheme.onPrimary,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onPrimary.copy(alpha = SUPPORTING_ALPHA),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onPrimary,
                )
                Icon(
                    imageVector = Lucide.ArrowRight,
                    contentDescription = null,
                    tint = scheme.onPrimary,
                    modifier = Modifier.size(spacing.xl),
                )
            }
        }
    }
}

private const val ICON_TILE_ALPHA = 0.14f
private const val SUPPORTING_ALPHA = 0.78f

@Preview(name = "Pilih materi", showBackground = true, heightDp = 820)
@Composable
private fun PickSourcePreview() {
    BrainXPTheme {
        PickSourceScreen(
            onPick = {},
            onPicked = {},
            onBack = {},
        )
    }
}

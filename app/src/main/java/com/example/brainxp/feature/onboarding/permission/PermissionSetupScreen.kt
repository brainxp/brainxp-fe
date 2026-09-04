package com.example.brainxp.feature.onboarding.permission

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.brainxp.R
import com.example.brainxp.core.permission.PermissionEntry
import com.example.brainxp.core.permission.PermissionRequirement
import com.example.brainxp.core.permission.PermissionSnapshot
import com.example.brainxp.core.permission.SpecialPermission
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.PrimaryButton

@Composable
fun PermissionSetupScreen(
    state: PermissionSetupUiState,
    onOpenSettings: (SpecialPermission) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    deviceMissingSettings: Boolean = false,
    reentrant: Boolean = false,
) {
    val spacing = BrainXPTheme.spacing

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = spacing.screenHorizontal,
                    end = spacing.screenHorizontal,
                    top = spacing.xxl,
                    bottom = spacing.screenBottom,
                ),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text(
            text = stringResource(R.string.permission_setup_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.permission_setup_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        state.steps.forEach { step ->
            PermissionStepCard(
                step = step,
                total = state.steps.size,
                onOpenSettings = { onOpenSettings(step.permission) },
                modifier = Modifier.padding(top = spacing.xs),
            )
        }

        Advisory(
            text =
                stringResource(
                    if (state.canContinue) R.string.permission_setup_degraded else R.string.permission_setup_blocked,
                ),
            alert = state.canContinue,
            modifier = Modifier.padding(top = spacing.sm),
            visible = !state.canContinue || state.degraded,
        )

        Advisory(
            text = stringResource(R.string.permission_setup_unavailable),
            alert = true,
            modifier = Modifier.padding(top = spacing.sm),
            visible = deviceMissingSettings,
        )

        PrimaryButton(
            text =
                stringResource(
                    if (reentrant) R.string.permission_setup_finish else R.string.permission_setup_continue,
                ),
            onClick = onContinue,
            enabled = state.canContinue,
            modifier = Modifier.padding(top = spacing.xxxl),
        )
    }
}

@Composable
private fun PermissionStepCard(
    step: PermissionStepUi,
    total: Int,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing
    val emphasised = step.isCurrent && !step.granted

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border =
            BorderStroke(
                width = if (emphasised) EMPHASIS_BORDER else HAIRLINE,
                color = if (emphasised) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            ),
    ) {
        Column(
            modifier = Modifier.padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.permission_setup_step_label, step.position, total),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RequirementBadge(step)
            }

            Text(
                text = stringResource(titleRes(step.permission)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(breaksRes(step.permission)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!step.granted) {
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth().padding(top = spacing.xs),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        text = stringResource(R.string.permission_setup_open),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun RequirementBadge(
    step: PermissionStepUi,
    modifier: Modifier = Modifier,
) {
    val label =
        when {
            step.granted -> R.string.permission_setup_granted
            step.requirement == PermissionRequirement.OPTIONAL -> R.string.permission_setup_optional
            step.requirement == PermissionRequirement.RECOMMENDED -> R.string.permission_setup_recommended
            else -> null
        } ?: return

    val spacing = BrainXPTheme.spacing
    val container =
        if (step.granted) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
    val ink =
        if (step.granted) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Surface(modifier = modifier, shape = MaterialTheme.shapes.extraLarge, color = container) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelSmall,
            color = ink,
            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
        )
    }
}

@Composable
private fun Advisory(
    text: String,
    alert: Boolean,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!visible) {
        return
    }
    val spacing = BrainXPTheme.spacing
    val container =
        if (alert) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val ink =
        if (alert) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer

    Surface(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = container) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = ink,
            modifier = Modifier.padding(spacing.md),
        )
    }
}

private val HAIRLINE = 1.dp
private val EMPHASIS_BORDER = 1.5.dp

private fun sampleState(granted: Set<SpecialPermission>): PermissionSetupUiState =
    PermissionSnapshot(SpecialPermission.entries.map { PermissionEntry(it, it in granted) }).toSetupState()

@Preview(name = "Permission setup fresh", showBackground = true)
@Composable
private fun PermissionSetupFreshPreview() {
    BrainXPTheme {
        PermissionSetupScreen(
            state = sampleState(emptySet()),
            onOpenSettings = {},
            onContinue = {},
        )
    }
}

@Preview(name = "Permission setup degraded", showBackground = true, heightDp = 1100)
@Composable
private fun PermissionSetupDegradedPreview() {
    BrainXPTheme {
        PermissionSetupScreen(
            state = sampleState(setOf(SpecialPermission.USAGE_ACCESS, SpecialPermission.OVERLAY)),
            onOpenSettings = {},
            onContinue = {},
        )
    }
}

@Preview(name = "Permission setup complete", showBackground = true, heightDp = 1100)
@Composable
private fun PermissionSetupCompletePreview() {
    BrainXPTheme {
        PermissionSetupScreen(
            state = sampleState(SpecialPermission.entries.toSet()),
            onOpenSettings = {},
            onContinue = {},
        )
    }
}

@Preview(name = "Permission setup dark", showBackground = true, heightDp = 1100)
@Composable
private fun PermissionSetupDarkPreview() {
    BrainXPTheme(darkTheme = true) {
        PermissionSetupScreen(
            state = sampleState(setOf(SpecialPermission.NOTIFICATIONS)),
            onOpenSettings = {},
            onContinue = {},
        )
    }
}

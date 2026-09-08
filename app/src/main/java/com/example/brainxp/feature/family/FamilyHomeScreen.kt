package com.example.brainxp.feature.family

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.LogOut
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.User
import com.example.brainxp.R
import com.example.brainxp.blocking.bodyOf
import com.example.brainxp.core.ui.AlertNote
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.ChoiceRow
import com.example.brainxp.core.ui.ConfirmDialog
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.RowGroup
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.core.ui.StatusPill
import com.example.brainxp.core.ui.levelLabel
import com.example.brainxp.core.ui.shortDuration
import com.example.brainxp.domain.model.AcademicLevel
import com.example.brainxp.domain.model.GuardianAlert

private const val BULLET = "• "

private fun GuardianAlert.wording(
    context: Context,
    name: String,
): String = detail?.takeIf { it.isNotBlank() } ?: context.getString(bodyOf(kind), name)

@Composable
fun FamilyHomeScreen(
    state: FamilyHomeUiState,
    onOpenChild: (String) -> Unit,
    onNewChild: () -> Unit,
    onSelfRules: () -> Unit,
    onJoinRules: () -> Unit,
    modifier: Modifier = Modifier,
    onSignOut: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
) {
    val spacing = BrainXPTheme.spacing
    var asking by remember { mutableStateOf(false) }

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
        ScreenNav(title = stringResource(R.string.family_title), onBack = onBack) {
            StatusPill(text = stringResource(R.string.family_role), tone = PillTone.BLUE)
            if (onSignOut != null) {
                IconButton(onClick = { asking = true }, modifier = Modifier.size(NAV_TAP)) {
                    Icon(
                        imageVector = Lucide.LogOut,
                        contentDescription = stringResource(R.string.settings_sign_out),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        val context = LocalContext.current
        state.alerts.groupBy { alert -> alert.subjectName }.forEach { (name, raised) ->
            AlertNote(
                title = stringResource(R.string.alert_title_named, name),
                body =
                    raised.joinToString(separator = "\n") { alert ->
                        BULLET + alert.wording(context, name)
                    },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Text(
            text = stringResource(R.string.family_children),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.children.isEmpty()) {
            Note(text = stringResource(R.string.family_empty))
        } else {
            RowGroup {
                state.children.forEach { child ->
                    item(
                        title = "${child.name} · ${levelLabel(child.level)}",
                        subtitle =
                            stringResource(
                                R.string.family_child_sub,
                                child.streakDays,
                                shortDuration(child.remainingCapSeconds),
                            ),
                        value = shortDuration(child.balanceSeconds),
                        emphasiseValue = child.balanceSeconds > 0,
                        onClick = { onOpenChild(child.id) },
                    )
                }
            }
        }

        ChoiceRow(
            title = stringResource(R.string.family_add_child),
            subtitle = stringResource(R.string.family_add_child_sub),
            icon = Lucide.Plus,
            highlight = true,
            onClick = onNewChild,
        )

        Text(
            text = stringResource(R.string.family_self),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = spacing.sm),
        )

        val self = state.self
        if (self == null) {
            Note(text = stringResource(R.string.family_self_invite))
            OutlinedButton(
                onClick = onJoinRules,
                modifier = Modifier.padding(top = spacing.xs),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = stringResource(R.string.family_self_join),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        } else {
            RowGroup {
                item(
                    title = self.name,
                    subtitle = stringResource(R.string.family_self_sub),
                    value = shortDuration(self.balanceSeconds),
                    emphasiseValue = self.balanceSeconds > 0,
                    leading = { RowIcon(Lucide.User) },
                    onClick = { onOpenChild(self.id) },
                )
                item(
                    title = stringResource(R.string.family_self_rules),
                    subtitle = stringResource(R.string.family_self_rules_sub),
                    leading = { RowIcon(Lucide.Lock) },
                    onClick = onSelfRules,
                )
            }
        }

        Spacer(modifier = Modifier.padding(spacing.xs))
    }

    if (asking && onSignOut != null) {
        ConfirmDialog(
            title = stringResource(R.string.settings_sign_out),
            body = stringResource(R.string.settings_sign_out_warning),
            confirm = stringResource(R.string.settings_sign_out_confirm),
            onConfirm = {
                asking = false
                onSignOut()
            },
            onDismiss = { asking = false },
        )
    }
}

@Composable
internal fun RowIcon(vector: ImageVector) {
    Icon(
        imageVector = vector,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(name = "FamilyHome", showBackground = true, heightDp = 900)
@Composable
private fun FamilyHomePreview() {
    BrainXPTheme {
        FamilyHomeScreen(
            state = SAMPLE_FAMILY,
            onOpenChild = {},
            onNewChild = {},
            onSelfRules = {},
            onJoinRules = {},
            onBack = {},
        )
    }
}

@Preview(name = "FamilyHome empty", showBackground = true, heightDp = 700)
@Composable
private fun FamilyHomeEmptyPreview() {
    BrainXPTheme {
        FamilyHomeScreen(
            state = FamilyHomeUiState(),
            onOpenChild = {},
            onNewChild = {},
            onSelfRules = {},
            onJoinRules = {},
            onBack = {},
        )
    }
}

private val NAV_TAP = 40.dp

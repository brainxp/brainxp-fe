package com.example.brainxp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Library
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Settings
import com.example.brainxp.core.ui.BrainXPTheme

enum class BottomTab(
    val route: MainRoute,
    val icon: ImageVector,
    val label: Int,
    val spoken: Int,
) {
    HOME(MainRoute.Home, Lucide.House, R.string.tab_home, R.string.home_title),
    HISTORY(MainRoute.History, Lucide.Clock, R.string.tab_history, R.string.history_title),
    LIBRARY(MainRoute.MaterialList, Lucide.Library, R.string.tab_library, R.string.library_title),
    SETTINGS(MainRoute.Settings, Lucide.Settings, R.string.tab_settings, R.string.settings_title),
}

fun NavKey.showsBottomBar(): Boolean =
    when (this) {
        is MainRoute.Capture,
        is MainRoute.PermissionSetup,
        is MainRoute.CameraCapture,
        is MainRoute.Preparing,
        is MainRoute.Rejected,
        is MainRoute.Questions,
        is MainRoute.Results,
        is MainRoute.FamilyHome,
        is MainRoute.FamilyChild,
        is MainRoute.FamilyChildPolicy,
        is MainRoute.FamilyPairing,
        is MainRoute.FamilyNewChild,
        is MainRoute.FamilyBalance,
        -> false

        is MainRoute -> true

        else -> false
    }

fun NavKey.risesFromBottom(): Boolean =
    when (this) {
        is MainRoute.Capture,
        is MainRoute.CameraCapture,
        is MainRoute.Preparing,
        is MainRoute.Rejected,
        is MainRoute.Questions,
        is MainRoute.Results,
        -> true

        else -> false
    }

fun NavKey.selectedTab(): BottomTab? = BottomTab.entries.firstOrNull { it.route == this }

@Composable
fun MainBottomBar(
    selected: BottomTab?,
    onSelect: (BottomTab) -> Unit,
    onAddMaterial: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(thickness = HAIRLINE, color = MaterialTheme.colorScheme.outline)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = BrainXPTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BottomTab.entries.take(CENTRE_AT).forEach { tab ->
                    TabItem(
                        tab = tab,
                        active = tab == selected,
                        onClick = { onSelect(tab) },
                        modifier = Modifier.weight(1f),
                    )
                }
                AddMaterialButton(onClick = onAddMaterial, modifier = Modifier.weight(1f))
                BottomTab.entries.drop(CENTRE_AT).forEach { tab ->
                    TabItem(
                        tab = tab,
                        active = tab == selected,
                        onClick = { onSelect(tab) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: BottomTab,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink =
        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier =
            modifier
                .selectable(selected = active, role = Role.Tab, onClick = onClick)
                .padding(vertical = BrainXPTheme.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.xs),
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = stringResource(tab.spoken),
            tint = ink,
            modifier = Modifier.size(GLYPH),
        )
        Text(
            text = stringResource(tab.label),
            style = MaterialTheme.typography.labelSmall,
            color = ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun AddMaterialButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.size(FAB),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = LIFT,
            onClick = onClick,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Lucide.Plus,
                    contentDescription = stringResource(R.string.home_start_earning),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(FAB_GLYPH),
                )
            }
        }
    }
}

private const val CENTRE_AT = 2
private val HAIRLINE = 1.dp
private val GLYPH = 21.dp
private val FAB = 46.dp
private val FAB_GLYPH = 24.dp
private val LIFT = 6.dp

@Preview(showBackground = true)
@Composable
private fun MainBottomBarPreview() {
    BrainXPTheme {
        MainBottomBar(selected = BottomTab.HOME, onSelect = {}, onAddMaterial = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun MainBottomBarDarkPreview() {
    BrainXPTheme(darkTheme = true) {
        MainBottomBar(selected = BottomTab.LIBRARY, onSelect = {}, onAddMaterial = {})
    }
}

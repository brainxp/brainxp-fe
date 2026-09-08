package com.example.brainxp.feature.family

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.composables.icons.lucide.Delete
import com.composables.icons.lucide.Lucide
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTextStyles
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.ScreenNav

const val PAIRING_CODE_LENGTH = 6

@Composable
fun PairDeviceScreen(
    digits: String,
    onKey: (Char) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    error: String? = null,
    busy: Boolean = false,
) {
    val spacing = BrainXPTheme.spacing

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = spacing.xl)
                .padding(top = spacing.sm, bottom = spacing.screenBottom),
    ) {
        ScreenNav(title = stringResource(R.string.pair_nav), onBack = onBack)
        Text(
            text = stringResource(R.string.pair_title),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.size(spacing.sm))
        Text(
            text = stringResource(R.string.pair_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.size(spacing.xl))

        CodeBoxes(digits = digits)
        Spacer(modifier = Modifier.size(spacing.lg))

        if (error != null) {
            Note(text = error, alert = true)
            Spacer(modifier = Modifier.size(spacing.sm))
        }
        Note(text = stringResource(R.string.pair_note))

        Spacer(modifier = Modifier.weight(1f))
        Keypad(onKey = onKey, onDelete = onDelete, enabled = !busy)
    }
}

@Composable
private fun CodeBoxes(
    digits: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.sm),
    ) {
        repeat(PAIRING_CODE_LENGTH) { index ->
            val filled = index < digits.length
            val cursor = index == digits.length

            Surface(
                modifier = Modifier.weight(1f).height(BOX),
                shape = MaterialTheme.shapes.medium,
                color = if (filled) scheme.primaryContainer else scheme.surface,
                border =
                    BorderStroke(
                        if (filled || cursor) THICK else HAIRLINE,
                        if (filled || cursor) scheme.primary else scheme.outline,
                    ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (filled) digits[index].toString() else "",
                        style = MaterialTheme.typography.headlineMedium,
                        color = scheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun Keypad(
    onKey: (Char) -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KEY_GAP),
    ) {
        listOf("123", "456", "789").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(KEY_GAP)) {
                row.forEach { digit ->
                    Key(
                        label = digit.toString(),
                        enabled = enabled,
                        onClick = { onKey(digit) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(KEY_GAP)) {
            Spacer(modifier = Modifier.weight(1f))
            Key(
                label = "0",
                enabled = enabled,
                onClick = { onKey('0') },
                modifier = Modifier.weight(1f),
            )
            Key(
                label = null,
                enabled = enabled,
                onClick = onDelete,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.size(spacing.xs))
    }
}

@Composable
private fun Key(
    label: String?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(KEY),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        border = BorderStroke(HAIRLINE, MaterialTheme.colorScheme.outline),
    ) {
        if (label == null) {
            Icon(
                imageVector = Lucide.Delete,
                contentDescription = stringResource(R.string.pair_delete),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(GLYPH),
            )
        } else {
            Text(
                text = label,
                style = BrainXPTextStyles.numericSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private val BOX = 60.dp
private val KEY = 62.dp
private val KEY_GAP = 9.dp
private val GLYPH = 20.dp
private val HAIRLINE = 1.dp
private val THICK = 1.5.dp

@Preview(name = "PairDevice empty", showBackground = true, heightDp = 860)
@Composable
private fun PairDevicePreview() {
    BrainXPTheme {
        PairDeviceScreen(digits = "", onKey = {}, onDelete = {}, onBack = {})
    }
}

@Preview(name = "PairDevice partial", showBackground = true, heightDp = 860)
@Composable
private fun PairDevicePartialPreview() {
    BrainXPTheme {
        PairDeviceScreen(digits = "418", onKey = {}, onDelete = {}, onBack = {})
    }
}

@Preview(name = "PairDevice rejected", showBackground = true, heightDp = 860)
@Composable
private fun PairDeviceErrorPreview() {
    BrainXPTheme {
        PairDeviceScreen(
            digits = "",
            onKey = {},
            onDelete = {},
            onBack = {},
            error = "Kode itu sudah kedaluwarsa. Minta yang baru.",
        )
    }
}

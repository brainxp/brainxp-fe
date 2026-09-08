package com.example.brainxp.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Minus
import com.composables.icons.lucide.Plus
import com.example.brainxp.R

@Composable
fun Stepper(
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    canDecrease: Boolean = true,
    canIncrease: Boolean = true,
    emphasise: Boolean = false,
    entry: StepperEntry? = null,
) {
    var typing by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = PillShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(HAIRLINE, MaterialTheme.colorScheme.outline),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton(
                icon = Lucide.Minus,
                description = stringResource(R.string.stepper_decrease),
                enabled = canDecrease && !typing,
                onClick = onDecrease,
            )
            if (typing && entry != null) {
                Readout(entry = entry, onSettled = { typing = false })
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    color =
                        if (emphasise) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .width(READOUT)
                            .then(
                                if (entry == null) {
                                    Modifier
                                } else {
                                    Modifier
                                        .clickable { typing = true }
                                        .semantics { contentDescription = entry.label }
                                },
                            ),
                )
            }
            StepButton(
                icon = Lucide.Plus,
                description = stringResource(R.string.stepper_increase),
                enabled = canIncrease && !typing,
                onClick = onIncrease,
            )
        }
    }
}

@Composable
private fun Readout(
    entry: StepperEntry,
    onSettled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(entry.value.toString()) }
    var focused by remember { mutableStateOf(false) }
    var settled by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val manager = LocalFocusManager.current

    val settle = {
        if (!settled) {
            settled = true
            entry.onCommit(text.toIntOrNull()?.coerceIn(entry.range) ?: entry.value)
            onSettled()
        }
    }

    LaunchedEffect(Unit) { focus.requestFocus() }

    BasicTextField(
        value = text,
        onValueChange = { raw -> text = raw.filter { it.isDigit() }.take(MAX_DIGITS) },
        singleLine = true,
        textStyle =
            LocalTextStyle.current.merge(
                MaterialTheme.typography.labelLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                ),
            ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions =
            KeyboardActions(
                onDone = {
                    settle()
                    manager.clearFocus()
                },
            ),
        modifier =
            modifier
                .width(READOUT)
                .semantics { contentDescription = entry.label }
                .focusRequester(focus)
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        focused = true
                    } else if (focused) {
                        settle()
                    }
                },
    )
}

@Composable
private fun StepButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(TAP),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint =
                if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = SPENT)
                },
            modifier = Modifier.size(GLYPH),
        )
    }
}

private val HAIRLINE = 1.dp
private val TAP = 34.dp
private val GLYPH = 15.dp
private val READOUT = 46.dp
private const val SPENT = 0.38f
private const val MAX_DIGITS = 5

@Composable
private fun StepperSample(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(BrainXPTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.sm),
    ) {
        Stepper(
            value = "2 mnt",
            onDecrease = {},
            onIncrease = {},
            emphasise = true,
            entry = StepperEntry(value = 2, range = 0..5, label = "Reward", onCommit = {}),
        )
        Stepper(value = "10", onDecrease = {}, onIncrease = {}, canIncrease = false)
        Stepper(value = "20%", onDecrease = {}, onIncrease = {}, canDecrease = false)
    }
}

@Preview(name = "Stepper light", showBackground = true)
@Composable
private fun StepperPreview() {
    BrainXPTheme { StepperSample() }
}

@Preview(name = "Stepper dark", showBackground = true)
@Composable
private fun StepperDarkPreview() {
    BrainXPTheme(darkTheme = true) { Box { StepperSample() } }
}

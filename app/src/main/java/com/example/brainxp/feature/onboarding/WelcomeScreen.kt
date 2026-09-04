package com.example.brainxp.feature.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.brainxp.R
import com.example.brainxp.core.time.clock
import com.example.brainxp.core.ui.BrainXPTextStyles
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.LightSystemBars
import com.example.brainxp.core.ui.PillShape
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.Tokens
import kotlinx.coroutines.delay

private data class Taking(
    val labelRes: Int,
    val levelRes: Int,
    val gainSeconds: Int,
)

private val TAKINGS =
    listOf(
        Taking(R.string.welcome_take_motion, R.string.welcome_level_easy, 72),
        Taking(R.string.welcome_take_newton, R.string.welcome_level_medium, 120),
        Taking(R.string.welcome_take_friction, R.string.welcome_level_hard, 345),
    )

@Composable
fun WelcomeScreen(
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing
    LightSystemBars()

    Box(modifier = modifier.fillMaxSize().background(Tokens.Blue900)) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = spacing.xl)
                    .padding(top = spacing.xl, bottom = spacing.screenBottom),
        ) {
            Spacer(modifier = Modifier.size(spacing.xxxl))
            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.displaySmall,
                color = Tokens.Neutral0,
            )
            Spacer(modifier = Modifier.size(spacing.md))
            Text(
                text = stringResource(R.string.welcome_body),
                style = MaterialTheme.typography.bodyMedium,
                color = Tokens.Neutral0.copy(alpha = BODY_ALPHA),
                modifier = Modifier.widthIn(max = BODY_WIDTH),
            )

            Spacer(modifier = Modifier.weight(1f))
            RewardMeter()
            Spacer(modifier = Modifier.weight(1f))

            PrimaryButton(
                text = stringResource(R.string.welcome_start),
                onClick = onStart,
                onDark = true,
            )
        }
    }
}

@Composable
private fun RewardMeter(modifier: Modifier = Modifier) {
    var claimed by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        delay(FIRST_DELAY_MS)
        while (true) {
            for (index in 1..TAKINGS.size) {
                claimed = index
                delay(STEP_MS)
            }
            delay(HOLD_MS)
            claimed = 0
            delay(STEP_MS)
        }
    }

    val earned = TAKINGS.take(claimed).sumOf { it.gainSeconds }
    val shown by
        animateFloatAsState(
            targetValue = earned.toFloat(),
            animationSpec = tween(durationMillis = COUNT_MS),
            label = "meterSeconds",
        )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = Tokens.Neutral0.copy(alpha = CARD_ALPHA),
    ) {
        Column(
            modifier =
                Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Tokens.Neutral0.copy(alpha = SHEEN_TOP),
                                Tokens.Neutral0.copy(alpha = SHEEN_BOTTOM),
                            ),
                        ),
                    ).padding(horizontal = CARD_HORIZONTAL, vertical = CARD_VERTICAL),
        ) {
            Text(
                text = stringResource(R.string.welcome_meter_label).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Tokens.Blue300,
            )
            Spacer(modifier = Modifier.size(BrainXPTheme.spacing.xs))
            Text(
                text = clock(shown.toInt()),
                style = BrainXPTextStyles.numeric,
                color = Tokens.Neutral0,
            )
            Spacer(modifier = Modifier.size(BrainXPTheme.spacing.md))

            TAKINGS.forEachIndexed { index, taking ->
                if (index > 0) {
                    HorizontalDivider(
                        thickness = HAIRLINE,
                        color = Tokens.Neutral0.copy(alpha = DIVIDER_ALPHA),
                    )
                }
                TakingRow(taking = taking, claimed = index < claimed)
            }
        }
    }
}

@Composable
private fun TakingRow(
    taking: Taking,
    claimed: Boolean,
    modifier: Modifier = Modifier,
) {
    val alpha by
        animateFloatAsState(
            targetValue = if (claimed) 1f else DIM_ALPHA,
            animationSpec = tween(durationMillis = FADE_MS),
            label = "takingAlpha",
        )

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = ROW_VERTICAL),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ROW_GAP),
    ) {
        Surface(
            modifier = Modifier.size(DOT),
            shape = PillShape,
            color =
                if (claimed) {
                    Tokens.Mint.copy(alpha = alpha)
                } else {
                    Tokens.Neutral0.copy(alpha = DOT_ALPHA)
                },
        ) {}
        Text(
            text = stringResource(taking.labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = Tokens.Neutral0.copy(alpha = alpha),
        )
        Text(
            text = stringResource(taking.levelRes),
            style = MaterialTheme.typography.labelSmall,
            color = Tokens.Blue300.copy(alpha = alpha),
        )
        Text(
            text = stringResource(R.string.welcome_gain, clock(taking.gainSeconds)),
            style = MaterialTheme.typography.labelSmall,
            color = Tokens.Mint.copy(alpha = alpha),
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val BODY_WIDTH = 280.dp
private val CARD_HORIZONTAL = 18.dp
private val CARD_VERTICAL = 16.dp
private val DOT = 7.dp
private val HAIRLINE = 1.dp
private val ROW_GAP = 9.dp
private val ROW_VERTICAL = 7.dp

private const val BODY_ALPHA = 0.68f
private const val CARD_ALPHA = 0.06f
private const val SHEEN_TOP = 0.11f
private const val SHEEN_BOTTOM = 0.05f
private const val DIVIDER_ALPHA = 0.08f
private const val DIM_ALPHA = 0.32f
private const val DOT_ALPHA = 0.3f

private const val FIRST_DELAY_MS = 500L
private const val STEP_MS = 620L
private const val HOLD_MS = 1_900L
private const val COUNT_MS = 620
private const val FADE_MS = 350

@Preview(name = "Welcome", showBackground = true, heightDp = 780)
@Composable
private fun WelcomePreview() {
    BrainXPTheme { WelcomeScreen(onStart = {}) }
}

package com.example.brainxp.blocking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.HeroCard
import com.example.brainxp.core.ui.HeroTone
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.StatusPill
import com.example.brainxp.core.ui.Tokens
import com.example.brainxp.core.ui.shortDuration

@Composable
fun BlockScreen(
    appLabel: String,
    balanceSeconds: Int,
    onEarnTime: () -> Unit,
    modifier: Modifier = Modifier,
    capReached: Boolean = false,
) {
    BrainXPTheme(darkTheme = false) {
        val spacing = BrainXPTheme.spacing

        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(Tokens.Blue900)
                    .padding(horizontal = spacing.xl, vertical = spacing.xxxl),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Spacer(modifier = Modifier.weight(1f))

            StatusPill(text = appLabel, tone = PillTone.ON_DARK)

            Text(
                text =
                    stringResource(
                        if (capReached) R.string.block_screen_cap_title else R.string.block_screen_title,
                    ),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                modifier = Modifier.padding(top = spacing.md),
            )

            Text(
                text =
                    stringResource(
                        if (capReached) R.string.block_screen_cap_body else R.string.block_screen_body,
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = SECONDARY_INK),
            )

            HeroCard(
                label = stringResource(R.string.block_screen_hero_label),
                value = shortDuration(balanceSeconds),
                tone = HeroTone.GHOST,
                progress = 1f,
                modifier = Modifier.padding(top = spacing.md),
                footer = {
                    Text(
                        text = stringResource(R.string.block_screen_hero_sub),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = SECONDARY_INK),
                    )
                },
            )

            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = spacing.sm),
                shape = MaterialTheme.shapes.medium,
                color = Color.White.copy(alpha = NOTE_FILL),
            ) {
                Text(
                    text = stringResource(R.string.block_screen_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = SECONDARY_INK),
                    modifier = Modifier.padding(spacing.md),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            PrimaryButton(
                text = stringResource(R.string.block_screen_action),
                onClick = onEarnTime,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private const val SECONDARY_INK = 0.68f
private const val NOTE_FILL = 0.09f

@Preview(name = "Block screen empty balance", showBackground = true, heightDp = 800)
@Composable
private fun BlockScreenPreview() {
    BlockScreen(appLabel = "Mobile Legends", balanceSeconds = 0, onEarnTime = {})
}

@Preview(name = "Block screen daily cap", showBackground = true, heightDp = 800)
@Composable
private fun BlockScreenCapPreview() {
    BlockScreen(
        appLabel = "Instagram",
        balanceSeconds = 1_500,
        onEarnTime = {},
        capReached = true,
    )
}

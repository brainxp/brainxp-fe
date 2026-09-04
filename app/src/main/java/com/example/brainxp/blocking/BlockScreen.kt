package com.example.brainxp.blocking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.StatusPill
import com.example.brainxp.core.ui.Tokens

@Composable
fun BlockScreen(
    blockedPackage: String,
    onEarnTime: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BrainXPTheme(darkTheme = false) {
        val spacing = BrainXPTheme.spacing

        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(Tokens.Blue900)
                    .padding(horizontal = spacing.xl, vertical = spacing.xxxl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StatusPill(stringResource(R.string.block_screen_badge), tone = PillTone.ON_DARK)

            Text(
                text = stringResource(R.string.block_screen_title),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = spacing.xl),
            )

            Text(
                text = stringResource(R.string.block_screen_body),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = SECONDARY_INK),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = spacing.md),
            )

            Text(
                text = blockedPackage,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = TERTIARY_INK),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = spacing.xxl),
            )

            PrimaryButton(
                text = stringResource(R.string.block_screen_action),
                onClick = onEarnTime,
                modifier = Modifier.fillMaxWidth().padding(top = spacing.xxxl),
            )
        }
    }
}

private const val SECONDARY_INK = 0.68f
private const val TERTIARY_INK = 0.5f

@Preview(name = "Block screen", showBackground = true)
@Composable
private fun BlockScreenPreview() {
    BlockScreen(blockedPackage = "com.mobile.legends", onEarnTime = {})
}

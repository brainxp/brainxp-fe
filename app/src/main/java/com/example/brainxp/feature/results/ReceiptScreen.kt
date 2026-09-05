package com.example.brainxp.feature.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.brainxp.R
import com.example.brainxp.core.time.clock
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.ReceiptCard
import com.example.brainxp.core.ui.ReceiptLine
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.core.ui.StatusPill
import com.example.brainxp.core.ui.longDuration
import com.example.brainxp.core.ui.multiplierText
import com.example.brainxp.core.ui.shortDuration

data class ReceiptRow(
    val ordinal: Int,
    val label: String,
    val difficulty: String,
    val multiplier: Double,
    val rewardSeconds: Int,
    val voided: Boolean = false,
    val voidReason: String? = null,
    val explanation: String? = null,
)

data class ReceiptUiState(
    val title: String,
    val correctCount: Int,
    val questionCount: Int,
    val baseRewardSeconds: Int,
    val rows: List<ReceiptRow>,
    val subtotalSeconds: Int,
    val levelFactor: Double,
    val levelNote: String,
    val noveltyFactor: Double,
    val noveltyNote: String,
    val creditedSeconds: Int,
    val balanceSeconds: Int,
    val streakCurrent: Int,
    val newBadges: List<String> = emptyList(),
) {
    val missed: List<ReceiptRow> get() = rows.filter { it.voided && it.explanation != null }
}

@Composable
fun ReceiptScreen(
    state: ReceiptUiState,
    onHome: () -> Unit,
    onLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing

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
        ScreenNav(title = stringResource(R.string.receipt_title)) {
            StatusPill(
                text =
                    stringResource(
                        R.string.receipt_correct,
                        state.correctCount,
                        state.questionCount,
                    ),
                tone = PillTone.BLUE,
            )
        }

        ReceiptCard(
            header = stringResource(R.string.receipt_header),
            lines = receiptLines(state),
            totalLabel = stringResource(R.string.receipt_credited),
            totalValue = "+${longDuration(state.creditedSeconds)}",
        )

        Text(
            text = "${stringResource(R.string.receipt_new_balance)} · ${shortDuration(state.balanceSeconds)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.missed.isNotEmpty()) {
            Text(
                text = stringResource(R.string.receipt_missed),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = spacing.sm),
            )
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                state.missed.forEach { row -> MissedCard(row = row) }
            }
        }

        Note(text = streakNote(state))

        PrimaryButton(
            text = stringResource(R.string.receipt_home),
            onClick = onHome,
            modifier = Modifier.padding(top = spacing.sm),
        )
        TextButton(onClick = onLibrary, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.receipt_library),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun MissedCard(
    row: ReceiptRow,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(
                text = stringResource(R.string.receipt_missed_row, row.ordinal, row.difficulty),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = row.explanation.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun receiptLines(state: ReceiptUiState): List<ReceiptLine> {
    val base =
        ReceiptLine(
            label = stringResource(R.string.receipt_base),
            value = clock(state.baseRewardSeconds),
            heading = true,
        )
    val perQuestion =
        state.rows.map { row ->
            ReceiptLine(
                label = "${row.label} ${multiplierText(row.multiplier)}",
                value =
                    if (row.voided) {
                        row.voidReason ?: stringResource(R.string.void_wrong)
                    } else {
                        "+${clock(row.rewardSeconds)}"
                    },
                voided = row.voided,
            )
        }
    val tail =
        listOf(
            ReceiptLine(
                label = stringResource(R.string.receipt_subtotal),
                value = "+${clock(state.subtotalSeconds)}",
                heading = true,
            ),
            ReceiptLine(
                label = stringResource(R.string.receipt_level, state.levelNote),
                value = multiplierText(state.levelFactor),
            ),
            ReceiptLine(
                label = stringResource(R.string.receipt_novelty, state.noveltyNote),
                value = multiplierText(state.noveltyFactor),
            ),
        )

    return listOf(base) + perQuestion + tail
}

@Composable
private fun streakNote(state: ReceiptUiState): String {
    val tail =
        if (state.newBadges.isEmpty()) {
            stringResource(R.string.receipt_streak_keep)
        } else {
            stringResource(R.string.receipt_new_badges, state.newBadges.joinToString(", "))
        }
    return stringResource(R.string.receipt_streak, state.streakCurrent, tail)
}

private val PREVIEW_RECEIPT =
    ReceiptUiState(
        title = "Bab 4 — Gerak Lurus",
        correctCount = 4,
        questionCount = 6,
        baseRewardSeconds = 120,
        rows =
            listOf(
                ReceiptRow(1, "Soal 1", "mudah", 1.0, 120),
                ReceiptRow(2, "Soal 2", "sedang", 1.2, 144),
                ReceiptRow(
                    3,
                    "Soal 3",
                    "sedang",
                    1.2,
                    0,
                    voided = true,
                    voidReason = "salah",
                    explanation = "Perlambatan dihitung dari selisih kecepatan dibagi waktu, bukan dikalikan.",
                ),
                ReceiptRow(4, "Soal 4", "sulit", 1.6, 192),
                ReceiptRow(
                    5,
                    "Soal 5",
                    "sulit",
                    1.6,
                    0,
                    voided = true,
                    voidReason = "di bawah ambang",
                    explanation = "Jawabannya belum menyebut satuan dan arah percepatannya.",
                ),
                ReceiptRow(6, "Esai", "sulit", 2.88, 346),
            ),
        subtotalSeconds = 802,
        levelFactor = 1.0,
        levelNote = "setara jenjangmu",
        noveltyFactor = 0.6,
        noveltyNote = "sudah pernah dipelajari",
        creditedSeconds = 481,
        balanceSeconds = 1_981,
        streakCurrent = 4,
        newBadges = listOf("Penulis"),
    )

@Preview(name = "Receipt", showBackground = true, heightDp = 1300)
@Composable
private fun ReceiptPreview() {
    BrainXPTheme { ReceiptScreen(state = PREVIEW_RECEIPT, onHome = {}, onLibrary = {}) }
}

package com.example.brainxp.feature.results

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.CircleX
import com.composables.icons.lucide.Lucide
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTextStyles
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.HeroCard
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.RowGroup
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.core.ui.StatusPill
import com.example.brainxp.core.ui.multiplierText
import com.example.brainxp.core.ui.shortDuration
import com.example.brainxp.feature.questions.difficultyLabel
import com.example.brainxp.feature.questions.questionTypeLabel

data class ReceiptRow(
    val ordinal: Int,
    val label: String,
    val difficulty: String,
    val multiplier: Double,
    val rewardSeconds: Int,
    val voided: Boolean = false,
    val voidReason: String? = null,
    val explanation: String? = null,
    val qtype: String = "",
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
)

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
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal)
                .padding(bottom = spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        ScreenNav(title = stringResource(R.string.receipt_title))

        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.receipt_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HeroCard(
            label = stringResource(R.string.receipt_reward_hero),
            value = shortDuration(state.creditedSeconds),
            badge = {
                StatusPill(
                    text =
                        stringResource(
                            R.string.receipt_correct,
                            state.correctCount,
                            state.questionCount,
                        ),
                    tone = PillTone.ON_DARK,
                )
            },
            footer = {
                Text(
                    text =
                        stringResource(
                            R.string.receipt_balance_now,
                            shortDuration(state.balanceSeconds),
                        ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            },
        )

        ResultSectionHeading(
            title = stringResource(R.string.receipt_review_title),
            body = stringResource(R.string.receipt_review_body),
        )

        var opened by rememberSaveable { mutableStateOf(emptySet<Int>()) }

        RowGroup {
            state.rows.forEach { row ->
                val explanation = row.explanationText()
                val open = row.ordinal in opened
                item(
                    title = row.displayTitle(),
                    subtitle = row.resultSummary(open),
                    onClick = explanation?.let { { opened = opened.toggled(row.ordinal) } },
                    expanded = open,
                    details = explanation?.let { { ExplanationText(it) } },
                    leading = {
                        Icon(
                            imageVector = if (row.voided) Lucide.CircleX else Lucide.CircleCheck,
                            contentDescription = null,
                            tint =
                                if (row.voided) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    BrainXPTheme.extendedColors.ok
                                },
                            modifier = Modifier.size(spacing.xl),
                        )
                    },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ResultValue(
                                text = stringResource(R.string.receipt_question_reward, shortDuration(row.rewardSeconds)),
                                positive = !row.voided,
                            )
                            if (explanation != null) {
                                ExpandChevron(open = open)
                            }
                        }
                    },
                )
            }
        }

        ResultSectionHeading(
            title = stringResource(R.string.receipt_calculation_title),
            body = stringResource(R.string.receipt_calculation_body),
        )

        RowGroup {
            item(
                title = stringResource(R.string.receipt_subtotal),
                trailing = {
                    ResultValue(
                        text = stringResource(R.string.receipt_question_reward, shortDuration(state.subtotalSeconds)),
                    )
                },
            )
            item(
                title = stringResource(R.string.receipt_level_title),
                subtitle = state.levelNote,
                trailing = { ResultValue(text = multiplierText(state.levelFactor)) },
            )
            item(
                title = stringResource(R.string.receipt_novelty_title),
                subtitle = state.noveltyNote,
                trailing = { ResultValue(text = multiplierText(state.noveltyFactor)) },
            )
            item(
                title = stringResource(R.string.receipt_credited),
                trailing = {
                    ResultValue(
                        text = stringResource(R.string.receipt_question_reward, shortDuration(state.creditedSeconds)),
                        positive = true,
                    )
                },
            )
        }

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
private fun ResultSectionHeading(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing

    Column(
        modifier = modifier.fillMaxWidth().padding(top = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ResultValue(
    text: String,
    modifier: Modifier = Modifier,
    positive: Boolean = false,
) {
    Text(
        text = text,
        style =
            MaterialTheme.typography.titleSmall.copy(
                fontFeatureSettings = BrainXPTextStyles.TABULAR_FIGURES,
            ),
        color = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

@Composable
private fun ReceiptRow.displayTitle(): String {
    val number = stringResource(R.string.receipt_question_fallback, ordinal)
    val type = questionTypeLabel(qtype)
    val level = difficultyLabel(difficulty).takeIf { it.isNotBlank() }
    return when {
        type != null && level != null -> stringResource(R.string.receipt_row_title_typed_level, number, type, level)
        type != null -> stringResource(R.string.receipt_row_title_typed, number, type)
        level != null -> stringResource(R.string.receipt_row_title_level, number, level)
        else -> number
    }
}

@Composable
private fun ExpandChevron(open: Boolean) {
    val turn by animateFloatAsState(targetValue = if (open) HALF_TURN else 0f, label = "chevron")
    Icon(
        imageVector = Lucide.ChevronDown,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .padding(start = BrainXPTheme.spacing.sm)
                .size(CHEVRON)
                .rotate(turn),
    )
}

private const val HALF_TURN = 180f
private val CHEVRON = 18.dp

private fun ReceiptRow.explanationText(): String? = (explanation ?: voidReason)?.takeIf { voided && it.isNotBlank() }

@Composable
private fun ReceiptRow.resultSummary(open: Boolean): String =
    when {
        explanationText() != null -> stringResource(if (open) R.string.receipt_row_hide else R.string.receipt_row_show)
        voided -> stringResource(R.string.receipt_row_incorrect)
        else -> stringResource(R.string.receipt_row_correct)
    }

@Composable
private fun ExplanationText(text: String) {
    val spacing = BrainXPTheme.spacing
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = spacing.lg + EXPLANATION_INDENT + spacing.md, end = spacing.lg, bottom = spacing.md),
    )
}

private fun Set<Int>.toggled(value: Int): Set<Int> = if (value in this) this - value else this + value

private val EXPLANATION_INDENT = 38.dp

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

@Preview(name = "Hasil belajar", showBackground = true, heightDp = 1500)
@Composable
private fun ReceiptPreview() {
    BrainXPTheme { ReceiptScreen(state = PREVIEW_RECEIPT, onHome = {}, onLibrary = {}) }
}

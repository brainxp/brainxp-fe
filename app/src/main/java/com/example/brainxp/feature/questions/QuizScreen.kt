package com.example.brainxp.feature.questions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.Field
import com.example.brainxp.core.ui.PillShape
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.core.ui.StatusPill
import com.example.brainxp.core.ui.multiplierText

@Composable
fun QuizScreen(
    state: QuizUiState,
    onEvent: (QuizEvent) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val spacing = BrainXPTheme.spacing
    val question = state.current ?: return

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = spacing.screenHorizontal)
                .padding(bottom = spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        ScreenNav(title = state.title, onBack = onBack) {
            StatusPill(
                text = "${question.difficulty} ${multiplierText(question.factor)}",
                tone = PillTone.BLUE,
            )
        }

        ProgressStrip(state = state)
        QuestionDots(state = state, onEvent = onEvent)

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(
                text = question.stem,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = spacing.xs),
            )

            if (question.essay) {
                EssayAnswer(state = state, question = question, onEvent = onEvent)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    question.options.forEachIndexed { index, text ->
                        OptionRow(
                            letter = LETTERS[index].toString(),
                            text = text,
                            selected = state.chosen == index,
                            onClick = { onEvent(QuizEvent.Choose(index)) },
                        )
                    }
                }
            }
        }

        Text(
            text =
                stringResource(
                    if (state.complete) {
                        R.string.quiz_hint_complete
                    } else {
                        R.string.quiz_hint_partial
                    },
                ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        PrimaryButton(
            text =
                stringResource(if (state.answered) R.string.quiz_change else R.string.quiz_save),
            onClick = { onEvent(QuizEvent.Save) },
            enabled = state.canSave,
            loading = state.busy,
        )

        if (state.complete) {
            OutlinedButton(
                onClick = { onEvent(QuizEvent.Submit) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                enabled = !state.busy,
            ) {
                Text(
                    text = stringResource(R.string.quiz_submit),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun ProgressStrip(
    state: QuizUiState,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.quiz_position, state.index + 1, state.total),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(TRACK)
                    .background(scheme.surfaceContainerHigh, PillShape),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(state.progress)
                        .height(TRACK)
                        .background(scheme.primary, PillShape),
            )
        }
        Text(
            text =
                if (state.pending.isEmpty()) {
                    stringResource(R.string.quiz_filled, state.filled)
                } else {
                    stringResource(R.string.quiz_pending, state.pending.size)
                },
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun QuestionDots(
    state: QuizUiState,
    onEvent: (QuizEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DOT_GAP),
        verticalArrangement = Arrangement.spacedBy(DOT_GAP),
    ) {
        state.questions.forEachIndexed { index, question ->
            val done = question.id in state.answers
            val here = index == state.index

            Box(
                modifier =
                    Modifier
                        .size(DOT_TAP)
                        .clickable(
                            onClickLabel = stringResource(R.string.quiz_jump_to, index + 1),
                            onClick = { onEvent(QuizEvent.Jump(index)) },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.size(DOT),
                    shape = PillShape,
                    color = if (done) scheme.primary else scheme.surfaceContainerHigh,
                    border = if (here) BorderStroke(RING, scheme.primary) else null,
                ) {}
            }
        }
    }
}

@Composable
private fun EssayAnswer(
    state: QuizUiState,
    question: QuizQuestion,
    onEvent: (QuizEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing
    val scheme = MaterialTheme.colorScheme

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        question.sourceExcerpt?.let { excerpt ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = scheme.surfaceContainerHigh,
            ) {
                Text(
                    text = stringResource(R.string.quiz_excerpt, excerpt),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(spacing.md),
                )
            }
        }
        Field(
            label = stringResource(R.string.quiz_answer_label),
            value = state.draft,
            onValueChange = { onEvent(QuizEvent.Draft(it)) },
            placeholder = stringResource(R.string.quiz_answer_hint),
            enabled = !state.busy,
            multiline = true,
        )
        if (question.rubricCriteria > 0) {
            Text(
                text = stringResource(R.string.quiz_rubric, question.rubricCriteria),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OptionRow(
    letter: String,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) scheme.primaryContainer else scheme.surface,
        border =
            BorderStroke(
                if (selected) RING else HAIRLINE,
                if (selected) scheme.primary else scheme.outline,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = OPTION_H, vertical = OPTION_V),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.md),
        ) {
            Surface(
                modifier = Modifier.size(BADGE),
                shape = MaterialTheme.shapes.small,
                color = if (selected) scheme.primary else scheme.surfaceContainerHigh,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
            )
        }
    }
}

private const val LETTERS = "ABCDEF"
private val TRACK = 6.dp
private val DOT = 12.dp
private val DOT_TAP = 48.dp
private val DOT_GAP = 6.dp
private val RING = 1.5.dp
private val HAIRLINE = 1.dp
private val BADGE = 30.dp
private val OPTION_H = 14.dp
private val OPTION_V = 13.dp

private val PREVIEW_MCQ =
    QuizUiState(
        title = "Bab 4 — Gerak Lurus",
        questions =
            listOf(
                QuizQuestion(
                    id = "q1",
                    stem = "Sebuah mobil bergerak 20 m/s lalu berhenti dalam 4 detik. Berapa perlambatannya?",
                    difficulty = "sedang",
                    factor = 1.2,
                    options = listOf("2 m/s²", "4 m/s²", "5 m/s²", "80 m/s²"),
                ),
                QuizQuestion(id = "q2", stem = "Soal kedua", difficulty = "mudah", factor = 1.0),
                QuizQuestion(id = "q3", stem = "Soal ketiga", difficulty = "sulit", factor = 1.6),
            ),
        answers = mapOf("q2" to "sudah"),
        chosen = 1,
    )

private val PREVIEW_ESSAY =
    QuizUiState(
        title = "Bab 4 — Gerak Lurus",
        questions =
            listOf(
                QuizQuestion(
                    id = "e1",
                    stem = "Jelaskan bedanya kecepatan dan percepatan dengan contohmu sendiri.",
                    difficulty = "sulit",
                    factor = 1.8,
                    sourceExcerpt = "Percepatan adalah laju perubahan kecepatan terhadap waktu.",
                    rubricCriteria = 3,
                ),
                QuizQuestion(id = "e2", stem = "Soal kedua", difficulty = "mudah", factor = 1.0),
            ),
        draft = "Kecepatan itu seberapa cepat",
    )

@Preview(name = "Quiz multiple choice", showBackground = true, heightDp = 880)
@Composable
private fun QuizPreview() {
    BrainXPTheme { QuizScreen(state = PREVIEW_MCQ, onEvent = {}, onBack = {}) }
}

@Preview(name = "Quiz essay", showBackground = true, heightDp = 880)
@Composable
private fun QuizEssayPreview() {
    BrainXPTheme { QuizScreen(state = PREVIEW_ESSAY, onEvent = {}, onBack = {}) }
}

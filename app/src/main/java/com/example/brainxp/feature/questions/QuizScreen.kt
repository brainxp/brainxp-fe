package com.example.brainxp.feature.questions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.Field
import com.example.brainxp.core.ui.Note
import com.example.brainxp.core.ui.PillShape
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.core.ui.StatusPill
import com.example.brainxp.core.ui.Tokens
import com.example.brainxp.core.ui.multiplierText

@Composable
fun QuizScreen(
    state: QuizUiState,
    onEvent: (QuizEvent) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onGuide: (() -> Unit)? = null,
) {
    val spacing = BrainXPTheme.spacing
    if (state.questions.isEmpty()) return

    var confirming by rememberSaveable { mutableStateOf(false) }
    val pager = rememberPagerState(initialPage = state.index, pageCount = { state.total })
    val shown by rememberUpdatedState(state.index)
    val dispatch by rememberUpdatedState(onEvent)

    LaunchedEffect(state.index) {
        if (pager.currentPage != state.index) pager.animateScrollToPage(state.index)
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.currentPage }.collect { page ->
            if (page != shown) dispatch(QuizEvent.Jump(page))
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = spacing.screenHorizontal)
                .padding(bottom = spacing.screenBottom),
    ) {
        ScreenNav(title = state.title, onBack = onBack) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                state.current?.let { question ->
                    StatusPill(
                        text = "${question.difficulty} ${multiplierText(question.factor)}",
                        tone = PillTone.BLUE,
                    )
                }
                if (onGuide != null) {
                    GuideButton(onClick = onGuide)
                }
            }
        }

        ProgressStrip(state = state)
        QuestionDots(state = state, onJump = { onEvent(QuizEvent.Jump(it)) })

        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f),
            pageSpacing = spacing.lg,
            verticalAlignment = Alignment.Top,
        ) { page ->
            val question = state.questions[page]

            QuestionPage(
                state = state,
                question = question,
                onDraft = { onEvent(QuizEvent.Draft(question.id, it)) },
                onChoose = { onEvent(QuizEvent.Choose(question.id, it)) },
            )
        }

        Column(
            modifier = Modifier.padding(top = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            if (confirming) {
                Note(text = stringResource(R.string.quiz_confirm_doubts, state.doubtedCount), alert = true)
                PrimaryButton(
                    text = stringResource(R.string.quiz_submit_now),
                    onClick = { onEvent(QuizEvent.Submit) },
                )
                TextButton(onClick = { confirming = false }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.quiz_review_again))
                }
            } else {
                Text(
                    text =
                        if (state.complete) {
                            stringResource(R.string.quiz_hint_complete)
                        } else {
                            stringResource(R.string.quiz_hint_partial, state.total - state.filled)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.current?.let { question ->
                    DoubtButton(
                        marked = question.id in state.doubts,
                        onClick = { onEvent(QuizEvent.ToggleDoubt(question.id)) },
                    )
                }
                if (state.complete) {
                    PrimaryButton(
                        text = stringResource(R.string.quiz_submit),
                        onClick = {
                            if (state.doubtedCount > 0) confirming = true else onEvent(QuizEvent.Submit)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.size(TAP).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = scheme.surface,
        border = BorderStroke(SELECTED_RING, scheme.outline),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.quiz_guide_mark),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuestionPage(
    state: QuizUiState,
    question: QuizQuestion,
    onDraft: (String) -> Unit,
    onChoose: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text(
            text = question.stem,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (question.essay) {
            EssayAnswer(state = state, question = question, onDraft = onDraft)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                question.options.forEachIndexed { index, text ->
                    OptionRow(
                        letter = LETTERS[index].toString(),
                        text = text,
                        selected = state.chosenFor(question.id) == index,
                        onClick = { onChoose(index) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun DoubtButton(
    marked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .height(DOUBT_HEIGHT)
                .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (marked) Tokens.Amber100 else Tokens.Amber500,
        border = if (marked) BorderStroke(SELECTED_RING, Tokens.Amber400) else null,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(if (marked) R.string.quiz_doubt_marked else R.string.quiz_doubt),
                style = MaterialTheme.typography.labelLarge,
                color = if (marked) Tokens.Amber700 else Tokens.Neutral0,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun QuestionDots(
    state: QuizUiState,
    onJump: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth().padding(vertical = DOT_MARGIN),
        horizontalArrangement = Arrangement.spacedBy(DOT_GAP),
        verticalArrangement = Arrangement.spacedBy(DOT_GAP),
    ) {
        state.questions.forEachIndexed { position, question ->
            val dot = state.dotState(position)
            val label =
                if (question.id in state.doubts) {
                    stringResource(R.string.quiz_jump_doubted, position + 1)
                } else {
                    stringResource(R.string.quiz_jump, position + 1)
                }

            Box(
                modifier =
                    Modifier
                        .width(DOT_WIDTH)
                        .height(DOT_TAP)
                        .clickable(onClickLabel = label, onClick = { onJump(position) }),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(if (position == state.index) DOT_CURRENT else DOT_HEIGHT)
                            .background(dot.colour(), PillShape),
                )
            }
        }
    }
}

@Composable
internal fun ProgressStrip(
    state: QuizUiState,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
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
internal fun OptionRow(
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
        color = if (selected) Tokens.Blue50 else scheme.surface,
        border = BorderStroke(SELECTED_RING, if (selected) scheme.primary else scheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = OPTION_HORIZONTAL, vertical = OPTION_VERTICAL),
            horizontalArrangement = Arrangement.spacedBy(BrainXPTheme.spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(LETTER)
                        .background(
                            if (selected) scheme.primary else scheme.surfaceContainerHigh,
                            MaterialTheme.shapes.extraSmall,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letter,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) Tokens.Neutral0 else scheme.onSurfaceVariant,
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) Tokens.Blue800 else scheme.onSurface,
            )
        }
    }
}

@Composable
private fun EssayAnswer(
    state: QuizUiState,
    question: QuizQuestion,
    onDraft: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        question.sourceExcerpt?.let { excerpt ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Box(
                    modifier =
                        Modifier
                            .width(QUOTE_EDGE)
                            .height(QUOTE_HEIGHT)
                            .background(Tokens.Blue300, PillShape),
                )
                Text(
                    text = stringResource(R.string.quiz_excerpt, excerpt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Field(
            label = stringResource(R.string.quiz_answer_label),
            value = state.draftFor(question.id),
            onValueChange = onDraft,
            placeholder = stringResource(R.string.quiz_answer_hint),
            multiline = true,
        )
        if (question.rubricCriteria > 0) {
            Text(
                text = stringResource(R.string.quiz_rubric, question.rubricCriteria),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DotState.colour() =
    when (this) {
        DotState.CURRENT -> MaterialTheme.colorScheme.primary
        DotState.ANSWERED -> Tokens.Blue300
        DotState.DOUBTED -> Tokens.Amber400
        DotState.DOUBTED_ANSWERED -> Tokens.Amber500
        DotState.EMPTY -> Tokens.Blue100
    }

internal const val LETTERS = "ABCD"
private val DOT_HEIGHT = 5.dp
private val DOT_CURRENT = 8.dp
private val DOT_WIDTH = 22.dp
private val DOT_TAP = 24.dp
private val DOT_GAP = 6.dp
private val DOT_MARGIN = 6.dp
private val TRACK = 4.dp
private val LETTER = 24.dp
private val SELECTED_RING = 1.5.dp
private val DOUBT_HEIGHT = 52.dp
private val OPTION_HORIZONTAL = 14.dp
private val OPTION_VERTICAL = 13.dp
private val QUOTE_EDGE = 3.dp
private val QUOTE_HEIGHT = 34.dp
private val TAP = 38.dp

@Preview(heightDp = 860)
@Composable
private fun QuizPreview() {
    BrainXPTheme {
        QuizScreen(
            state =
                QuizUiState(
                    title = "Sesi belajar",
                    questions =
                        List(PREVIEW_COUNT) { index ->
                            QuizQuestion(
                                id = "q$index",
                                stem = "Manakah rumus yang tepat untuk gerak lurus beraturan?",
                                difficulty = "sedang",
                                factor = 1.2,
                                options = listOf("s = v × t", "v = v0 + a × t", "F = m × a", "p = m × v"),
                            )
                        },
                    index = 1,
                    answers = mapOf("q0" to "s = v × t"),
                    chosenByQuestion = mapOf("q0" to 0, "q1" to 1),
                    doubts = setOf("q2"),
                ),
            onEvent = {},
            onBack = {},
            onGuide = {},
        )
    }
}

private const val PREVIEW_COUNT = 5

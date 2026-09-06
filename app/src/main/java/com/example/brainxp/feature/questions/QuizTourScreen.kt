package com.example.brainxp.feature.questions

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.LocalRevealCanvas
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.core.ui.StatusPill
import com.example.brainxp.core.ui.Tokens
import com.svenjacobs.reveal.Reveal
import com.svenjacobs.reveal.RevealCanvasState
import com.svenjacobs.reveal.RevealOverlayArrangement
import com.svenjacobs.reveal.RevealOverlayScope
import com.svenjacobs.reveal.RevealShape
import com.svenjacobs.reveal.RevealState
import com.svenjacobs.reveal.effect.dim.DimRevealOverlayEffect
import com.svenjacobs.reveal.rememberRevealState
import com.svenjacobs.reveal.revealable

internal enum class TourSpot {
    QUESTION,
    DOTS,
    DOUBT,
    FOOTER,
    ACTS,
}

internal data class TourStep(
    @StringRes val title: Int,
    @StringRes val body: Int,
    val key: TourSpot,
    val below: Boolean = true,
    val waits: Boolean = false,
    val swipe: Boolean = false,
)

private const val ANSWER_OPTION = 1

internal val TOUR_STEPS =
    listOf(
        TourStep(R.string.tour_answer_title, R.string.tour_answer_body, TourSpot.QUESTION, waits = true),
        TourStep(R.string.tour_autosave_title, R.string.tour_autosave_body, TourSpot.QUESTION),
        TourStep(R.string.tour_doubt_title, R.string.tour_doubt_body, TourSpot.DOUBT, below = false, waits = true),
        TourStep(R.string.tour_dots_title, R.string.tour_dots_body, TourSpot.DOTS),
        TourStep(
            title = R.string.tour_move_title,
            body = R.string.tour_move_body,
            key = TourSpot.DOTS,
            waits = true,
            swipe = true,
        ),
        TourStep(R.string.tour_leave_title, R.string.tour_leave_body, TourSpot.FOOTER, below = false),
        TourStep(R.string.tour_receipt_title, R.string.tour_receipt_body, TourSpot.ACTS, below = false),
    )

@Composable
fun QuizTourScreen(
    onDone: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    ready: Boolean = false,
    onStart: (() -> Unit)? = null,
    footer: @Composable () -> Unit = {},
) {
    TourBody(
        canvas = LocalRevealCanvas.current,
        onDone = onDone,
        onSkip = onSkip,
        ready = ready,
        onStart = onStart,
        footer = footer,
        modifier = modifier,
    )
}

@Composable
private fun TourBody(
    canvas: RevealCanvasState,
    onDone: () -> Unit,
    onSkip: () -> Unit,
    ready: Boolean,
    onStart: (() -> Unit)?,
    footer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing
    val practice = practiceQuestions()
    val reveal = rememberRevealState()
    val flow = remember(practice) { TourFlow(practice) }
    flow.onDone = onDone

    val step = flow.step
    val pager = rememberPagerState(initialPage = 0, pageCount = { practice.size })
    val state = flow.practice()

    LaunchedEffect(flow.at, flow.index) { reveal.settle(flow.step) }

    LaunchedEffect(pager) {
        snapshotFlow { pager.currentPage }.collect { page -> flow.paged(page) }
    }

    Reveal(
        revealCanvasState = canvas,
        revealState = reveal,
        modifier = modifier.fillMaxSize(),
        overlayEffect = DimRevealOverlayEffect(color = Tokens.Blue900.copy(alpha = SCRIM)),
        onRevealableClick = { key -> if (flow.step.waits) flow.tapped(key) },
        overlayContent = { key ->
            if (key == flow.step.key) {
                CoachCard(
                    step = flow.step,
                    last = flow.last,
                    ready = ready,
                    onStart = onStart,
                    onAdvance = flow::advance,
                    onSkip = onSkip,
                    modifier =
                        Modifier
                            .align(
                                if (flow.step.below) {
                                    RevealOverlayArrangement.Bottom
                                } else {
                                    RevealOverlayArrangement.Top
                                },
                            ).padding(BrainXPTheme.spacing.lg),
                )
            }
        },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.screenHorizontal)
                    .padding(bottom = spacing.screenBottom),
        ) {
            ScreenNav(title = stringResource(R.string.tour_title)) {
                StatusPill(
                    text = stringResource(R.string.tour_step, flow.at + 1, TOUR_STEPS.size),
                    tone = PillTone.OUTLINE,
                )
            }

            Column(modifier = Modifier.spot(TourSpot.DOTS, reveal)) {
                ProgressStrip(state = state)
                QuestionDots(state = state, onJump = {})
            }

            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f),
                pageSpacing = spacing.lg,
                userScrollEnabled = step.swipe,
                verticalAlignment = Alignment.Top,
            ) { page ->
                PracticePage(
                    question = practice[page],
                    chosen = flow.picked[practice[page].id],
                    modifier = Modifier.spot(TourSpot.QUESTION, reveal),
                )
            }

            Column(
                modifier = Modifier.padding(top = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Text(
                    text =
                        if (state.complete) {
                            stringResource(R.string.quiz_hint_complete)
                        } else {
                            stringResource(R.string.quiz_hint_partial, state.total - state.filled)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.spot(TourSpot.ACTS, reveal),
                )
                DoubtButton(
                    marked = flow.marked,
                    onClick = {},
                    modifier = Modifier.spot(TourSpot.DOUBT, reveal),
                )
                Column(modifier = Modifier.spot(TourSpot.FOOTER, reveal)) { footer() }

                if (step.swipe) {
                    CoachCard(
                        step = step,
                        last = flow.last,
                        ready = ready,
                        onStart = onStart,
                        onAdvance = flow::advance,
                        onSkip = onSkip,
                        modifier = Modifier.padding(top = spacing.sm),
                    )
                }
            }
        }
    }
}

private fun Modifier.spot(
    key: TourSpot,
    state: RevealState,
): Modifier =
    revealable(
        key = key,
        state = state,
        shape = RevealShape.RoundRect(RING_RADIUS),
        padding = PaddingValues(RING_PADDING),
        borderStroke = BorderStroke(RING_WIDTH, Tokens.Blue500),
    )

@Composable
private fun PracticePage(
    question: QuizQuestion,
    chosen: Int?,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Text(
            text = question.stem,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            question.options.forEachIndexed { option, text ->
                OptionRow(
                    letter = LETTERS[option].toString(),
                    text = text,
                    selected = chosen == option,
                    onClick = {},
                )
            }
        }
    }
}

@Composable
private fun CoachCard(
    step: TourStep,
    last: Boolean,
    ready: Boolean,
    onStart: (() -> Unit)?,
    onAdvance: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = scheme.surface,
        shadowElevation = CARD_ELEVATION,
    ) {
        Column(
            modifier = Modifier.padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(
                text = stringResource(step.title),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
            )
            Text(
                text = stringResource(step.body),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            if (step.swipe) {
                SwipeHint(modifier = Modifier.padding(top = spacing.sm))
            }

            Column(
                modifier = Modifier.padding(top = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                if (step.waits) {
                    WaitHint()
                } else {
                    PrimaryButton(
                        text = stringResource(if (last) R.string.tour_start else R.string.tour_next),
                        onClick = onAdvance,
                    )
                }
                if (ready && onStart != null) {
                    TextButton(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(R.string.preparing_start))
                    }
                } else if (!last) {
                    TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(R.string.tour_skip))
                    }
                }
            }
        }
    }
}

@Composable
private fun WaitHint(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = scheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(BrainXPTheme.spacing.md),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.tour_wait),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSecondaryContainer,
            )
        }
    }
}

private class TourFlow(
    private val questions: List<QuizQuestion>,
) {
    var onDone: () -> Unit = {}
    var at by mutableIntStateOf(0)
        private set
    var index by mutableIntStateOf(0)
        private set
    var marked by mutableStateOf(false)
        private set
    var picked by mutableStateOf(emptyMap<String, Int>())
        private set

    val step: TourStep get() = TOUR_STEPS[at]

    val last: Boolean get() = at == TOUR_STEPS.lastIndex

    fun advance() {
        if (last) onDone() else at += 1
    }

    fun tapped(key: Any) {
        when (key) {
            TourSpot.QUESTION -> picked = picked + (questions[index].id to ANSWER_OPTION)
            TourSpot.DOUBT -> marked = !marked
            TourSpot.DOTS -> index = (index + 1).coerceAtMost(questions.lastIndex)
            else -> Unit
        }
        advance()
    }

    fun paged(page: Int) {
        if (page == index) return
        index = page
        if (step.swipe) advance()
    }

    fun practice(): QuizUiState = practiceState(questions = questions, index = index, picked = picked, marked = marked)
}

private suspend fun RevealState.settle(step: TourStep) {
    if (step.swipe) {
        hide()
        return
    }
    var tries = 0
    while (!containsRevealable(step.key) && tries < REVEAL_TRIES) {
        tries += 1
        withFrameNanos { }
    }
    tryReveal(step.key)
}

@Composable
private fun practiceQuestions(): List<QuizQuestion> =
    listOf(
        QuizQuestion(
            id = "tour-1",
            stem = stringResource(R.string.tour_q1),
            difficulty = "",
            factor = 1.0,
            options =
                listOf(
                    stringResource(R.string.tour_q1_a),
                    stringResource(R.string.tour_q1_b),
                    stringResource(R.string.tour_q1_c),
                    stringResource(R.string.tour_q1_d),
                ),
        ),
        QuizQuestion(
            id = "tour-2",
            stem = stringResource(R.string.tour_q2),
            difficulty = "",
            factor = 1.0,
            options =
                listOf(
                    stringResource(R.string.tour_q2_a),
                    stringResource(R.string.tour_q2_b),
                    stringResource(R.string.tour_q2_c),
                    stringResource(R.string.tour_q2_d),
                ),
        ),
    )

private fun practiceState(
    questions: List<QuizQuestion>,
    index: Int,
    picked: Map<String, Int>,
    marked: Boolean,
): QuizUiState =
    QuizUiState(
        title = "",
        questions = questions,
        index = index,
        answers =
            picked
                .mapNotNull { (id, option) ->
                    questions
                        .firstOrNull { it.id == id }
                        ?.options
                        ?.getOrNull(option)
                        ?.let { id to it }
                }.toMap(),
        chosenByQuestion = picked,
        doubts = if (marked) setOf(questions.first().id) else emptySet(),
    )

private const val SCRIM = 0.62f
private const val REVEAL_TRIES = 60
private val CARD_ELEVATION = 10.dp
private val RING_RADIUS = 16.dp
private val RING_WIDTH = 2.dp
private val RING_PADDING = 6.dp

@Preview(heightDp = 900)
@Composable
private fun QuizTourPreview() {
    BrainXPTheme {
        QuizTourScreen(onDone = {}, onSkip = {})
    }
}

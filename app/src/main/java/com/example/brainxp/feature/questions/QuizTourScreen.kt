package com.example.brainxp.feature.questions

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.brainxp.R
import com.example.brainxp.core.ui.BrainXPTheme
import com.example.brainxp.core.ui.PillTone
import com.example.brainxp.core.ui.PrimaryButton
import com.example.brainxp.core.ui.ScreenNav
import com.example.brainxp.core.ui.StatusPill

internal enum class TourSpot {
    NONE,
    DOTS,
    OPTIONS,
    DOUBT,
    ACTS,
}

internal enum class TourWait {
    ANSWER,
    MARK,
    NEXT,
}

internal data class TourStep(
    @StringRes val title: Int,
    @StringRes val body: Int,
    val spot: TourSpot = TourSpot.NONE,
    val waits: TourWait? = null,
)

internal val TOUR_STEPS =
    listOf(
        TourStep(R.string.tour_answer_title, R.string.tour_answer_body, TourSpot.OPTIONS, TourWait.ANSWER),
        TourStep(R.string.tour_autosave_title, R.string.tour_autosave_body, TourSpot.OPTIONS),
        TourStep(R.string.tour_doubt_title, R.string.tour_doubt_body, TourSpot.DOUBT, TourWait.MARK),
        TourStep(R.string.tour_dots_title, R.string.tour_dots_body, TourSpot.DOTS),
        TourStep(R.string.tour_move_title, R.string.tour_move_body, TourSpot.OPTIONS, TourWait.NEXT),
        TourStep(R.string.tour_leave_title, R.string.tour_leave_body),
        TourStep(R.string.tour_receipt_title, R.string.tour_receipt_body, TourSpot.ACTS),
    )

@Composable
fun QuizTourScreen(
    onDone: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BrainXPTheme.spacing
    val practice = practiceQuestions()

    var at by remember { mutableIntStateOf(0) }
    var index by remember { mutableIntStateOf(0) }
    var marked by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf(emptyMap<String, Int>()) }

    val step = TOUR_STEPS[at]
    val last = at == TOUR_STEPS.lastIndex
    val advance: () -> Unit = { if (at == TOUR_STEPS.lastIndex) onDone() else at += 1 }

    val state =
        practiceState(
            questions = practice,
            index = index,
            picked = picked,
            marked = marked,
        )

    val pager = rememberPagerState(initialPage = index, pageCount = { practice.size })

    LaunchedEffect(pager) {
        snapshotFlow { pager.currentPage }.collect { page ->
            if (page == index) return@collect
            index = page
            if (TOUR_STEPS[at].waits == TourWait.NEXT) advance()
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = spacing.screenHorizontal)
                .padding(bottom = spacing.screenBottom),
    ) {
        ScreenNav(title = stringResource(R.string.tour_title)) {
            StatusPill(
                text = stringResource(R.string.tour_step, at + 1, TOUR_STEPS.size),
                tone = PillTone.OUTLINE,
            )
        }

        Spotlight(lit = step.spot == TourSpot.DOTS) {
            Column {
                ProgressStrip(state = state)
                QuestionDots(state = state, onJump = {})
            }
        }

        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f),
            pageSpacing = spacing.lg,
            verticalAlignment = Alignment.Top,
        ) { page ->
            val question = practice[page]

            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                Text(
                    text = question.stem,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spotlight(lit = step.spot == TourSpot.OPTIONS) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        question.options.forEachIndexed { option, text ->
                            OptionRow(
                                letter = LETTERS[option].toString(),
                                text = text,
                                selected = picked[question.id] == option,
                                onClick = {
                                    picked = picked + (question.id to option)
                                    if (TOUR_STEPS[at].waits == TourWait.ANSWER) advance()
                                },
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(top = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Spotlight(lit = step.spot == TourSpot.ACTS) {
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
            }
            Spotlight(lit = step.spot == TourSpot.DOUBT) {
                DoubtButton(
                    marked = marked,
                    onClick = {
                        marked = !marked
                        if (TOUR_STEPS[at].waits == TourWait.MARK) advance()
                    },
                )
            }
        }

        CoachCard(
            step = step,
            last = last,
            onAdvance = advance,
            onSkip = onSkip,
            modifier = Modifier.padding(top = spacing.md),
        )
    }
}

@Composable
private fun CoachCard(
    step: TourStep,
    last: Boolean,
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
        border = BorderStroke(HAIRLINE, scheme.outline),
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

            Column(
                modifier = Modifier.padding(top = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                if (step.waits != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = scheme.secondaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(spacing.md),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.tour_wait),
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSecondaryContainer,
                            )
                        }
                    }
                } else {
                    PrimaryButton(
                        text = stringResource(if (last) R.string.tour_start else R.string.tour_next),
                        onClick = onAdvance,
                    )
                }
                if (!last) {
                    TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(R.string.tour_skip))
                    }
                }
            }
        }
    }
}

@Composable
private fun Spotlight(
    lit: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dim by animateFloatAsState(targetValue = if (lit) 1f else DIMMED, label = "spotlight")

    Column(modifier = modifier.alpha(dim)) { content() }
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

private const val DIMMED = 0.3f
private val HAIRLINE = 1.dp
private val CARD_ELEVATION = 8.dp

@Preview(heightDp = 900)
@Composable
private fun QuizTourPreview() {
    BrainXPTheme {
        QuizTourScreen(onDone = {}, onSkip = {})
    }
}

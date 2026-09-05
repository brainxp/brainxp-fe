package com.example.brainxp.feature.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.core.upload.AnswerFlushScheduler
import com.example.brainxp.data.db.QuestionSessionDao
import com.example.brainxp.data.db.QuestionSessionEntity
import com.example.brainxp.data.prefs.DoubtStore
import com.example.brainxp.data.prefs.GuideStore
import com.example.brainxp.data.repo.AnswerQueue
import com.example.brainxp.data.repo.QueuedAnswer
import com.example.brainxp.data.repo.QuizRepository
import com.example.brainxp.domain.model.Question
import com.example.brainxp.domain.model.QuestionSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuizLoad(
    val quiz: QuizUiState? = null,
    val sessionId: String? = null,
    val loading: Boolean = true,
    val error: ApiError? = null,
    val tour: Boolean = false,
)

@HiltViewModel
class QuizViewModel
    @Inject
    constructor(
        private val quizzes: QuizRepository,
        private val answers: AnswerQueue,
        private val flushes: AnswerFlushScheduler,
        private val sessions: QuestionSessionDao,
        private val doubts: DoubtStore,
        private val guides: GuideStore,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(QuizLoad())
        val state: StateFlow<QuizLoad> = mutableState.asStateFlow()

        private var startedFor: String? = null
        private var essaySettle: Job? = null

        fun start(
            materialId: String,
            restart: Boolean = false,
        ) {
            if (startedFor == materialId && !restart) return
            startedFor = materialId
            mutableState.update { it.copy(loading = true, error = null) }
            viewModelScope.launch { open(materialId) }
        }

        fun onEvent(event: QuizEvent) {
            val quiz = mutableState.value.quiz ?: return
            when (event) {
                is QuizEvent.Jump -> {
                    settleEssay(quiz)
                    mutableState.put(quiz.copy(index = event.index))
                }

                is QuizEvent.Choose -> {
                    choose(quiz, event.questionId, event.option)
                }

                is QuizEvent.Draft -> {
                    draft(quiz, event.questionId, event.text)
                }

                is QuizEvent.ToggleDoubt -> {
                    toggleDoubt(quiz, event.questionId)
                }

                QuizEvent.Submit -> {
                    settleEssay(quiz)
                    mutableState.value.sessionId?.let { id -> viewModelScope.launch { doubts.forget(id) } }
                }
            }
        }

        fun showGuide(visible: Boolean) = mutableState.update { it.copy(tour = visible) }

        private suspend fun open(materialId: String) {
            val existing = sessions.findForMaterial(materialId).firstOrNull { it.status == STATUS_OPEN }
            val result = existing?.let { quizzes.session(it.id) } ?: quizzes.start(materialId)

            if (result is AppResult.Failure) {
                mutableState.update { it.copy(loading = false, error = result.error) }
                return
            }

            val session = (result as AppResult.Success).value
            val guided = guides.quizGuideSeen()
            if (!guided) guides.rememberQuizGuide()
            if (existing == null) {
                sessions.upsert(
                    QuestionSessionEntity(
                        id = session.sessionId,
                        materialId = materialId,
                        mode = session.mode.name,
                        status = STATUS_OPEN,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
            }

            mutableState.update {
                it.copy(
                    quiz = session.toUiState().copy(doubts = doubts.load(session.sessionId)),
                    sessionId = session.sessionId,
                    loading = false,
                    tour = !guided,
                )
            }
            answers.flush()
        }

        private fun choose(
            quiz: QuizUiState,
            questionId: String,
            option: Int,
        ) {
            val sessionId = mutableState.value.sessionId ?: return
            val next = quiz.withChoice(questionId, option)
            if (next == quiz) return
            mutableState.put(next.copy(pending = next.pending + questionId))
            send(
                QueuedAnswer(sessionId = sessionId, questionId = questionId, chosenIndex = option),
                questionId,
            )
        }

        private fun draft(
            quiz: QuizUiState,
            questionId: String,
            text: String,
        ) {
            mutableState.put(quiz.withDraft(questionId, text))

            essaySettle?.cancel()
            if (!essayLongEnough(text)) return
            essaySettle =
                viewModelScope.launch {
                    delay(ESSAY_SETTLE_MILLIS)
                    mutableState.value.quiz?.let { settled -> saveEssay(settled, questionId) }
                }
        }

        private fun settleEssay(quiz: QuizUiState) {
            val question = quiz.current ?: return
            if (!question.essay) return
            essaySettle?.cancel()
            saveEssay(quiz, question.id)
        }

        private fun saveEssay(
            quiz: QuizUiState,
            questionId: String,
        ) {
            val text = quiz.draftFor(questionId).trim()
            val sessionId = mutableState.value.sessionId ?: return
            if (!essayLongEnough(text) || quiz.answers[questionId] == text) return

            val next = quiz.withEssaySaved(questionId)
            mutableState.put(next.copy(pending = next.pending + questionId))
            send(
                QueuedAnswer(sessionId = sessionId, questionId = questionId, essayText = text),
                questionId,
            )
        }

        private fun toggleDoubt(
            quiz: QuizUiState,
            questionId: String,
        ) {
            val next = quiz.withDoubtToggled(questionId)
            mutableState.put(next)
            val sessionId = mutableState.value.sessionId ?: return
            viewModelScope.launch { doubts.save(sessionId, next.doubts) }
        }

        private fun send(
            queued: QueuedAnswer,
            questionId: String,
        ) {
            viewModelScope.launch {
                val settled = answers.send(queued)
                if (settled is AppResult.Failure && settled.error.retryable) {
                    flushes.schedule()
                    return@launch
                }
                mutableState.update { now ->
                    val current = now.quiz ?: return@update now
                    now.copy(quiz = current.copy(pending = current.pending - questionId))
                }
                if (settled is AppResult.Failure) {
                    mutableState.update { it.copy(error = settled.error) }
                }
            }
        }
    }

internal fun QuestionSession.toUiState(): QuizUiState {
    val shown = questions.mapNotNull(Question::toUiQuestion)
    val saved = answers.associateBy { it.questionId }
    val chosen = mutableMapOf<String, Int>()
    val drafts = mutableMapOf<String, String>()
    val settled = mutableMapOf<String, String>()

    shown.forEach { question ->
        val answer = saved[question.id]
        val picked = answer?.chosenIndex
        val option = picked?.let { question.options.getOrNull(it) }
        val essay = answer?.essayText.orEmpty()

        if (picked != null && option != null) {
            chosen[question.id] = picked
            settled[question.id] = option
        }
        if (essay.isNotEmpty()) {
            drafts[question.id] = essay
            if (essayLongEnough(essay)) settled[question.id] = essay.trim()
        }
        if (question.id in answeredIds) settled.putIfAbsent(question.id, question.id)
    }

    return QuizUiState(
        title = title ?: FALLBACK_TITLE,
        questions = shown,
        answers = settled,
        chosenByQuestion = chosen,
        draftByQuestion = drafts,
        index = shown.indexOfFirst { it.id !in settled }.coerceAtLeast(0),
    )
}

private fun Question.toUiQuestion(): QuizQuestion? =
    when (this) {
        is Question.MultipleChoice -> {
            QuizQuestion(
                id = id,
                stem = stem,
                difficulty = difficulty,
                factor = factor,
                options = options,
                sourceExcerpt = sourceExcerpt,
            )
        }

        is Question.ShortAnswer -> {
            QuizQuestion(
                id = id,
                stem = stem,
                difficulty = difficulty,
                factor = factor,
                sourceExcerpt = sourceExcerpt,
                rubricCriteria = rubricCriteria,
            )
        }

        else -> {
            null
        }
    }

private const val FALLBACK_TITLE = "Sesi belajar"
private const val STATUS_OPEN = "OPEN"
private const val ESSAY_SETTLE_MILLIS = 900L

private fun MutableStateFlow<QuizLoad>.put(quiz: QuizUiState) = update { it.copy(quiz = quiz) }

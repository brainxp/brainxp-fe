package com.example.brainxp.feature.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.core.upload.AnswerFlushScheduler
import com.example.brainxp.data.repo.AnswerQueue
import com.example.brainxp.data.repo.QueuedAnswer
import com.example.brainxp.data.repo.QuizRepository
import com.example.brainxp.domain.model.Question
import com.example.brainxp.domain.model.QuestionSession
import dagger.hilt.android.lifecycle.HiltViewModel
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
)

@HiltViewModel
class QuizViewModel
    @Inject
    constructor(
        private val quizzes: QuizRepository,
        private val answers: AnswerQueue,
        private val flushes: AnswerFlushScheduler,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(QuizLoad())
        val state: StateFlow<QuizLoad> = mutableState.asStateFlow()

        private var startedFor: String? = null

        fun start(materialId: String) {
            if (startedFor == materialId) return
            startedFor = materialId
            mutableState.update { it.copy(loading = true, error = null) }
            viewModelScope.launch {
                mutableState.update {
                    when (val result = quizzes.start(materialId)) {
                        is AppResult.Success -> {
                            it.copy(
                                quiz = result.value.toUiState(),
                                sessionId = result.value.sessionId,
                                loading = false,
                            )
                        }

                        is AppResult.Failure -> {
                            it.copy(loading = false, error = result.error)
                        }
                    }
                }
            }
        }

        fun retry() {
            val materialId = startedFor ?: return
            startedFor = null
            start(materialId)
        }

        fun apply(quiz: QuizUiState) = mutableState.update { it.copy(quiz = quiz) }

        fun save(quiz: QuizUiState) {
            val question = quiz.current ?: return
            val sessionId = mutableState.value.sessionId ?: return
            val queued =
                QueuedAnswer(
                    sessionId = sessionId,
                    questionId = question.id,
                    chosenIndex = if (question.essay) null else quiz.chosen,
                    essayText = if (question.essay) quiz.draft.trim() else null,
                )

            val advanced = quiz.recordAnswer()
            apply(advanced.copy(pending = advanced.pending + question.id))

            viewModelScope.launch {
                val settled = answers.send(queued)
                val queuedForLater = settled is AppResult.Failure && settled.error.retryable
                if (queuedForLater) flushes.schedule()
                if (!queuedForLater) {
                    mutableState.update { now ->
                        val current = now.quiz ?: return@update now
                        now.copy(quiz = current.copy(pending = current.pending - question.id))
                    }
                }
                if (settled is AppResult.Failure && !settled.error.retryable) {
                    mutableState.update { it.copy(error = settled.error) }
                }
            }
        }

        fun flushQueue() {
            viewModelScope.launch {
                if (answers.flush() == 0) return@launch
                mutableState.update { now ->
                    val current = now.quiz ?: return@update now
                    now.copy(quiz = current.copy(pending = emptySet()))
                }
            }
        }
    }

internal fun QuestionSession.toUiState(): QuizUiState =
    QuizUiState(
        title = title ?: FALLBACK_TITLE,
        questions = questions.mapNotNull(Question::toUiQuestion),
    )

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

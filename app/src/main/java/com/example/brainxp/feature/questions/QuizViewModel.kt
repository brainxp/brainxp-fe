package com.example.brainxp.feature.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
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

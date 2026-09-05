package com.example.brainxp.feature.questions

data class QuizQuestion(
    val id: String,
    val stem: String,
    val difficulty: String,
    val factor: Double,
    val options: List<String> = emptyList(),
    val sourceExcerpt: String? = null,
    val rubricCriteria: Int = 0,
) {
    val essay: Boolean get() = options.isEmpty()
}

enum class DotState {
    ANSWERED,
    DOUBTED,
    DOUBTED_ANSWERED,
    CURRENT,
    EMPTY,
}

data class QuizUiState(
    val title: String,
    val questions: List<QuizQuestion> = emptyList(),
    val index: Int = 0,
    val answers: Map<String, String> = emptyMap(),
    val chosenByQuestion: Map<String, Int> = emptyMap(),
    val draftByQuestion: Map<String, String> = emptyMap(),
    val pending: Set<String> = emptySet(),
    val doubts: Set<String> = emptySet(),
) {
    val current: QuizQuestion? get() = questions.getOrNull(index)

    val total: Int get() = questions.size

    val filled: Int get() = answers.size

    val complete: Boolean get() = total > 0 && filled == total

    val progress: Float get() = if (total == 0) 0f else filled.toFloat() / total

    val doubtedCount: Int get() = doubts.count { id -> questions.any { it.id == id } }

    fun chosenFor(questionId: String): Int? = chosenByQuestion[questionId]

    fun draftFor(questionId: String): String = draftByQuestion[questionId].orEmpty()

    fun dotState(position: Int): DotState {
        val question = questions.getOrNull(position) ?: return DotState.EMPTY
        val answered = question.id in answers
        return when {
            question.id in doubts && answered -> DotState.DOUBTED_ANSWERED
            question.id in doubts -> DotState.DOUBTED
            position == index -> DotState.CURRENT
            answered -> DotState.ANSWERED
            else -> DotState.EMPTY
        }
    }
}

fun QuizUiState.withChoice(
    questionId: String,
    option: Int,
): QuizUiState {
    val question = questions.firstOrNull { it.id == questionId } ?: return this
    val text = question.options.getOrNull(option) ?: return this
    return copy(
        chosenByQuestion = chosenByQuestion + (questionId to option),
        answers = answers + (questionId to text),
    )
}

fun QuizUiState.withDraft(
    questionId: String,
    text: String,
): QuizUiState = copy(draftByQuestion = draftByQuestion + (questionId to text))

fun QuizUiState.withEssaySaved(questionId: String): QuizUiState {
    val text = draftFor(questionId).trim()
    if (!essayLongEnough(text)) return this
    return copy(answers = answers + (questionId to text))
}

fun QuizUiState.withDoubtToggled(questionId: String): QuizUiState =
    copy(doubts = if (questionId in doubts) doubts - questionId else doubts + questionId)

fun essayLongEnough(text: String): Boolean = text.trim().length >= MIN_ESSAY_LENGTH

sealed interface QuizEvent {
    data class Jump(
        val index: Int,
    ) : QuizEvent

    data class Choose(
        val questionId: String,
        val option: Int,
    ) : QuizEvent

    data class Draft(
        val questionId: String,
        val text: String,
    ) : QuizEvent

    data class ToggleDoubt(
        val questionId: String,
    ) : QuizEvent

    data object Submit : QuizEvent
}

const val MIN_ESSAY_LENGTH = 8

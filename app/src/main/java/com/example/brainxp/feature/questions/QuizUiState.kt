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

data class QuizUiState(
    val title: String,
    val questions: List<QuizQuestion> = emptyList(),
    val index: Int = 0,
    val answers: Map<String, String> = emptyMap(),
    val chosen: Int? = null,
    val draft: String = "",
    val busy: Boolean = false,
    val pending: Set<String> = emptySet(),
) {
    val current: QuizQuestion? get() = questions.getOrNull(index)

    val total: Int get() = questions.size

    val filled: Int get() = answers.size

    val complete: Boolean get() = total > 0 && filled == total

    val answered: Boolean get() = current?.id in answers

    val progress: Float get() = if (total == 0) 0f else filled.toFloat() / total

    val canSave: Boolean
        get() {
            val question = current ?: return false
            return if (question.essay) {
                draft.trim().split(WHITESPACE).count { it.isNotEmpty() } >= MIN_ESSAY_WORDS
            } else {
                chosen != null
            }
        }
}

fun QuizUiState.recordAnswer(): QuizUiState {
    val question = current ?: return this
    val answer =
        if (question.essay) draft.trim() else question.options.getOrNull(chosen ?: -1).orEmpty()
    if (answer.isEmpty()) {
        return this
    }
    val nextIndex = (index + 1).coerceAtMost(questions.lastIndex)
    return copy(
        answers = answers + (question.id to answer),
        index = nextIndex,
        chosen = null,
        draft = "",
    )
}

sealed interface QuizEvent {
    data class Jump(
        val index: Int,
    ) : QuizEvent

    data class Choose(
        val option: Int,
    ) : QuizEvent

    data class Draft(
        val text: String,
    ) : QuizEvent

    data object Save : QuizEvent

    data object Submit : QuizEvent
}

private val WHITESPACE = "\\s+".toRegex()
private const val MIN_ESSAY_WORDS = 8

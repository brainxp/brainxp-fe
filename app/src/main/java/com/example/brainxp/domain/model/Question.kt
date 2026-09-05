package com.example.brainxp.domain.model

enum class SessionMode {
    NEW,
    REVIEW,
}

sealed interface Question {
    val id: String
    val conceptIds: List<String>

    data class MultipleChoice(
        override val id: String,
        override val conceptIds: List<String>,
        val stem: String,
        val options: List<String>,
        val difficulty: String = "",
        val factor: Double = 1.0,
        val sourceExcerpt: String? = null,
    ) : Question

    data class TrueFalse(
        override val id: String,
        override val conceptIds: List<String>,
        val stem: String,
    ) : Question

    data class ShortAnswer(
        override val id: String,
        override val conceptIds: List<String>,
        val stem: String,
        val difficulty: String = "",
        val factor: Double = 1.0,
        val sourceExcerpt: String? = null,
        val rubricCriteria: Int = 0,
    ) : Question

    data class Unsupported(
        override val id: String,
        override val conceptIds: List<String>,
        val rawType: String,
    ) : Question
}

data class QuestionSession(
    val sessionId: String,
    val title: String? = null,
    val materialId: String,
    val mode: SessionMode,
    val questions: List<Question>,
    val createdAt: Long,
)

enum class GenerationStatus {
    PENDING,
    READY,
    FAILED,
}

data class GenerationJob(
    val jobId: String,
    val status: GenerationStatus,
    val sessionId: String?,
    val error: String?,
)

data class AnswerSaved(
    val questionId: String,
    val answeredCount: Int,
    val totalCount: Int,
)

data class ConceptCoverage(
    val conceptId: String,
    val label: String,
    val correct: Int,
    val total: Int,
)

data class SessionResult(
    val sessionId: String,
    val score: Double,
    val rewardSeconds: Int,
    val rewardGranted: Boolean,
    val reason: String?,
    val coverage: List<ConceptCoverage>,
)

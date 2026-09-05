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

data class SavedAnswer(
    val questionId: String,
    val chosenIndex: Int? = null,
    val essayText: String? = null,
)

data class QuestionSession(
    val sessionId: String,
    val title: String? = null,
    val materialId: String,
    val mode: SessionMode,
    val questions: List<Question>,
    val createdAt: Long,
    val answeredIds: Set<String> = emptySet(),
    val answers: List<SavedAnswer> = emptyList(),
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

data class ReceiptLine(
    val ordinal: Int,
    val label: String,
    val difficulty: String,
    val multiplier: Double,
    val rewardSeconds: Int,
    val voided: Boolean,
    val voidReason: String?,
    val explanation: String?,
)

data class SessionReceipt(
    val sessionId: String,
    val title: String?,
    val baseRewardSeconds: Int,
    val lines: List<ReceiptLine>,
    val subtotalSeconds: Int,
    val levelFactor: Double,
    val levelNote: String,
    val noveltyFactor: Double,
    val noveltyNote: String,
    val creditedSeconds: Int,
    val balanceSeconds: Int,
    val correctCount: Int,
    val questionCount: Int,
    val streakCurrent: Int,
    val newBadges: List<String>,
)

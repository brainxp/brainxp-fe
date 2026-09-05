package com.example.brainxp.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuestionDto(
    val id: String,
    val ordinal: Int,
    val qtype: String,
    val stem: String,
    val difficulty: String,
    @SerialName("bloom_level") val bloomLevel: String,
    @SerialName("source_excerpt") val sourceExcerpt: String,
    @SerialName("type_factor") val typeFactor: Double,
    @SerialName("difficulty_factor") val difficultyFactor: Double,
    val options: List<String>? = null,
    @SerialName("rubric_criteria") val rubricCriteria: List<String>? = null,
)

@Serializable
data class QuizDto(
    @SerialName("session_id") val sessionId: String,
    @SerialName("material_id") val materialId: String,
    val title: String? = null,
    @SerialName("base_reward_seconds") val baseRewardSeconds: Int,
    @SerialName("level_factor") val levelFactor: Double,
    @SerialName("novelty_factor") val noveltyFactor: Double,
    @SerialName("max_reward_seconds") val maxRewardSeconds: Int,
    @SerialName("ready_count") val readyCount: Int,
    @SerialName("total_count") val totalCount: Int,
    val status: String,
    val questions: List<QuestionDto> = emptyList(),
    @SerialName("answered_ids") val answeredIds: List<String> = emptyList(),
    val answers: List<AnswerStateDto> = emptyList(),
)

@Serializable
data class AnswerStateDto(
    @SerialName("question_id") val questionId: String,
    @SerialName("chosen_index") val chosenIndex: Int? = null,
    @SerialName("essay_text") val essayText: String? = null,
)

@Serializable
data class AnswerRequestDto(
    @SerialName("question_id") val questionId: String,
    @SerialName("chosen_index") val chosenIndex: Int? = null,
    @SerialName("essay_text") val essayText: String? = null,
)

@Serializable
data class AnswerSavedDto(
    @SerialName("question_id") val questionId: String,
    @SerialName("answered_count") val answeredCount: Int,
    @SerialName("total_count") val totalCount: Int,
)

@Serializable
data class QuizStartDto(
    @SerialName("material_id") val materialId: String,
)

@Serializable
data class ReceiptRowDto(
    val ordinal: Int,
    val label: String,
    val qtype: String,
    val difficulty: String,
    val multiplier: Double,
    @SerialName("reward_seconds") val rewardSeconds: Double,
    val voided: Boolean = false,
    @SerialName("void_reason") val voidReason: String? = null,
    val explanation: String? = null,
    val score: Double? = null,
)

@Serializable
data class ReceiptDto(
    @SerialName("session_id") val sessionId: String,
    val title: String? = null,
    @SerialName("base_reward_seconds") val baseRewardSeconds: Int,
    val rows: List<ReceiptRowDto> = emptyList(),
    @SerialName("subtotal_seconds") val subtotalSeconds: Double,
    @SerialName("level_factor") val levelFactor: Double,
    @SerialName("level_note") val levelNote: String,
    @SerialName("novelty_factor") val noveltyFactor: Double,
    @SerialName("novelty_note") val noveltyNote: String,
    @SerialName("gross_seconds") val grossSeconds: Double,
    @SerialName("credited_seconds") val creditedSeconds: Int,
    @SerialName("balance_seconds") val balanceSeconds: Int,
    @SerialName("correct_count") val correctCount: Int,
    @SerialName("question_count") val questionCount: Int,
    @SerialName("streak_current") val streakCurrent: Int,
    @SerialName("new_badges") val newBadges: List<String> = emptyList(),
)

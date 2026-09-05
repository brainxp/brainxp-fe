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

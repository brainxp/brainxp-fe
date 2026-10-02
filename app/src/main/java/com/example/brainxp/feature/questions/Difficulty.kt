package com.example.brainxp.feature.questions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.brainxp.R

enum class Difficulty(
    val wire: String,
) {
    EASY("mudah"),
    MEDIUM("sedang"),
    HARD("sulit"),
}

fun difficultyOf(raw: String): Difficulty? {
    val word = raw.trim().lowercase()
    return Difficulty.entries.firstOrNull { it.wire == word }
}

enum class QuestionType(
    val wire: String,
) {
    MULTIPLE_CHOICE("mcq"),
    ESSAY("essay"),
}

fun questionTypeOf(raw: String): QuestionType? {
    val word = raw.trim().lowercase()
    return QuestionType.entries.firstOrNull { it.wire == word }
}

@Composable
fun questionTypeLabel(raw: String): String? =
    when (questionTypeOf(raw)) {
        QuestionType.MULTIPLE_CHOICE -> stringResource(R.string.question_type_multiple_choice)
        QuestionType.ESSAY -> stringResource(R.string.question_type_essay)
        null -> null
    }

@Composable
fun difficultyLabel(raw: String): String =
    when (difficultyOf(raw)) {
        Difficulty.EASY -> stringResource(R.string.difficulty_easy)
        Difficulty.MEDIUM -> stringResource(R.string.difficulty_medium)
        Difficulty.HARD -> stringResource(R.string.difficulty_hard)
        null -> raw.trim()
    }

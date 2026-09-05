package com.example.brainxp.data.repo

import com.example.brainxp.core.network.QuestionDto
import com.example.brainxp.core.network.QuizDto
import com.example.brainxp.domain.model.Question
import com.example.brainxp.domain.model.QuestionSession
import com.example.brainxp.domain.model.SessionMode

const val QTYPE_MCQ = "mcq"
const val QTYPE_ESSAY = "essay"

fun QuestionDto.toQuestion(): Question =
    when (qtype) {
        QTYPE_MCQ -> {
            Question.MultipleChoice(
                id = id,
                conceptIds = emptyList(),
                stem = stem,
                options = options.orEmpty(),
            )
        }

        QTYPE_ESSAY -> {
            Question.ShortAnswer(
                id = id,
                conceptIds = emptyList(),
                stem = stem,
            )
        }

        else -> {
            Question.Unsupported(
                id = id,
                conceptIds = emptyList(),
                rawType = qtype,
            )
        }
    }

fun QuizDto.toSession(): QuestionSession =
    QuestionSession(
        sessionId = sessionId,
        materialId = materialId,
        mode = SessionMode.NEW,
        questions = questions.sortedBy { it.ordinal }.map { it.toQuestion() },
        createdAt = 0L,
    )

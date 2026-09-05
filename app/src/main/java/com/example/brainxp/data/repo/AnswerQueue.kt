package com.example.brainxp.data.repo

import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.db.PendingOperationDao
import com.example.brainxp.data.db.PendingOperationEntity
import com.example.brainxp.data.db.PendingOperationType
import com.example.brainxp.domain.model.AnswerSaved
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class QueuedAnswer(
    val sessionId: String,
    val questionId: String,
    val chosenIndex: Int? = null,
    val essayText: String? = null,
)

@Singleton
class AnswerQueue
    @Inject
    constructor(
        private val quizzes: QuizRepository,
        private val pending: PendingOperationDao,
    ) {
        suspend fun send(answer: QueuedAnswer): AppResult<AnswerSaved> {
            val result = quizzes.answer(answer)
            if (result is AppResult.Failure && result.error.retryable) {
                enqueue(answer)
            }
            return result
        }

        suspend fun flush(): Int {
            val now = System.currentTimeMillis()
            val ready = pending.findReadyForRetryByType(PendingOperationType.SUBMIT_ANSWER, now)
            var sent = 0
            ready.forEach { row ->
                val queued = runCatching { json.decodeFromString<QueuedAnswer>(row.payload) }.getOrNull()
                if (queued == null) {
                    pending.deleteById(row.id)
                    return@forEach
                }
                when (val result = quizzes.answer(queued)) {
                    is AppResult.Success -> {
                        pending.deleteById(row.id)
                        sent++
                    }

                    is AppResult.Failure -> {
                        if (result.error.retryable) {
                            pending.recordAttempt(row.id, now + RETRY_DELAY_MILLIS)
                        } else {
                            pending.deleteById(row.id)
                        }
                    }
                }
            }
            pending.deleteExhausted(MAX_ATTEMPTS)
            return sent
        }

        suspend fun queuedCount(): Int =
            pending
                .findReadyForRetryByType(PendingOperationType.SUBMIT_ANSWER, Long.MAX_VALUE)
                .size

        private suspend fun enqueue(answer: QueuedAnswer) {
            val now = System.currentTimeMillis()
            pending.insert(
                PendingOperationEntity(
                    type = PendingOperationType.SUBMIT_ANSWER,
                    payload = json.encodeToString(answer),
                    nextAttemptAt = now,
                    createdAt = now,
                ),
            )
        }

        private companion object {
            val json = Json { ignoreUnknownKeys = true }
            const val RETRY_DELAY_MILLIS = 15_000L
            const val MAX_ATTEMPTS = 8
        }
    }

internal fun unsupportedAnswer(): AppResult<AnswerSaved> = AppResult.Failure(ApiError.Validation(field = "answer", message = null))

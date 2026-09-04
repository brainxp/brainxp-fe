package com.example.brainxp.data.repo.fake

import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.repo.SessionRepository
import com.example.brainxp.domain.model.AnswerVerdict
import com.example.brainxp.domain.model.ConceptCoverage
import com.example.brainxp.domain.model.GenerationJob
import com.example.brainxp.domain.model.GenerationStatus
import com.example.brainxp.domain.model.Question
import com.example.brainxp.domain.model.QuestionSession
import com.example.brainxp.domain.model.SessionMode
import com.example.brainxp.domain.model.SessionResult
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeSessionRepository
    @Inject
    constructor(
        private val backend: FakeBackend,
    ) : SessionRepository {
        private class Job(
            val materialId: String,
            val mode: SessionMode,
            var polls: Int = 0,
        )

        private val jobs = ConcurrentHashMap<String, Job>()
        private val sessions = ConcurrentHashMap<String, QuestionSession>()
        private val answers = ConcurrentHashMap<String, MutableMap<String, String>>()

        var pollsBeforeReady: Int = DEFAULT_POLLS_BEFORE_READY

        override suspend fun requestGeneration(
            materialId: String,
            questionCount: Int,
            mode: SessionMode,
        ): AppResult<GenerationJob> =
            backend.respond(FakeBackend.SESSION_REQUEST) {
                val jobId = "job-${UUID.randomUUID().toString().take(ID_LENGTH)}"
                jobs[jobId] = Job(materialId, mode)
                GenerationJob(jobId, GenerationStatus.PENDING, sessionId = null, error = null)
            }

        override suspend fun pollGeneration(jobId: String): AppResult<GenerationJob> {
            val job =
                jobs[jobId]
                    ?: return AppResult.Failure(ApiError.Unknown(NOT_FOUND, "job $jobId"))

            return backend.respond(FakeBackend.SESSION_POLL) {
                job.polls++
                if (job.polls < pollsBeforeReady) {
                    GenerationJob(jobId, GenerationStatus.PENDING, null, null)
                } else {
                    val sessionId = "ses-${UUID.randomUUID().toString().take(ID_LENGTH)}"
                    sessions[sessionId] =
                        QuestionSession(
                            sessionId = sessionId,
                            materialId = job.materialId,
                            mode = job.mode,
                            questions = FakeData.questions(),
                            createdAt = FakeData.now,
                        )
                    GenerationJob(jobId, GenerationStatus.READY, sessionId, null)
                }
            }
        }

        override suspend fun session(sessionId: String): AppResult<QuestionSession> {
            val session = sessions[sessionId] ?: seedSession(sessionId)
            return backend.respond(FakeBackend.SESSION_GET) { session }
        }

        override suspend fun submitAnswer(
            sessionId: String,
            questionId: String,
            answer: String,
            clientTimestamp: Long,
        ): AppResult<AnswerVerdict> =
            backend.respond(FakeBackend.SESSION_ANSWER) {
                answers.getOrPut(sessionId) { ConcurrentHashMap() }[questionId] = answer
                val question = sessions[sessionId]?.questions?.firstOrNull { it.id == questionId }
                val correct = gradeOnServerSide(questionId, answer)
                AnswerVerdict(
                    questionId = questionId,
                    correct = correct,
                    explanation = if (correct) EXPLANATION_CORRECT else EXPLANATION_WRONG,
                    conceptIds = question?.conceptIds ?: emptyList(),
                )
            }

        override suspend fun complete(sessionId: String): AppResult<SessionResult> =
            backend.respond(FakeBackend.SESSION_COMPLETE) {
                val given = answers[sessionId].orEmpty()
                val graded = given.count { gradeOnServerSide(it.key, it.value) }
                val total = sessions[sessionId]?.questions?.size ?: given.size
                val score = if (total == 0) 0.0 else graded.toDouble() / total
                val granted = score >= GRANT_THRESHOLD

                SessionResult(
                    sessionId = sessionId,
                    score = score,
                    rewardSeconds = if (granted) REWARD_SECONDS else 0,
                    rewardGranted = granted,
                    reason = if (granted) null else REASON_TOO_LOW,
                    coverage =
                        listOf(
                            ConceptCoverage("kinematika", "Kinematika", graded, total),
                            ConceptCoverage("percepatan", "Percepatan", 0, 1),
                        ),
                )
            }

        private fun seedSession(sessionId: String): QuestionSession {
            val seeded =
                QuestionSession(
                    sessionId = sessionId,
                    materialId = "mat-1",
                    mode = SessionMode.NEW,
                    questions = FakeData.questions(),
                    createdAt = FakeData.now,
                )
            sessions[sessionId] = seeded
            return seeded
        }

        private fun gradeOnServerSide(
            questionId: String,
            answer: String,
        ): Boolean =
            when (questionId) {
                "q-1" -> answer == "120 km"
                "q-2" -> answer.equals("false", ignoreCase = true)
                "q-3" -> answer.contains("m/s", ignoreCase = true)
                else -> false
            }

        private companion object {
            const val DEFAULT_POLLS_BEFORE_READY = 3
            const val ID_LENGTH = 6
            const val NOT_FOUND = 404
            const val GRANT_THRESHOLD = 0.5
            const val REWARD_SECONDS = 900
            const val EXPLANATION_CORRECT = "Tepat. Jarak = kecepatan x waktu."
            const val EXPLANATION_WRONG = "Belum tepat. Coba tinjau ulang rumusnya."
            const val REASON_TOO_LOW = "Skor di bawah ambang minimum untuk mendapat waktu."
        }
    }

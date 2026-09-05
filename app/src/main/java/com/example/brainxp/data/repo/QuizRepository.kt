package com.example.brainxp.data.repo

import com.example.brainxp.core.network.ErrorMapper
import com.example.brainxp.core.network.QuizApi
import com.example.brainxp.core.network.QuizStartDto
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.core.result.map
import com.example.brainxp.data.prefs.AuthDataStore
import com.example.brainxp.domain.model.QuestionSession
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuizRepository
    @Inject
    constructor(
        private val api: QuizApi,
        private val auth: AuthDataStore,
        private val errors: ErrorMapper,
    ) {
        suspend fun start(materialId: String): AppResult<QuestionSession> {
            val subject = auth.current().subjectId ?: return AppResult.Failure(ApiError.Unauthorized)
            return call { api.start(subject, QuizStartDto(materialId = materialId)) }.map { it.toSession() }
        }

        suspend fun session(sessionId: String): AppResult<QuestionSession> = call { api.quiz(sessionId) }.map { it.toSession() }

        private suspend fun <T> call(block: suspend () -> T): AppResult<T> =
            runCatching { block() }
                .fold(
                    onSuccess = { AppResult.Success(it) },
                    onFailure = { AppResult.Failure(errors.map(it)) },
                )
    }

package com.example.brainxp.data.repo

import com.example.brainxp.core.network.ErrorMapper
import com.example.brainxp.core.network.PolicyApi
import com.example.brainxp.core.network.PolicyPatchDto
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.core.result.map
import com.example.brainxp.data.prefs.AuthDataStore
import com.example.brainxp.domain.model.AcademicLevel
import javax.inject.Inject
import javax.inject.Singleton

data class SubjectPolicy(
    val level: AcademicLevel?,
    val language: String,
    val questionsPerSession: Int,
    val dayResetHour: Int,
    val idleDaysAllowed: Int,
    val pendingWeakenAt: String?,
)

data class PolicyChange(
    val applied: Boolean,
    val pendingUntil: String?,
    val message: String?,
)

@Singleton
class PolicyRepository
    @Inject
    constructor(
        private val api: PolicyApi,
        private val auth: AuthDataStore,
        private val errors: ErrorMapper,
    ) {
        suspend fun policy(): AppResult<SubjectPolicy> {
            val subject = auth.current().subjectId ?: return AppResult.Failure(ApiError.Unauthorized)
            return call { api.policy(subject) }.map { dto ->
                SubjectPolicy(
                    level = AcademicLevel.fromWire(dto.academicLevel),
                    language = dto.questionLanguage.orEmpty(),
                    questionsPerSession = dto.questionsPerSession,
                    dayResetHour = dto.dayResetHour,
                    idleDaysAllowed = dto.idleDaysAllowed,
                    pendingWeakenAt = dto.pendingWeakenAt,
                )
            }
        }

        suspend fun setLevel(level: AcademicLevel): AppResult<PolicyChange> = patch(PolicyPatchDto(academicLevel = level.wire))

        suspend fun setLanguage(language: String): AppResult<PolicyChange> = patch(PolicyPatchDto(questionLanguage = language))

        suspend fun setQuestionsPerSession(count: Int): AppResult<PolicyChange> = patch(PolicyPatchDto(questionsPerSession = count))

        private suspend fun patch(body: PolicyPatchDto): AppResult<PolicyChange> {
            val subject = auth.current().subjectId ?: return AppResult.Failure(ApiError.Unauthorized)
            return call { api.update(subject, body) }
                .map { PolicyChange(applied = it.applied, pendingUntil = it.pendingUntil, message = it.message) }
        }

        private suspend fun <T> call(block: suspend () -> T): AppResult<T> =
            runCatching { block() }
                .fold(
                    onSuccess = { AppResult.Success(it) },
                    onFailure = { AppResult.Failure(errors.map(it)) },
                )
    }

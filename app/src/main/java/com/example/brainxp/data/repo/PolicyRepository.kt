package com.example.brainxp.data.repo

import com.example.brainxp.core.network.ErrorMapper
import com.example.brainxp.core.network.PolicyApi
import com.example.brainxp.core.network.PolicyDto
import com.example.brainxp.core.network.PolicyPatchDto
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.core.result.map
import com.example.brainxp.data.prefs.AuthDataStore
import com.example.brainxp.domain.model.AcademicLevel
import com.example.brainxp.domain.model.PolicyDraft
import com.example.brainxp.domain.model.UploadMethod
import javax.inject.Inject
import javax.inject.Singleton

data class SubjectPolicy(
    val level: AcademicLevel?,
    val language: String,
    val questionsPerSession: Int,
    val dayResetHour: Int,
    val idleDaysAllowed: Int,
    val pendingWeakenAt: String?,
    val essayCount: Int,
    val dailyCapSeconds: List<Int>,
    val dailyGrantSeconds: List<Int>,
    val lockedApps: List<String>,
    val baseRewardSeconds: Int,
    val uploadMethods: Set<UploadMethod>,
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
        suspend fun policy(subjectId: String? = null): AppResult<SubjectPolicy> {
            val subject = subjectId ?: auth.current().subjectId ?: return AppResult.Failure(ApiError.Unauthorized)
            return call { api.policy(subject) }.map(PolicyDto::toSubjectPolicy)
        }

        suspend fun save(
            draft: PolicyDraft,
            subjectId: String? = null,
        ): AppResult<PolicyChange> = patch(draft.toPatch(), subjectId)

        suspend fun setLevel(
            level: AcademicLevel,
            subjectId: String? = null,
        ): AppResult<PolicyChange> = patch(PolicyPatchDto(academicLevel = level.wire), subjectId)

        suspend fun setLanguage(
            language: String,
            subjectId: String? = null,
        ): AppResult<PolicyChange> = patch(PolicyPatchDto(questionLanguage = language), subjectId)

        private suspend fun patch(
            body: PolicyPatchDto,
            subjectId: String?,
        ): AppResult<PolicyChange> {
            val subject = subjectId ?: auth.current().subjectId ?: return AppResult.Failure(ApiError.Unauthorized)
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

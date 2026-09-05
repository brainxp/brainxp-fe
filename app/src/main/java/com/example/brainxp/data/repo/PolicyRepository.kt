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
        suspend fun level(): AppResult<AcademicLevel?> {
            val subject = auth.current().subjectId ?: return AppResult.Failure(ApiError.Unauthorized)
            return call { api.policy(subject) }.map { AcademicLevel.fromWire(it.academicLevel) }
        }

        suspend fun setLevel(level: AcademicLevel): AppResult<PolicyChange> {
            val subject = auth.current().subjectId ?: return AppResult.Failure(ApiError.Unauthorized)
            return call { api.update(subject, PolicyPatchDto(academicLevel = level.wire)) }
                .map { PolicyChange(applied = it.applied, pendingUntil = it.pendingUntil, message = it.message) }
        }

        private suspend fun <T> call(block: suspend () -> T): AppResult<T> =
            runCatching { block() }
                .fold(
                    onSuccess = { AppResult.Success(it) },
                    onFailure = { AppResult.Failure(errors.map(it)) },
                )
    }

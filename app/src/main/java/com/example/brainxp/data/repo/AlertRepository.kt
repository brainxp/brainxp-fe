package com.example.brainxp.data.repo

import com.example.brainxp.core.network.AlertApi
import com.example.brainxp.core.network.AlertDto
import com.example.brainxp.core.network.ErrorMapper
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.domain.model.GuardianAlert
import com.example.brainxp.domain.model.alertKindOf
import javax.inject.Inject
import javax.inject.Singleton

interface AlertRepository {
    suspend fun openAlerts(): AppResult<List<GuardianAlert>>
}

@Singleton
class NetworkAlertRepository
    @Inject
    constructor(
        private val api: AlertApi,
        private val errors: ErrorMapper,
    ) : AlertRepository {
        override suspend fun openAlerts(): AppResult<List<GuardianAlert>> = call { api.alerts(OPEN).map(AlertDto::toAlert) }

        private suspend fun <T> call(block: suspend () -> T): AppResult<T> =
            runCatching { block() }
                .fold(
                    onSuccess = { AppResult.Success(it) },
                    onFailure = { AppResult.Failure(errors.map(it)) },
                )

        private companion object {
            const val OPEN = "open"
        }
    }

private fun AlertDto.toAlert(): GuardianAlert =
    GuardianAlert(
        id = id,
        subjectId = subjectId,
        subjectName = subjectName,
        kind = alertKindOf(kind),
        detail = detail,
        acknowledged = acknowledgedAt != null,
    )

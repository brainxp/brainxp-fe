package com.example.brainxp.core.upload

import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.repo.MaterialRepository
import com.example.brainxp.di.AppScope
import com.example.brainxp.domain.model.Material
import com.example.brainxp.domain.model.MaterialStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

const val FIRST_DELAY_MILLIS = 1_000L
const val MAX_DELAY_MILLIS = 5_000L
const val PREPARATION_BUDGET_MILLIS = 90_000L
private const val GROWTH_NUMERATOR = 3
private const val GROWTH_DENOMINATOR = 2

sealed interface PreparationState {
    data object Idle : PreparationState

    data class Working(
        val materialId: String,
        val waitedMillis: Long,
    ) : PreparationState

    data class Settled(
        val material: Material,
    ) : PreparationState

    data class Stalled(
        val materialId: String,
        val error: ApiError,
    ) : PreparationState
}

fun nextDelay(previous: Long): Long = (previous * GROWTH_NUMERATOR / GROWTH_DENOMINATOR).coerceAtMost(MAX_DELAY_MILLIS)

fun settled(status: MaterialStatus): Boolean = status == MaterialStatus.READY || status == MaterialStatus.FAILED

@Singleton
class MaterialPreparation
    @Inject
    constructor(
        private val materials: MaterialRepository,
        @AppScope private val scope: CoroutineScope,
    ) {
        private val mutableState = MutableStateFlow<PreparationState>(PreparationState.Idle)
        val state: StateFlow<PreparationState> = mutableState.asStateFlow()

        private var watcher: Job? = null

        fun watch(materialId: String) {
            if (watcher?.isActive == true && watchedId() == materialId) return
            watcher?.cancel()
            mutableState.value = PreparationState.Working(materialId, waitedMillis = 0L)
            watcher = scope.launch { follow(materialId) }
        }

        fun forget() {
            watcher?.cancel()
            watcher = null
            mutableState.value = PreparationState.Idle
        }

        private fun watchedId(): String? =
            when (val now = mutableState.value) {
                is PreparationState.Working -> now.materialId
                is PreparationState.Stalled -> now.materialId
                is PreparationState.Settled -> now.material.id
                PreparationState.Idle -> null
            }

        private suspend fun follow(materialId: String) {
            var waited = 0L
            var wait = FIRST_DELAY_MILLIS

            while (waited < PREPARATION_BUDGET_MILLIS) {
                delay(wait)
                waited += wait
                wait = nextDelay(wait)

                when (val result = materials.detail(materialId)) {
                    is AppResult.Success -> {
                        val material = result.value.material
                        if (settled(material.status)) {
                            mutableState.value = PreparationState.Settled(material)
                            return
                        }
                        mutableState.value = PreparationState.Working(materialId, waited)
                    }

                    is AppResult.Failure -> {
                        if (!result.error.retryable) {
                            mutableState.value = PreparationState.Stalled(materialId, result.error)
                            return
                        }
                        mutableState.value = PreparationState.Working(materialId, waited)
                    }
                }
            }

            mutableState.value = PreparationState.Stalled(materialId, ApiError.Timeout)
        }
    }

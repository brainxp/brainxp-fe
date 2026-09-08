package com.example.brainxp.core.upload

import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.repo.MaterialRepository
import com.example.brainxp.di.AppScope
import com.example.brainxp.domain.model.Material
import com.example.brainxp.domain.model.MaterialStatus
import kotlinx.coroutines.CancellationException
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
const val PREPARATION_BUDGET_MILLIS = 600_000L
private const val GROWTH_NUMERATOR = 3
private const val GROWTH_DENOMINATOR = 2

sealed interface PreparationState {
    data object Idle : PreparationState

    data class Working(
        val materialId: String,
        val name: String = "",
        val stage: PreparingStage = PreparingStage.READING,
        val ready: Int = 0,
        val total: Int = 0,
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
        private val stages: MaterialStageStream,
        @AppScope private val scope: CoroutineScope,
    ) {
        private val mutableState = MutableStateFlow<PreparationState>(PreparationState.Idle)
        val state: StateFlow<PreparationState> = mutableState.asStateFlow()

        private var watcher: Job? = null

        fun watch(
            materialId: String,
            name: String = "",
        ) {
            if (watcher?.isActive == true && watchedId() == materialId) return
            watcher?.cancel()
            mutableState.value = PreparationState.Working(materialId = materialId, name = name)
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
            if (streamed(materialId)) return
            poll(materialId)
        }

        private suspend fun streamed(materialId: String): Boolean {
            var reached = false
            val followed =
                runCatching {
                    stages.follow(materialId).collect { update ->
                        reached = reached || update.settled
                        advance(materialId, update)
                    }
                }
            followed.exceptionOrNull()?.let { failure ->
                if (failure is CancellationException) throw failure
                return false
            }
            return reached && settle(materialId)
        }

        private fun advance(
            materialId: String,
            update: StageUpdate,
        ) {
            if (update.settled) return
            mutableState.value =
                working(materialId).copy(stage = update.stage, ready = update.ready, total = update.total)
        }

        private fun working(materialId: String): PreparationState.Working =
            mutableState.value as? PreparationState.Working ?: PreparationState.Working(materialId)

        private suspend fun settle(materialId: String): Boolean {
            val result = materials.detail(materialId)
            if (result is AppResult.Success && settled(result.value.status)) {
                mutableState.value = PreparationState.Settled(result.value)
                return true
            }
            return false
        }

        private suspend fun poll(materialId: String) {
            var waited = 0L
            var wait = FIRST_DELAY_MILLIS

            while (waited < PREPARATION_BUDGET_MILLIS) {
                delay(wait)
                waited += wait
                wait = nextDelay(wait)

                when (val result = materials.detail(materialId)) {
                    is AppResult.Success -> {
                        val material = result.value
                        if (settled(material.status)) {
                            mutableState.value = PreparationState.Settled(material)
                            return
                        }
                        mutableState.value =
                            working(materialId).copy(
                                stage = PreparingStage.VALIDATING,
                                ready = material.questionCount,
                            )
                    }

                    is AppResult.Failure -> {
                        if (!result.error.retryable) {
                            mutableState.value = PreparationState.Stalled(materialId, result.error)
                            return
                        }
                    }
                }
            }

            mutableState.value = PreparationState.Stalled(materialId, ApiError.Timeout)
        }
    }

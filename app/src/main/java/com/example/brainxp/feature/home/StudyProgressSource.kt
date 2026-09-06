package com.example.brainxp.feature.home

import com.example.brainxp.core.upload.MaterialPreparation
import com.example.brainxp.core.upload.PreparationState
import com.example.brainxp.data.repo.MaterialRepository
import com.example.brainxp.domain.model.Material
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

data class PreparingRow(
    val name: String,
    val ready: Int,
    val total: Int,
)

data class StudyProgress(
    val pending: PendingSession? = null,
    val preparing: PreparingRow? = null,
)

@Singleton
class StudyProgressSource
    @Inject
    constructor(
        private val materials: MaterialRepository,
        private val preparation: MaterialPreparation,
    ) {
        fun observe(): Flow<StudyProgress> =
            combine(materials.observeCached(), preparation.state) { cached, prep ->
                StudyProgress(pending = cached.pendingSession(), preparing = prep.row())
            }

        suspend fun refresh() {
            materials.page(cursor = null)
        }
    }

private fun List<Material>.pendingSession(): PendingSession? =
    firstNotNullOfOrNull { material ->
        material.unfinished?.let { open ->
            PendingSession(
                materialId = material.id,
                title = material.title,
                answered = open.answered,
                total = open.total,
            )
        }
    }

private fun PreparationState.row(): PreparingRow? =
    when (this) {
        is PreparationState.Working -> PreparingRow(name = name, ready = ready, total = total)
        is PreparationState.Stalled -> PreparingRow(name = "", ready = 0, total = 0)
        is PreparationState.Settled -> null
        PreparationState.Idle -> null
    }

package com.example.brainxp.data.repo

import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.db.UnlockSessionDao
import com.example.brainxp.data.db.UnlockSessionEntity
import com.example.brainxp.data.db.UnlockStatus
import com.example.brainxp.domain.model.UnlockState
import javax.inject.Inject
import javax.inject.Singleton

interface UnlockRepository {
    suspend fun loadActive(): UnlockState.Active?

    suspend fun save(
        state: UnlockState.Active,
        status: UnlockStatus,
    ): AppResult<Unit>
}

@Singleton
class RoomUnlockRepository
    @Inject
    constructor(
        private val dao: UnlockSessionDao,
    ) : UnlockRepository {
        override suspend fun loadActive(): UnlockState.Active? {
            val row = dao.findActive() ?: return null
            return UnlockState.Active(
                unlockId = row.id,
                budgetMillis = row.budgetMillis,
                consumedByPackage = row.consumedByPackage,
                allowedPackages = row.allowedPackages.toSet(),
            )
        }

        override suspend fun save(
            state: UnlockState.Active,
            status: UnlockStatus,
        ): AppResult<Unit> {
            dao.upsert(
                UnlockSessionEntity(
                    id = state.unlockId,
                    budgetMillis = state.budgetMillis,
                    consumedByPackage = state.consumedByPackage,
                    allowedPackages = state.allowedPackages.toList(),
                    status = status,
                ),
            )
            return AppResult.Success(Unit)
        }
    }

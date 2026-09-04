package com.example.brainxp.data.repo

import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.db.UnlockSessionDao
import com.example.brainxp.data.db.UnlockSessionEntity
import com.example.brainxp.data.db.UnlockStatus
import com.example.brainxp.domain.model.UnlockState
import com.example.brainxp.domain.model.rebasedAfterBoot
import javax.inject.Inject
import javax.inject.Singleton

data class StoredUnlock(
    val state: UnlockState.Active,
    val bootWallClock: Long,
)

interface UnlockRepository {
    suspend fun activeUnlock(
        nowWallClock: Long,
        nowElapsed: Long,
    ): UnlockState

    suspend fun loadActive(): StoredUnlock?

    suspend fun save(
        state: UnlockState.Active,
        bootWallClock: Long,
    ): AppResult<Unit>

    suspend fun markEnded(
        unlockId: String,
        status: UnlockStatus,
    ): AppResult<Unit>
}

@Singleton
class RoomUnlockRepository
    @Inject
    constructor(
        private val dao: UnlockSessionDao,
    ) : UnlockRepository {
        override suspend fun activeUnlock(
            nowWallClock: Long,
            nowElapsed: Long,
        ): UnlockState {
            val stored = loadActive() ?: return UnlockState.Locked
            val rebased = stored.state.rebasedAfterBoot(nowWallClock, nowElapsed)
            if (rebased is UnlockState.Expired) {
                dao.updateStatus(stored.state.unlockId, UnlockStatus.EXPIRED)
            }
            return rebased
        }

        override suspend fun loadActive(): StoredUnlock? {
            val row = dao.findActive() ?: return null
            return StoredUnlock(
                state =
                    UnlockState.Active(
                        unlockId = row.id,
                        endAtElapsed = row.endAtElapsed,
                        endAtWallClock = row.endAtWallClock,
                        allowedPackages = row.allowedPackages.toSet(),
                    ),
                bootWallClock = row.bootWallClock,
            )
        }

        override suspend fun save(
            state: UnlockState.Active,
            bootWallClock: Long,
        ): AppResult<Unit> {
            dao.upsert(
                UnlockSessionEntity(
                    id = state.unlockId,
                    endAtElapsed = state.endAtElapsed,
                    endAtWallClock = state.endAtWallClock,
                    bootWallClock = bootWallClock,
                    allowedPackages = state.allowedPackages.toList(),
                    status = UnlockStatus.ACTIVE,
                ),
            )
            return AppResult.Success(Unit)
        }

        override suspend fun markEnded(
            unlockId: String,
            status: UnlockStatus,
        ): AppResult<Unit> {
            dao.updateStatus(unlockId, status)
            return AppResult.Success(Unit)
        }
    }

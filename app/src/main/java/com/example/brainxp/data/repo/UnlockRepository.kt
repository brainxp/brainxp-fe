package com.example.brainxp.data.repo

import com.example.brainxp.data.db.UnlockSessionDao
import com.example.brainxp.data.db.UnlockStatus
import com.example.brainxp.domain.model.UnlockState
import com.example.brainxp.domain.model.rebasedAfterBoot
import javax.inject.Inject
import javax.inject.Singleton

interface UnlockRepository {
    suspend fun activeUnlock(
        nowWallClock: Long,
        nowElapsed: Long,
    ): UnlockState
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
            val stored = dao.findActive() ?: return UnlockState.Locked
            val active =
                UnlockState.Active(
                    unlockId = stored.id,
                    endAtElapsed = stored.endAtElapsed,
                    endAtWallClock = stored.endAtWallClock,
                    allowedPackages = stored.allowedPackages.toSet(),
                )
            val rebased = active.rebasedAfterBoot(nowWallClock, nowElapsed)
            if (rebased is UnlockState.Expired) {
                dao.updateStatus(stored.id, UnlockStatus.EXPIRED)
            }
            return rebased
        }
    }

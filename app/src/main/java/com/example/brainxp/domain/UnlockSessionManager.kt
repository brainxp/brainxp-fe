package com.example.brainxp.domain

import com.example.brainxp.core.time.AppClock
import com.example.brainxp.data.db.UnlockStatus
import com.example.brainxp.data.repo.ActivityLogRepository
import com.example.brainxp.data.repo.UnlockRepository
import com.example.brainxp.domain.model.ActivityEvent
import com.example.brainxp.domain.model.ActivityKind
import com.example.brainxp.domain.model.UnlockState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class UnlockSessionManager
    @Inject
    constructor(
        private val clock: AppClock,
        private val repository: UnlockRepository,
        private val activityLog: ActivityLogRepository,
    ) {
        private val mutex = Mutex()
        private val mutableState = MutableStateFlow<UnlockState>(UnlockState.Locked)
        private var lastSeenElapsed: Long? = null
        private var bootWallClock: Long? = null

        val state: StateFlow<UnlockState> = mutableState.asStateFlow()

        fun remaining(): Duration {
            val active = mutableState.value as? UnlockState.Active ?: return Duration.ZERO
            return (active.endAtElapsed - clock.elapsedRealtime()).coerceAtLeast(0).milliseconds
        }

        fun allows(packageName: String): Boolean {
            val active = mutableState.value as? UnlockState.Active ?: return false
            return packageName in active.allowedPackages && clock.elapsedRealtime() < active.endAtElapsed
        }

        suspend fun start(
            durationSeconds: Int,
            allowedPackages: Set<String>,
        ): UnlockState =
            mutex.withLock {
                val elapsed = clock.elapsedRealtime()
                val wall = clock.wallClock()
                val active =
                    UnlockState.Active(
                        unlockId = UUID.randomUUID().toString(),
                        endAtElapsed = elapsed + durationSeconds * MILLIS_PER_SECOND,
                        endAtWallClock = wall + durationSeconds * MILLIS_PER_SECOND,
                        allowedPackages = allowedPackages,
                    )
                bootWallClock = wall - elapsed
                lastSeenElapsed = elapsed
                repository.save(active, requireNotNull(bootWallClock))
                activityLog.record(ActivityEvent(ActivityKind.UNLOCK_STARTED, wall))
                mutableState.value = active
                active
            }

        suspend fun endEarly(): UnlockState =
            mutex.withLock {
                finish(UnlockStatus.ENDED, ActivityKind.UNLOCK_ENDED)
            }

        suspend fun refresh(): UnlockState =
            mutex.withLock {
                val stored = repository.loadActive()
                if (stored == null) {
                    mutableState.value = UnlockState.Locked
                    return@withLock UnlockState.Locked
                }
                bootWallClock = stored.bootWallClock
                mutableState.value = stored.state
                lastSeenElapsed = null
                evaluateLocked()
            }

        suspend fun evaluate(): UnlockState = mutex.withLock { evaluateLocked() }

        private suspend fun evaluateLocked(): UnlockState {
            val active = mutableState.value as? UnlockState.Active ?: return mutableState.value
            val elapsed = clock.elapsedRealtime()
            val wall = clock.wallClock()
            val previous = lastSeenElapsed
            lastSeenElapsed = elapsed

            if (previous == null) {
                return recoverFromWallClock(active, elapsed, wall)
            }

            val byElapsed = active.endAtElapsed - elapsed
            val byWall = active.endAtWallClock - wall
            return when {
                abs(byElapsed - byWall) > TOLERANCE_MILLIS -> {
                    finish(UnlockStatus.ENDED, ActivityKind.PROTECTION_ANOMALY)
                }

                byElapsed <= 0 -> {
                    expire(active)
                }

                else -> {
                    active
                }
            }
        }

        private suspend fun recoverFromWallClock(
            active: UnlockState.Active,
            elapsed: Long,
            wall: Long,
        ): UnlockState {
            val derivedBoot = wall - elapsed
            val storedBoot = bootWallClock
            val tampered = storedBoot != null && derivedBoot < storedBoot - TOLERANCE_MILLIS
            val remainingByWall = active.endAtWallClock - wall

            return when {
                tampered -> {
                    finish(UnlockStatus.ENDED, ActivityKind.PROTECTION_ANOMALY)
                }

                remainingByWall <= 0 -> {
                    expire(active)
                }

                else -> {
                    val rebased = active.copy(endAtElapsed = elapsed + remainingByWall)
                    bootWallClock = derivedBoot
                    repository.save(rebased, derivedBoot)
                    mutableState.value = rebased
                    rebased
                }
            }
        }

        private suspend fun expire(active: UnlockState.Active): UnlockState {
            repository.markEnded(active.unlockId, UnlockStatus.EXPIRED)
            activityLog.record(ActivityEvent(ActivityKind.UNLOCK_ENDED, clock.wallClock()))
            mutableState.value = UnlockState.Expired
            return UnlockState.Expired
        }

        private suspend fun finish(
            status: UnlockStatus,
            kind: ActivityKind,
        ): UnlockState {
            val active = mutableState.value as? UnlockState.Active ?: return mutableState.value
            repository.markEnded(active.unlockId, status)
            activityLog.record(ActivityEvent(kind, clock.wallClock()))
            mutableState.value = UnlockState.Expired
            return UnlockState.Expired
        }

        private companion object {
            const val TOLERANCE_MILLIS = 60_000L
            const val MILLIS_PER_SECOND = 1_000L
        }
    }

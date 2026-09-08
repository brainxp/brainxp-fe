package com.example.brainxp.domain

import com.example.brainxp.core.time.AppClock
import com.example.brainxp.data.db.UnlockStatus
import com.example.brainxp.data.repo.ActivityLogRepository
import com.example.brainxp.data.repo.ConsumptionReporter
import com.example.brainxp.data.repo.UnlockRepository
import com.example.brainxp.domain.model.ActivityEvent
import com.example.brainxp.domain.model.ActivityKind
import com.example.brainxp.domain.model.UnlockState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Singleton
class UnlockSessionManager
    @Inject
    constructor(
        private val clock: AppClock,
        private val repository: UnlockRepository,
        private val activityLog: ActivityLogRepository,
        private val rewards: ConsumptionReporter,
    ) {
        private val mutex = Mutex()
        private val mutableState = MutableStateFlow<UnlockState>(UnlockState.Locked)
        private var lastMeteredElapsed: Long? = null
        private var persistedMillis = 0L

        val state: StateFlow<UnlockState> = mutableState.asStateFlow()

        val remainingFlow: Flow<Duration> =
            mutableState
                .map { ((it as? UnlockState.Active)?.remainingMillis ?: 0L) / MILLIS_PER_SECOND }
                .distinctUntilChanged()
                .map { it.seconds }

        fun remaining(): Duration = ((mutableState.value as? UnlockState.Active)?.remainingMillis ?: 0L).milliseconds

        suspend fun start(
            durationSeconds: Int,
            allowedPackages: Set<String>,
        ): UnlockState =
            mutex.withLock {
                val active =
                    UnlockState.Active(
                        unlockId = UUID.randomUUID().toString(),
                        budgetMillis = durationSeconds * MILLIS_PER_SECOND,
                        allowedPackages = allowedPackages,
                    )
                lastMeteredElapsed = null
                persistedMillis = 0L
                repository.save(active, UnlockStatus.ACTIVE)
                activityLog.record(ActivityEvent(ActivityKind.UNLOCK_STARTED, clock.wallClock()))
                mutableState.value = active
                active
            }

        suspend fun meter(foregroundPackage: String?): UnlockState =
            mutex.withLock {
                val active = mutableState.value as? UnlockState.Active ?: return@withLock mutableState.value
                val metered = foregroundPackage?.takeIf(active::meters)
                val elapsed = clock.elapsedRealtime()
                val previous = lastMeteredElapsed
                lastMeteredElapsed = if (metered == null) null else elapsed

                if (metered == null || previous == null) {
                    return@withLock active
                }

                val advanced = active.advanced(metered, (elapsed - previous).coerceIn(0, MAX_INCREMENT_MILLIS))
                mutableState.value = advanced
                when {
                    advanced.exhausted -> {
                        finish(UnlockStatus.EXPIRED, ActivityKind.UNLOCK_ENDED)
                    }

                    advanced.consumedMillis - persistedMillis >= PERSIST_STEP_MILLIS -> {
                        persistedMillis = advanced.consumedMillis
                        repository.save(advanced, UnlockStatus.ACTIVE)
                        advanced
                    }

                    else -> {
                        advanced
                    }
                }
            }

        suspend fun endEarly(): UnlockState = mutex.withLock { finish(UnlockStatus.ENDED, ActivityKind.UNLOCK_ENDED) }

        suspend fun refresh(): UnlockState =
            mutex.withLock {
                val stored = repository.loadActive()
                lastMeteredElapsed = null
                persistedMillis = stored?.consumedMillis ?: 0L
                mutableState.value = stored ?: UnlockState.Locked
                if (stored != null && stored.exhausted) {
                    finish(UnlockStatus.EXPIRED, ActivityKind.UNLOCK_ENDED)
                } else {
                    mutableState.value
                }
            }

        private suspend fun finish(
            status: UnlockStatus,
            kind: ActivityKind,
        ): UnlockState {
            val active = mutableState.value as? UnlockState.Active ?: return mutableState.value
            repository.save(active, status)
            activityLog.record(ActivityEvent(kind, clock.wallClock()))
            lastMeteredElapsed = null
            persistedMillis = 0L
            mutableState.value = UnlockState.Expired

            val consumed =
                active.consumedByPackage
                    .mapValues { (_, millis) -> (millis / MILLIS_PER_SECOND).toInt() }
                    .filterValues { it > 0 }
            if (consumed.isNotEmpty()) {
                rewards.report(consumed)
            }
            return UnlockState.Expired
        }

        private companion object {
            const val MILLIS_PER_SECOND = 1_000L
            const val MAX_INCREMENT_MILLIS = 5_000L
            const val PERSIST_STEP_MILLIS = 5_000L
        }
    }

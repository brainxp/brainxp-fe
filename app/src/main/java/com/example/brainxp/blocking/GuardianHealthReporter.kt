package com.example.brainxp.blocking

import com.example.brainxp.core.permission.SpecialPermission
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.core.time.AppClock
import com.example.brainxp.data.repo.ActivityLogRepository
import com.example.brainxp.data.repo.PendingSyncQueue
import com.example.brainxp.domain.model.ActivityEvent
import com.example.brainxp.domain.model.ActivityKind
import com.example.brainxp.domain.model.GuardianEvent
import com.example.brainxp.domain.model.GuardianStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GuardianHealthReporter
    @Inject
    constructor(
        private val activityLog: ActivityLogRepository,
        private val pendingSync: PendingSyncQueue,
        private val clock: AppClock,
    ) {
        private val mutex = Mutex()
        private var lastMissing: Set<SpecialPermission> = emptySet()
        private var lastStatus: ProtectionStatus? = null

        suspend fun report(
            status: ProtectionStatus,
            missing: List<SpecialPermission> = emptyList(),
        ) = mutex.withLock {
            if (status != lastStatus) {
                status.toActivityKind()?.let { kind ->
                    activityLog.record(ActivityEvent(kind = kind, timestamp = clock.wallClock()))
                }
            }
            val currentMissing = missing.toSet()
            val result = pendingSync.sendHealth(guardianStatusOf(status, missing), eventsFor(currentMissing))
            if (result is AppResult.Success || (result is AppResult.Failure && result.error.retryable)) {
                lastMissing = currentMissing
                lastStatus = status
            }
        }

        private fun eventsFor(missing: Set<SpecialPermission>): List<GuardianEvent> {
            val revoked = missing - lastMissing
            val restored = lastMissing - missing
            return revoked.map { permission -> event(GuardianEvent.REVOKED, permission, required = true) } +
                restored.map { permission -> event(GuardianEvent.RESTORED, permission) }
        }

        private fun event(
            type: String,
            permission: SpecialPermission,
            required: Boolean = false,
        ): GuardianEvent =
            GuardianEvent(
                type = type,
                permission = permission.name.lowercase(),
                required = required,
                occurredAtWallClock = clock.wallClock(),
            )
    }

private fun ProtectionStatus.toActivityKind(): ActivityKind? =
    when (this) {
        ProtectionStatus.DEGRADED -> ActivityKind.PROTECTION_DEGRADED
        ProtectionStatus.OFF -> ActivityKind.PROTECTION_DISABLED
        ProtectionStatus.ACTIVE -> null
    }

internal fun guardianStatusOf(
    status: ProtectionStatus,
    missing: List<SpecialPermission>,
): GuardianStatus =
    when {
        missing.isNotEmpty() -> GuardianStatus.DEGRADED
        status == ProtectionStatus.DEGRADED -> GuardianStatus.DEGRADED
        else -> GuardianStatus.OK
    }

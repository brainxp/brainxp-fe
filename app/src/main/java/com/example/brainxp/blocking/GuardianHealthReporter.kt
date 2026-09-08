package com.example.brainxp.blocking

import com.example.brainxp.core.permission.SpecialPermission
import com.example.brainxp.data.repo.ActivityLogRepository
import com.example.brainxp.data.repo.FamilyRepository
import com.example.brainxp.domain.model.ActivityEvent
import com.example.brainxp.domain.model.ActivityKind
import com.example.brainxp.domain.model.GuardianEvent
import com.example.brainxp.domain.model.GuardianStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GuardianHealthReporter
    @Inject
    constructor(
        private val activityLog: ActivityLogRepository,
        private val family: FamilyRepository,
    ) {
        private var lastMissing: Set<SpecialPermission> = emptySet()

        suspend fun report(
            status: ProtectionStatus,
            missing: List<SpecialPermission> = emptyList(),
        ) {
            status.toActivityKind()?.let { kind ->
                activityLog.record(ActivityEvent(kind = kind, timestamp = System.currentTimeMillis()))
            }
            family.reportHealth(status.toGuardianStatus(), eventsFor(missing.toSet()))
        }

        private fun eventsFor(missing: Set<SpecialPermission>): List<GuardianEvent> {
            val restored = lastMissing - missing
            lastMissing = missing
            return missing.map { permission -> event(GuardianEvent.REVOKED, permission, required = true) } +
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
            )
    }

private fun ProtectionStatus.toActivityKind(): ActivityKind? =
    when (this) {
        ProtectionStatus.DEGRADED -> ActivityKind.PROTECTION_DEGRADED
        ProtectionStatus.OFF -> ActivityKind.PROTECTION_DISABLED
        ProtectionStatus.ACTIVE -> null
    }

private fun ProtectionStatus.toGuardianStatus(): GuardianStatus =
    when (this) {
        ProtectionStatus.ACTIVE -> GuardianStatus.OK
        ProtectionStatus.DEGRADED -> GuardianStatus.DEGRADED
        ProtectionStatus.OFF -> GuardianStatus.DISABLED
    }

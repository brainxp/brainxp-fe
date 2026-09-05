package com.example.brainxp.blocking

import com.example.brainxp.data.repo.ActivityLogRepository
import com.example.brainxp.data.repo.FamilyRepository
import com.example.brainxp.domain.model.ActivityEvent
import com.example.brainxp.domain.model.ActivityKind
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
        suspend fun report(status: ProtectionStatus) {
            status.toActivityKind()?.let { kind ->
                activityLog.record(ActivityEvent(kind = kind, timestamp = System.currentTimeMillis()))
            }
            family.reportHealth(status.toGuardianStatus())
        }
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

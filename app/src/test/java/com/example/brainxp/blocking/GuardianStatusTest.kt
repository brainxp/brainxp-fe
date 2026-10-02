package com.example.brainxp.blocking

import com.example.brainxp.core.permission.SpecialPermission
import com.example.brainxp.domain.model.GuardianStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class GuardianStatusTest {
    @Test
    fun `a paired phone with nothing locked yet is healthy, not disabled`() {
        assertEquals(GuardianStatus.OK, guardianStatusOf(ProtectionStatus.OFF, emptyList()))
    }

    @Test
    fun `a revoked permission is reported even before any app is locked`() {
        assertEquals(
            GuardianStatus.DEGRADED,
            guardianStatusOf(ProtectionStatus.OFF, listOf(SpecialPermission.USAGE_ACCESS)),
        )
    }

    @Test
    fun `an enforcing phone with every permission is healthy`() {
        assertEquals(GuardianStatus.OK, guardianStatusOf(ProtectionStatus.ACTIVE, emptyList()))
    }

    @Test
    fun `an enforcing phone missing a permission is degraded`() {
        assertEquals(
            GuardianStatus.DEGRADED,
            guardianStatusOf(ProtectionStatus.DEGRADED, listOf(SpecialPermission.OVERLAY)),
        )
    }
}

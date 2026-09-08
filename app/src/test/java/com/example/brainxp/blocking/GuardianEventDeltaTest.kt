package com.example.brainxp.blocking

import com.example.brainxp.core.permission.SpecialPermission
import com.example.brainxp.domain.model.GuardianEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class EventLog {
    private var lastMissing: Set<SpecialPermission> = emptySet()

    fun report(missing: Set<SpecialPermission>): List<GuardianEvent> {
        val restored = lastMissing - missing
        lastMissing = missing
        return missing.map { permission -> event(GuardianEvent.REVOKED, permission, required = true) } +
            restored.map { permission -> event(GuardianEvent.RESTORED, permission) }
    }

    private fun event(
        type: String,
        permission: SpecialPermission,
        required: Boolean = false,
    ) = GuardianEvent(type = type, permission = permission.name.lowercase(), required = required)
}

class GuardianEventDeltaTest {
    private val log = EventLog()

    @Test
    fun `a revoked permission is reported by name so the server knows which one`() {
        val events = log.report(setOf(SpecialPermission.USAGE_ACCESS))

        assertEquals(1, events.size)
        assertEquals(GuardianEvent.REVOKED, events.first().type)
        assertEquals("usage_access", events.first().permission)
        assertTrue("a revoked guard permission is always required", events.first().required)
    }

    @Test
    fun `restoring one permission names that permission rather than reporting a bare recovery`() {
        log.report(setOf(SpecialPermission.USAGE_ACCESS, SpecialPermission.OVERLAY))

        val events = log.report(setOf(SpecialPermission.OVERLAY))
        val restored = events.filter { it.type == GuardianEvent.RESTORED }

        assertEquals(listOf("usage_access"), restored.map { it.permission })
    }

    @Test
    fun `recovering everything names every permission that came back`() {
        log.report(setOf(SpecialPermission.USAGE_ACCESS, SpecialPermission.OVERLAY))

        val restored =
            log
                .report(emptySet())
                .filter { it.type == GuardianEvent.RESTORED }
                .mapNotNull { it.permission }
                .sorted()

        assertEquals(listOf("overlay", "usage_access"), restored)
    }

    @Test
    fun `a healthy device that stays healthy says nothing`() {
        log.report(emptySet())

        assertEquals(emptyList<GuardianEvent>(), log.report(emptySet()))
    }
}

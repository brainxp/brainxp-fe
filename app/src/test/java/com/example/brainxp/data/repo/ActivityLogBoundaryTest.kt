package com.example.brainxp.data.repo

import com.example.brainxp.data.db.ActivityEventType
import com.example.brainxp.domain.model.ActivityKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val MEANINGFUL_KINDS =
    setOf(
        "MATERIAL_ADDED",
        "SESSION_COMPLETED",
        "REWARD_EARNED",
        "UNLOCK_STARTED",
        "UNLOCK_ENDED",
        "PROTECTION_DISABLED",
        "PROTECTION_DEGRADED",
        "PROTECTION_ANOMALY",
    )

private val FORBIDDEN_FRAGMENTS =
    listOf("APP_OPEN", "APP_CLOSE", "APP_LAUNCH", "APP_EXIT", "FOREGROUND", "BACKGROUND", "SCREEN_ON", "SCREEN_OFF")

class ActivityLogBoundaryTest {
    @Test
    fun `the log admits only the meaningful kinds`() {
        assertEquals(MEANINGFUL_KINDS, ActivityKind.entries.map { it.name }.toSet())
    }

    @Test
    fun `no app open or close event type can exist`() {
        val offenders =
            ActivityKind.entries.map { it.name }.filter { name ->
                FORBIDDEN_FRAGMENTS.any { name.contains(it) }
            }

        assertTrue(
            "app open/close events must never be loggable, found: $offenders",
            offenders.isEmpty(),
        )
    }

    @Test
    fun `the stored types cannot outgrow the domain kinds`() {
        assertEquals(
            ActivityKind.entries.map { it.name }.toSet(),
            ActivityEventType.entries.map { it.name }.toSet(),
        )
    }

    @Test
    fun `every domain kind maps to a stored type`() {
        ActivityKind.entries.forEach { kind ->
            assertEquals(kind.name, kind.toType().name)
        }
    }

    @Test
    fun `no storable type carries a per-app usage fragment`() {
        val offenders =
            ActivityEventType.entries.map { it.name }.filter { name ->
                FORBIDDEN_FRAGMENTS.any { name.contains(it) }
            }

        assertTrue("found storable per-app usage types: $offenders", offenders.isEmpty())
    }
}

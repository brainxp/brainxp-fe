package com.example.brainxp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GuardianAlertTest {
    @Test
    fun `every kind the server can send has a home`() {
        val wire =
            mapOf(
                "accessibility_off" to AlertKind.ACCESSIBILITY_OFF,
                "usage_access_off" to AlertKind.USAGE_ACCESS_OFF,
                "overlay_off" to AlertKind.OVERLAY_OFF,
                "protection_disabled" to AlertKind.PROTECTION_DISABLED,
                "device_silent" to AlertKind.DEVICE_SILENT,
            )

        wire.forEach { (sent, expected) -> assertEquals(sent, expected, alertKindOf(sent)) }
    }

    @Test
    fun `a kind we have never seen is kept rather than dropped`() {
        assertEquals(AlertKind.UNKNOWN, alertKindOf("something_new_from_the_server"))
    }

    @Test
    fun `the mapping covers the whole enum so a new kind cannot be forgotten`() {
        val mapped =
            listOf(
                "accessibility_off",
                "usage_access_off",
                "overlay_off",
                "protection_disabled",
                "device_silent",
            ).map(::alertKindOf)

        assertEquals(AlertKind.entries - AlertKind.UNKNOWN, mapped)
    }
}

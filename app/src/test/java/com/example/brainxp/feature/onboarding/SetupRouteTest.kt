package com.example.brainxp.feature.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class SetupRouteTest {
    @Test
    fun `a personal account that never picked a level is asked for one`() {
        assertEquals(SetupRoute.LEVEL, setupRouteOf(family = false, hasSubject = false, permissionsReady = true))
    }

    @Test
    fun `a personal account that already has a level skips the level screen`() {
        assertEquals(SetupRoute.HOME, setupRouteOf(family = false, hasSubject = true, permissionsReady = true))
    }

    @Test
    fun `a returning account that revoked a required permission goes to permissions, not the level`() {
        assertEquals(SetupRoute.PERMISSIONS, setupRouteOf(family = false, hasSubject = true, permissionsReady = false))
    }

    @Test
    fun `a parent never sees the level or permission screens`() {
        assertEquals(SetupRoute.HOME, setupRouteOf(family = true, hasSubject = false, permissionsReady = false))
    }
}

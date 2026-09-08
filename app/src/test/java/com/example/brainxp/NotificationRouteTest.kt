package com.example.brainxp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationRouteTest {
    @Test
    fun `a rejected material opens the generation screen that refused it`() {
        assertEquals(
            MainRoute.Preparing("m-1"),
            notificationRoute(rejectedMaterial = "m-1", readyMaterial = null, blockedPackage = null),
        )
    }

    @Test
    fun `a ready material still opens straight into its questions`() {
        assertEquals(
            MainRoute.Questions("m-2"),
            notificationRoute(rejectedMaterial = null, readyMaterial = "m-2", blockedPackage = null),
        )
    }

    @Test
    fun `a rejection is never mistaken for something answerable`() {
        val route = notificationRoute(rejectedMaterial = "m-3", readyMaterial = "m-3", blockedPackage = null)

        assertEquals(MainRoute.Preparing("m-3"), route)
    }

    @Test
    fun `a blocked app opens the place material is handed in`() {
        assertEquals(
            MainRoute.Capture,
            notificationRoute(rejectedMaterial = null, readyMaterial = null, blockedPackage = "com.mobile.legends"),
        )
    }

    @Test
    fun `a material takes precedence over a blocked app`() {
        assertEquals(
            MainRoute.Questions("m-4"),
            notificationRoute(rejectedMaterial = null, readyMaterial = "m-4", blockedPackage = "com.whatever"),
        )
    }

    @Test
    fun `an intent carrying nothing routes nowhere`() {
        assertNull(notificationRoute(rejectedMaterial = null, readyMaterial = null, blockedPackage = null))
    }
}

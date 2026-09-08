package com.example.brainxp.blocking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val OWN = "com.example.brainxp"
private const val LAUNCHER = "com.sec.android.app.launcher"
private const val SETTINGS = "com.android.settings"
private const val DIALER = "com.samsung.android.dialer"
private const val EMERGENCY = "com.samsung.android.emergency"
private const val GAME = "com.mobile.legends"
private const val SOCIAL = "com.instagram.android"

private val PROTECTED =
    ProtectedPackages(
        own = OWN,
        launcher = LAUNCHER,
        settings = SETTINGS,
        dialer = DIALER,
        emergency = setOf(EMERGENCY),
    )

private fun app(
    packageName: String,
    label: String = packageName,
    isGame: Boolean = false,
) = InstalledApp(packageName, label, isGame)

class SystemCriticalFilterTest {
    @Test
    fun `our own package is excluded`() {
        assertTrue(SystemCriticalFilter.isProtected(OWN, PROTECTED))
    }

    @Test
    fun `the current launcher is excluded`() {
        assertTrue(SystemCriticalFilter.isProtected(LAUNCHER, PROTECTED))
    }

    @Test
    fun `the settings package is excluded`() {
        assertTrue(SystemCriticalFilter.isProtected(SETTINGS, PROTECTED))
    }

    @Test
    fun `the default dialer is excluded`() {
        assertTrue(SystemCriticalFilter.isProtected(DIALER, PROTECTED))
    }

    @Test
    fun `an emergency role app is excluded`() {
        assertTrue(SystemCriticalFilter.isProtected(EMERGENCY, PROTECTED))
    }

    @Test
    fun `an ordinary app is not excluded`() {
        assertFalse(SystemCriticalFilter.isProtected(GAME, PROTECTED))
        assertFalse(SystemCriticalFilter.isProtected(SOCIAL, PROTECTED))
    }

    @Test
    fun `every protected category is stripped from the list at once`() {
        val apps =
            listOf(
                app(OWN),
                app(LAUNCHER),
                app(SETTINGS),
                app(DIALER),
                app(EMERGENCY),
                app(GAME, "Mobile Legends", isGame = true),
                app(SOCIAL, "Instagram"),
            )

        val selectable = SystemCriticalFilter.selectable(apps, PROTECTED).map { it.packageName }

        assertEquals(listOf(GAME, SOCIAL), selectable)
    }

    @Test
    fun `games are listed before other apps`() {
        val apps =
            listOf(
                app(SOCIAL, "Instagram"),
                app(GAME, "Mobile Legends", isGame = true),
            )

        val selectable = SystemCriticalFilter.selectable(apps, PROTECTED)

        assertEquals(GAME, selectable.first().packageName)
    }

    @Test
    fun `apps of the same kind are sorted by label case insensitively`() {
        val apps = listOf(app("c", "zebra"), app("a", "Alpha"), app("b", "beta"))

        val labels = SystemCriticalFilter.selectable(apps, PROTECTED).map { it.label }

        assertEquals(listOf("Alpha", "beta", "zebra"), labels)
    }

    @Test
    fun `a missing protected package does not exclude a blank name`() {
        val sparse = ProtectedPackages(own = OWN)

        assertFalse(SystemCriticalFilter.isProtected("", sparse))
        assertEquals(setOf(OWN), sparse.all)
    }

    @Test
    fun `an unresolved launcher does not accidentally protect everything`() {
        val sparse = ProtectedPackages(own = OWN)
        val apps = listOf(app(OWN), app(GAME), app(LAUNCHER))

        val selectable = SystemCriticalFilter.selectable(apps, sparse).map { it.packageName }

        assertEquals(listOf(GAME, LAUNCHER), selectable.sorted())
        assertFalse(selectable.contains(OWN))
    }

    @Test
    fun `multiple emergency packages are all excluded`() {
        val many = PROTECTED.copy(emergency = setOf(EMERGENCY, "com.other.emergency"))

        assertTrue(SystemCriticalFilter.isProtected("com.other.emergency", many))
        assertTrue(SystemCriticalFilter.isProtected(EMERGENCY, many))
    }
}

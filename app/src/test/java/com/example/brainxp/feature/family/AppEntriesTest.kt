package com.example.brainxp.feature.family

import com.example.brainxp.domain.model.DeviceApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun app(
    id: String,
    label: String,
    locked: Boolean = false,
) = DeviceApp(packageName = id, label = label, locked = locked)

class AppEntriesTest {
    @Test
    fun `a parent sees every app the child device reported, not only the locked ones`() {
        val entries =
            appEntriesOf(
                onDevice = listOf(app("com.game.one", "Block Blast"), app("com.chat.two", "Chat")),
                lockedPackages = emptyList(),
            )

        assertEquals(listOf("Block Blast", "Chat"), entries.map { it.label })
        assertTrue("nothing is locked yet", entries.none { it.locked })
    }

    @Test
    fun `apps carry the real label rather than the package name`() {
        val entries = appEntriesOf(listOf(app("com.game.one", "Block Blast")), emptyList())

        assertEquals("Block Blast", entries.single().label)
    }

    @Test
    fun `a package the policy locks is shown locked even if the device did not say so`() {
        val entries = appEntriesOf(listOf(app("com.game.one", "Block Blast")), listOf("com.game.one"))

        assertTrue(entries.single().locked)
    }

    @Test
    fun `a locked package the device never reported is still listed so it can be released`() {
        val entries = appEntriesOf(listOf(app("com.game.one", "Block Blast")), listOf("com.gone.away"))

        val stray = entries.first { it.packageName == "com.gone.away" }
        assertTrue("a stray lock must stay visible", stray.locked)
        assertEquals("com.gone.away", stray.label)
    }

    @Test
    fun `locked apps come first so a parent sees what is already restricted`() {
        val entries =
            appEntriesOf(
                onDevice = listOf(app("com.a", "Alpha"), app("com.z", "Zulu", locked = true)),
                lockedPackages = emptyList(),
            )

        assertEquals(listOf("Zulu", "Alpha"), entries.map { it.label })
    }

    @Test
    fun `an empty device report leaves nothing to choose from`() {
        assertEquals(emptyList<LockedAppEntry>(), appEntriesOf(emptyList(), emptyList()))
    }
}

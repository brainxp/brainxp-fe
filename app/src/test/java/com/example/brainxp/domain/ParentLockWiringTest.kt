package com.example.brainxp.domain

import com.example.brainxp.domain.model.DeviceRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val GUARDED_CALL_SITES =
    mapOf(
        GuardedAction.CHANGE_RESTRICTIONS to "com/example/brainxp/feature/apps/AppPickerViewModel.kt",
        GuardedAction.SWITCH_MODE to "com/example/brainxp/feature/settings/SettingsViewModel.kt",
    )

private fun sourceOf(relative: String): java.io.File {
    val roots = listOf("src/main/java", "app/src/main/java", "../app/src/main/java")
    return roots
        .map { java.io.File(it, relative) }
        .firstOrNull { it.exists() }
        ?: java.io.File(roots.first(), relative)
}

class ParentLockWiringTest {
    @Test
    fun `every guarded action is consulted somewhere in production code`() {
        val unwired =
            GuardedAction.entries.filter { action ->
                val path = GUARDED_CALL_SITES[action] ?: return@filter true
                val source = sourceOf(path)
                !source.exists() || !source.readText().contains("GuardedAction.${action.name}")
            }

        assertTrue(
            "these guarded actions have no production call site: $unwired",
            unwired.isEmpty(),
        )
    }

    @Test
    fun `each guarded action is named in the file that performs it`() {
        GUARDED_CALL_SITES.forEach { (action, path) ->
            val source = sourceOf(path)
            assertTrue("missing $path", source.exists())
            assertTrue(
                "${action.name} is not consulted in $path",
                source.readText().contains("GuardedAction.${action.name}"),
            )
        }
    }

    @Test
    fun `the wiring map covers the whole enum`() {
        assertEquals(GuardedAction.entries.toSet(), GUARDED_CALL_SITES.keys)
    }

    @Test
    fun `a paired child device refuses every guarded action with no way to unlock`() {
        val locked = ChildDeviceLock(role = DeviceRole.CHILD)

        GUARDED_CALL_SITES.keys.forEach { action ->
            assertFalse(
                "$action must stay refused on a paired child device",
                ParentGate.allows(locked, action),
            )
        }
    }
}

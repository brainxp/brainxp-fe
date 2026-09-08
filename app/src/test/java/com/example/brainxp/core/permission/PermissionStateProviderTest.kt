package com.example.brainxp.core.permission

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeReader(
    var granted: Set<SpecialPermission> = emptySet(),
) : PermissionReader {
    var reads = 0

    override fun isGranted(permission: SpecialPermission): Boolean {
        reads++
        return permission in granted
    }
}

private object NoIntents : PermissionIntents {
    override fun settingsIntents(permission: SpecialPermission): List<Intent> = emptyList()
}

private object StubLifecycleOwner : LifecycleOwner {
    override val lifecycle: Lifecycle get() = throw UnsupportedOperationException()
}

class PermissionStateProviderTest {
    private fun provider(reader: FakeReader) = DefaultPermissionStateProvider(reader, NoIntents)

    @Test
    fun `snapshot covers every special permission`() {
        val state = provider(FakeReader()).state.value

        assertEquals(SpecialPermission.entries.size, state.entries.size)
        assertEquals(SpecialPermission.entries, state.entries.map { it.permission })
    }

    @Test
    fun `reads status once per permission on construction`() {
        val reader = FakeReader()

        provider(reader)

        assertEquals(SpecialPermission.entries.size, reader.reads)
    }

    @Test
    fun `reports granted permissions from the reader`() {
        val reader = FakeReader(granted = setOf(SpecialPermission.OVERLAY))

        val state = provider(reader).state.value

        assertTrue(state.isGranted(SpecialPermission.OVERLAY))
        assertFalse(state.isGranted(SpecialPermission.USAGE_ACCESS))
    }

    @Test
    fun `refresh picks up a permission granted after construction`() {
        val reader = FakeReader()
        val provider = provider(reader)
        assertFalse(provider.state.value.isGranted(SpecialPermission.USAGE_ACCESS))

        reader.granted = setOf(SpecialPermission.USAGE_ACCESS)
        provider.refresh()

        assertTrue(provider.state.value.isGranted(SpecialPermission.USAGE_ACCESS))
    }

    @Test
    fun `refresh picks up a permission revoked after construction`() {
        val reader = FakeReader(granted = setOf(SpecialPermission.OVERLAY))
        val provider = provider(reader)

        reader.granted = emptySet()
        provider.refresh()

        assertFalse(provider.state.value.isGranted(SpecialPermission.OVERLAY))
    }

    @Test
    fun `resume observer refreshes the provider`() {
        val reader = FakeReader()
        val provider = provider(reader)
        reader.granted = setOf(SpecialPermission.OVERLAY)

        PermissionResumeObserver(provider).onResume(StubLifecycleOwner)

        assertTrue(provider.state.value.isGranted(SpecialPermission.OVERLAY))
    }

    @Test
    fun `protection is not ready while a required permission is missing`() {
        val state = provider(FakeReader(granted = setOf(SpecialPermission.OVERLAY))).state.value

        assertFalse(state.protectionReady)
        assertEquals(listOf(SpecialPermission.USAGE_ACCESS), state.missingRequired)
    }

    @Test
    fun `protection is ready once both required permissions are granted`() {
        val reader = FakeReader(granted = setOf(SpecialPermission.USAGE_ACCESS, SpecialPermission.OVERLAY))

        val state = provider(reader).state.value

        assertTrue(state.protectionReady)
        assertTrue(state.missingRequired.isEmpty())
    }

    @Test
    fun `optional and recommended permissions do not gate protection`() {
        val reader = FakeReader(granted = setOf(SpecialPermission.USAGE_ACCESS, SpecialPermission.OVERLAY))

        val state = provider(reader).state.value

        assertTrue(state.protectionReady)
        assertEquals(
            listOf(SpecialPermission.NOTIFICATIONS, SpecialPermission.BATTERY_EXEMPTION),
            state.missing(PermissionRequirement.RECOMMENDED),
        )
        assertEquals(listOf(SpecialPermission.ACCESSIBILITY), state.missing(PermissionRequirement.OPTIONAL))
    }

    @Test
    fun `accessibility is optional so it never blocks protection`() {
        assertEquals(PermissionRequirement.OPTIONAL, SpecialPermission.ACCESSIBILITY.requirement)
    }
}

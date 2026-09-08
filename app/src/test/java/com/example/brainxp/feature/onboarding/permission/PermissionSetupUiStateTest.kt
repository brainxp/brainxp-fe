package com.example.brainxp.feature.onboarding.permission

import com.example.brainxp.core.permission.PermissionEntry
import com.example.brainxp.core.permission.PermissionRequirement
import com.example.brainxp.core.permission.PermissionSnapshot
import com.example.brainxp.core.permission.SpecialPermission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun snapshot(granted: Set<SpecialPermission>) =
    PermissionSnapshot(SpecialPermission.entries.map { PermissionEntry(it, it in granted) })

class PermissionSetupUiStateTest {
    @Test
    fun `steps follow the order FR-ON-1 prescribes`() {
        val state = snapshot(emptySet()).toSetupState()

        assertEquals(
            listOf(
                SpecialPermission.NOTIFICATIONS,
                SpecialPermission.USAGE_ACCESS,
                SpecialPermission.OVERLAY,
                SpecialPermission.BATTERY_EXEMPTION,
                SpecialPermission.ACCESSIBILITY,
            ),
            state.steps.map { it.permission },
        )
    }

    @Test
    fun `positions are one based and contiguous`() {
        val state = snapshot(emptySet()).toSetupState()

        assertEquals(listOf(1, 2, 3, 4, 5), state.steps.map { it.position })
    }

    @Test
    fun `the first step is current on a fresh install`() {
        val state = snapshot(emptySet()).toSetupState()

        assertEquals(SpecialPermission.NOTIFICATIONS, state.steps.single { it.isCurrent }.permission)
    }

    @Test
    fun `the current step advances past granted ones`() {
        val state = snapshot(setOf(SpecialPermission.NOTIFICATIONS, SpecialPermission.USAGE_ACCESS)).toSetupState()

        assertEquals(SpecialPermission.OVERLAY, state.steps.single { it.isCurrent }.permission)
    }

    @Test
    fun `a granted step out of order does not become current`() {
        val state = snapshot(setOf(SpecialPermission.OVERLAY)).toSetupState()

        assertEquals(SpecialPermission.NOTIFICATIONS, state.steps.single { it.isCurrent }.permission)
        assertTrue(state.steps.first { it.permission == SpecialPermission.OVERLAY }.granted)
    }

    @Test
    fun `no step is current once everything is granted`() {
        val state = snapshot(SpecialPermission.entries.toSet()).toSetupState()

        assertTrue(state.steps.none { it.isCurrent })
        assertTrue(state.everythingGranted)
    }

    @Test
    fun `cannot continue while a required permission is missing`() {
        val state = snapshot(setOf(SpecialPermission.NOTIFICATIONS, SpecialPermission.USAGE_ACCESS)).toSetupState()

        assertFalse(state.canContinue)
    }

    @Test
    fun `can continue once both required permissions are granted`() {
        val state = snapshot(setOf(SpecialPermission.USAGE_ACCESS, SpecialPermission.OVERLAY)).toSetupState()

        assertTrue(state.canContinue)
    }

    @Test
    fun `skipping the optional step does not block completion`() {
        val granted = SpecialPermission.entries.toSet() - SpecialPermission.ACCESSIBILITY

        val state = snapshot(granted).toSetupState()

        assertTrue(state.canContinue)
        assertFalse(state.degraded)
        assertEquals(listOf(SpecialPermission.ACCESSIBILITY), state.skippable)
    }

    @Test
    fun `skipping a recommended step allows completion but reports degraded`() {
        val state = snapshot(setOf(SpecialPermission.USAGE_ACCESS, SpecialPermission.OVERLAY)).toSetupState()

        assertTrue(state.canContinue)
        assertTrue(state.degraded)
    }

    @Test
    fun `degraded is not reported while completion is still blocked`() {
        val state = snapshot(emptySet()).toSetupState()

        assertFalse(state.canContinue)
        assertFalse(state.degraded)
    }

    @Test
    fun `everything granted is neither blocked nor degraded`() {
        val state = snapshot(SpecialPermission.entries.toSet()).toSetupState()

        assertTrue(state.canContinue)
        assertFalse(state.degraded)
        assertTrue(state.skippable.isEmpty())
    }

    @Test
    fun `skippable excludes required permissions`() {
        val state = snapshot(emptySet()).toSetupState()

        assertEquals(
            listOf(
                SpecialPermission.NOTIFICATIONS,
                SpecialPermission.BATTERY_EXEMPTION,
                SpecialPermission.ACCESSIBILITY,
            ),
            state.skippable,
        )
        assertTrue(state.steps.filter { it.requirement == PermissionRequirement.REQUIRED }.none { it.permission in state.skippable })
    }
}

package com.example.brainxp.feature.onboarding.permission

import com.example.brainxp.core.permission.PermissionRequirement
import com.example.brainxp.core.permission.PermissionSnapshot
import com.example.brainxp.core.permission.SpecialPermission

data class PermissionStepUi(
    val permission: SpecialPermission,
    val position: Int,
    val granted: Boolean,
    val requirement: PermissionRequirement,
    val isCurrent: Boolean,
)

data class PermissionSetupUiState(
    val steps: List<PermissionStepUi> = emptyList(),
    val canContinue: Boolean = false,
    val degraded: Boolean = false,
    val everythingGranted: Boolean = false,
) {
    val skippable: List<SpecialPermission>
        get() = steps.filter { !it.granted && it.requirement != PermissionRequirement.REQUIRED }.map { it.permission }
}

/**
 * The current step is the first one still missing, walking FR-ON-1's order. Completion
 * is gated on REQUIRED only, so skipping an optional or recommended step still lets the
 * user finish; a missing recommended step reports degraded instead, per invariant 7.
 */
fun PermissionSnapshot.toSetupState(): PermissionSetupUiState {
    val currentIndex = entries.indexOfFirst { !it.granted }
    val steps =
        entries.mapIndexed { index, entry ->
            PermissionStepUi(
                permission = entry.permission,
                position = index + 1,
                granted = entry.granted,
                requirement = entry.requirement,
                isCurrent = index == currentIndex,
            )
        }
    return PermissionSetupUiState(
        steps = steps,
        canContinue = protectionReady,
        degraded = protectionReady && missing(PermissionRequirement.RECOMMENDED).isNotEmpty(),
        everythingGranted = currentIndex == -1,
    )
}

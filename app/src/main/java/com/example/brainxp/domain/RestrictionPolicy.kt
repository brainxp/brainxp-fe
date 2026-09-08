package com.example.brainxp.domain

import com.example.brainxp.domain.model.RestrictionState
import com.example.brainxp.domain.model.UnlockState

object RestrictionPolicy {
    fun isBlocked(
        packageName: String,
        state: RestrictionState,
    ): Boolean {
        if (packageName !in state.restrictedPackages) {
            return false
        }
        val unlock = state.unlock
        val covered =
            unlock is UnlockState.Active &&
                !unlock.exhausted &&
                unlock.meters(packageName)
        return !covered
    }
}

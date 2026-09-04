package com.example.brainxp.domain.model

sealed interface UnlockState {
    data object Locked : UnlockState

    data class Active(
        val unlockId: String,
        val endAtElapsed: Long,
        val endAtWallClock: Long,
        val allowedPackages: Set<String>,
    ) : UnlockState

    data object Expired : UnlockState
}

data class RestrictionState(
    val restrictedPackages: Set<String> = emptySet(),
    val unlock: UnlockState = UnlockState.Locked,
)

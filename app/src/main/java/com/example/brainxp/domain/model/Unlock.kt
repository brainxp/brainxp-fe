package com.example.brainxp.domain.model

sealed interface UnlockState {
    data object Locked : UnlockState

    data class Active(
        val unlockId: String,
        val budgetMillis: Long,
        val consumedByPackage: Map<String, Long> = emptyMap(),
        val allowedPackages: Set<String> = emptySet(),
    ) : UnlockState {
        val consumedMillis: Long get() = consumedByPackage.values.sum()

        val remainingMillis: Long get() = (budgetMillis - consumedMillis).coerceAtLeast(0)

        val exhausted: Boolean get() = remainingMillis == 0L

        fun meters(packageName: String): Boolean = packageName in allowedPackages

        fun advanced(
            packageName: String,
            millis: Long,
        ): Active {
            val capped = millis.coerceIn(0, remainingMillis)
            if (capped == 0L) {
                return this
            }
            val previous = consumedByPackage[packageName] ?: 0L
            return copy(consumedByPackage = consumedByPackage + (packageName to previous + capped))
        }
    }

    data object Expired : UnlockState
}

data class RestrictionState(
    val restrictedPackages: Set<String> = emptySet(),
    val unlock: UnlockState = UnlockState.Locked,
)

package com.example.brainxp.domain

import com.example.brainxp.domain.model.UnlockState

enum class PlaytimeCue {
    OFFER,
    WRAP_UP,
    LAST_CALL,
}

private const val OFFER_THRESHOLD_SECONDS = 1_800
private const val WRAP_UP_THRESHOLD_SECONDS = 300
private const val WRAP_UP_MINIMUM_BUDGET_SECONDS = 600
private const val MILLIS_PER_SECOND = 1_000L

fun thresholdOf(
    cue: PlaytimeCue,
    lastCallSeconds: Int,
): Int =
    when (cue) {
        PlaytimeCue.OFFER -> OFFER_THRESHOLD_SECONDS
        PlaytimeCue.WRAP_UP -> WRAP_UP_THRESHOLD_SECONDS
        PlaytimeCue.LAST_CALL -> lastCallSeconds
    }

private fun armed(
    cue: PlaytimeCue,
    budgetSeconds: Int,
): Boolean =
    when (cue) {
        PlaytimeCue.OFFER -> budgetSeconds > OFFER_THRESHOLD_SECONDS
        PlaytimeCue.WRAP_UP -> budgetSeconds >= WRAP_UP_MINIMUM_BUDGET_SECONDS
        PlaytimeCue.LAST_CALL -> true
    }

fun cueCrossed(
    budgetSeconds: Int,
    before: Int,
    after: Int,
    lastCallSeconds: Int,
): PlaytimeCue? {
    if (after <= 0) {
        return null
    }
    return PlaytimeCue.entries
        .filter { armed(it, budgetSeconds) }
        .filter { before > thresholdOf(it, lastCallSeconds) && after <= thresholdOf(it, lastCallSeconds) }
        .minByOrNull { thresholdOf(it, lastCallSeconds) }
}

class PlaytimeCueTracker {
    private var unlockId: String? = null
    private var previousRemaining: Int? = null
    private var fired: Set<PlaytimeCue> = emptySet()

    fun reset() {
        unlockId = null
        previousRemaining = null
        fired = emptySet()
    }

    fun cueFor(
        unlock: UnlockState.Active,
        lastCallSeconds: Int,
    ): PlaytimeCue? {
        if (unlockId != unlock.unlockId) {
            reset()
            unlockId = unlock.unlockId
        }
        val remaining = (unlock.remainingMillis / MILLIS_PER_SECOND).toInt()
        val before = previousRemaining
        previousRemaining = remaining
        val cue =
            before
                ?.let {
                    cueCrossed(
                        budgetSeconds = (unlock.budgetMillis / MILLIS_PER_SECOND).toInt(),
                        before = it,
                        after = remaining,
                        lastCallSeconds = lastCallSeconds,
                    )
                }?.takeUnless { it in fired }
        if (cue != null) {
            fired = fired + cue
        }
        return cue
    }
}

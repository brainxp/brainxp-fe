package com.example.brainxp.data.repo

import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.core.time.AppClock
import com.example.brainxp.data.prefs.CachedBalance
import com.example.brainxp.data.prefs.RewardCache
import com.example.brainxp.domain.model.Standing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

enum class BalanceSource {
    NONE,
    SERVER,
    CACHE,
}

data class ReconciledBalance(
    val balanceSeconds: Int = 0,
    val source: BalanceSource = BalanceSource.NONE,
    val standing: Standing? = null,
    val error: ApiError? = null,
) {
    val stale: Boolean get() = source == BalanceSource.CACHE

    val spendable: Boolean get() = source != BalanceSource.NONE && balanceSeconds > 0
}

@Singleton
class RewardReconciler
    @Inject
    constructor(
        private val rewards: RewardRepository,
        private val cache: RewardCache,
        private val clock: AppClock,
    ) {
        private val mutableState = MutableStateFlow(ReconciledBalance())

        val state: StateFlow<ReconciledBalance> = mutableState.asStateFlow()

        suspend fun reconcile(): ReconciledBalance {
            val reconciled =
                when (val result = rewards.standing()) {
                    is AppResult.Success -> fromServer(result.value)
                    is AppResult.Failure -> fromCache(result.error)
                }
            mutableState.value = reconciled
            return reconciled
        }

        private suspend fun fromServer(standing: Standing): ReconciledBalance {
            cache.write(CachedBalance(standing.balanceSeconds, clock.wallClock()))
            return ReconciledBalance(
                balanceSeconds = standing.balanceSeconds,
                source = BalanceSource.SERVER,
                standing = standing,
            )
        }

        private suspend fun fromCache(error: ApiError): ReconciledBalance {
            val cached = cache.cached.first()
            return ReconciledBalance(
                balanceSeconds = cached?.balanceSeconds ?: 0,
                source = if (cached == null) BalanceSource.NONE else BalanceSource.CACHE,
                error = error,
            )
        }
    }

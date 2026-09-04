package com.example.brainxp.data.repo

import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.core.time.AppClock
import com.example.brainxp.data.prefs.CachedBalance
import com.example.brainxp.data.prefs.RewardCache
import com.example.brainxp.domain.model.ConsumptionEntry
import com.example.brainxp.domain.model.Standing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.util.UUID
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

interface ConsumptionReporter {
    suspend fun report(secondsByPackage: Map<String, Int>): AppResult<ReconciledBalance>
}

@Singleton
class RewardReconciler
    @Inject
    constructor(
        private val rewards: RewardRepository,
        private val cache: RewardCache,
        private val clock: AppClock,
    ) : ConsumptionReporter {
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

        override suspend fun report(secondsByPackage: Map<String, Int>): AppResult<ReconciledBalance> {
            val entries =
                secondsByPackage.filterValues { it > 0 }.map { (packageName, seconds) ->
                    ConsumptionEntry(
                        clientEventId = UUID.randomUUID().toString(),
                        appLabel = packageName,
                        seconds = seconds,
                        occurredAtWallClock = clock.wallClock(),
                    )
                }
            if (entries.isEmpty()) {
                return AppResult.Success(mutableState.value)
            }
            return when (val result = rewards.reportConsumption(entries)) {
                is AppResult.Success -> {
                    val reconciled = fromServer(result.value)
                    mutableState.value = reconciled
                    AppResult.Success(reconciled)
                }

                is AppResult.Failure -> {
                    mutableState.value = deductLocally(entries.sumOf { it.seconds }, result.error)
                    AppResult.Failure(result.error)
                }
            }
        }

        private suspend fun deductLocally(
            seconds: Int,
            error: ApiError,
        ): ReconciledBalance {
            val remaining = (mutableState.value.balanceSeconds - seconds).coerceAtLeast(0)
            cache.write(CachedBalance(remaining, clock.wallClock()))
            return mutableState.value.copy(
                balanceSeconds = remaining,
                source = BalanceSource.CACHE,
                error = error,
            )
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

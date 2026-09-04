package com.example.brainxp.data.repo.fake

import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class FakeBackend
    @Inject
    constructor() {
        @Volatile
        var latencyMillis: LongRange = DEFAULT_LATENCY

        private val queuedFailures = ConcurrentHashMap<String, ArrayDeque<ApiError>>()
        private val callCounts = ConcurrentHashMap<String, Int>()

        fun failNext(
            operation: String,
            error: ApiError,
        ) {
            queuedFailures.getOrPut(operation) { ArrayDeque() }.addLast(error)
        }

        fun callsTo(operation: String): Int = callCounts[operation] ?: 0

        fun reset() {
            queuedFailures.clear()
            callCounts.clear()
            latencyMillis = DEFAULT_LATENCY
        }

        suspend fun <T> respond(
            operation: String,
            value: () -> T,
        ): AppResult<T> {
            callCounts.merge(operation, 1, Int::plus)
            delay(nextLatency())

            val failure = queuedFailures[operation]?.removeFirstOrNull()
            return if (failure != null) AppResult.Failure(failure) else AppResult.Success(value())
        }

        private fun nextLatency(): Long =
            when {
                latencyMillis.isEmpty() -> 0L
                latencyMillis.first == latencyMillis.last -> latencyMillis.first
                else -> Random.nextLong(latencyMillis.first, latencyMillis.last)
            }

        companion object {
            val DEFAULT_LATENCY = 180L..520L
            val INSTANT = 0L..0L

            const val MATERIAL_UPLOAD = "material.upload"
            const val MATERIAL_PAGE = "material.page"
            const val MATERIAL_DETAIL = "material.detail"
            const val SESSION_REQUEST = "session.requestGeneration"
            const val SESSION_POLL = "session.pollGeneration"
            const val SESSION_GET = "session.get"
            const val SESSION_ANSWER = "session.submitAnswer"
            const val SESSION_COMPLETE = "session.complete"
            const val REWARD_STANDING = "reward.standing"
            const val LEDGER_SYNC = "reward.reportConsumption"
            const val LEDGER_HISTORY = "reward.history"
            const val LEDGER_ADJUST = "reward.adjust"
            const val RESTRICTION_SET = "restriction.set"
            const val RESTRICTION_REPLACE = "restriction.replace"
            const val ACTIVITY_RECORD = "activity.record"
            const val ACTIVITY_FLUSH = "activity.flush"
            const val FAMILY_CHILDREN = "family.children"
            const val FAMILY_PAIR = "family.pair"
            const val FAMILY_CONFIG = "family.updateConfig"
        }
    }

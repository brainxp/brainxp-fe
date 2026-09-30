package com.example.brainxp.data.repo

import com.example.brainxp.core.result.AppResult
import com.example.brainxp.core.time.AppClock
import com.example.brainxp.data.db.PendingOperationDao
import com.example.brainxp.data.db.PendingOperationEntity
import com.example.brainxp.data.db.PendingOperationType
import com.example.brainxp.domain.model.ConsumptionEntry
import com.example.brainxp.domain.model.GuardianEvent
import com.example.brainxp.domain.model.GuardianStatus
import com.example.brainxp.domain.model.Standing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.math.min

@Serializable
private data class QueuedConsumption(
    val clientEventId: String,
    val appLabel: String,
    val seconds: Int,
    val occurredAtWallClock: Long? = null,
)

@Serializable
private data class QueuedConsumptionBatch(
    val entries: List<QueuedConsumption>,
)

@Serializable
private data class QueuedGuardianEvent(
    val type: String,
    val permission: String? = null,
    val required: Boolean = false,
    val occurredAtWallClock: Long? = null,
)

@Serializable
private data class QueuedHealthReport(
    val status: String,
    val events: List<QueuedGuardianEvent>,
)

@Singleton
class PendingSyncQueue
    @Inject
    constructor(
        private val rewards: Provider<RewardRepository>,
        private val family: Provider<FamilyRepository>,
        private val pending: PendingOperationDao,
        private val clock: AppClock,
    ) {
        suspend fun sendConsumption(entries: List<ConsumptionEntry>): AppResult<Standing> {
            val result = rewards.get().reportConsumption(entries)
            if (result is AppResult.Failure) {
                enqueue(PendingOperationType.SYNC_LEDGER, QueuedConsumptionBatch(entries.map(ConsumptionEntry::queued)))
            }
            return result
        }

        suspend fun sendHealth(
            status: GuardianStatus,
            events: List<GuardianEvent>,
        ): AppResult<Unit> {
            val result = family.get().reportHealth(status, events)
            if (result is AppResult.Failure && result.error.retryable) {
                enqueue(
                    PendingOperationType.SEND_EVENTS,
                    QueuedHealthReport(status.name, events.map(GuardianEvent::queued)),
                )
            }
            return result
        }

        suspend fun flush(): Int = flushConsumption() + flushHealth()

        suspend fun queuedCount(): Int =
            pending.findReadyForRetryByType(PendingOperationType.SYNC_LEDGER, Long.MAX_VALUE).size +
                pending.findReadyForRetryByType(PendingOperationType.SEND_EVENTS, Long.MAX_VALUE).size

        suspend fun hasPendingConsumption(): Boolean =
            pending.findReadyForRetryByType(PendingOperationType.SYNC_LEDGER, Long.MAX_VALUE).isNotEmpty()

        private suspend fun flushConsumption(): Int =
            flush(PendingOperationType.SYNC_LEDGER, retainFailures = true) { payload ->
                val batch = json.decodeFromString<QueuedConsumptionBatch>(payload)
                rewards.get().reportConsumption(batch.entries.map(QueuedConsumption::domain)).mapToUnit()
            }

        private suspend fun flushHealth(): Int =
            flush(PendingOperationType.SEND_EVENTS) { payload ->
                val report = json.decodeFromString<QueuedHealthReport>(payload)
                val status = GuardianStatus.valueOf(report.status)
                family.get().reportHealth(status, report.events.map(QueuedGuardianEvent::domain))
            }

        private suspend fun flush(
            type: PendingOperationType,
            retainFailures: Boolean = false,
            send: suspend (String) -> AppResult<Unit>,
        ): Int {
            val now = clock.wallClock()
            val ready = pending.findReadyForRetryByType(type, now)
            var sent = 0
            ready.forEach { row ->
                val result = runCatching { send(row.payload) }.getOrNull()
                when (result) {
                    is AppResult.Success -> {
                        pending.deleteById(row.id)
                        sent++
                    }

                    is AppResult.Failure -> {
                        if (result.error.retryable || retainFailures) {
                            pending.recordAttempt(row.id, now + retryDelay(row.attempts))
                        } else {
                            pending.deleteById(row.id)
                        }
                    }

                    null -> {
                        pending.deleteById(row.id)
                    }
                }
            }
            return sent
        }

        private suspend inline fun <reified T> enqueue(
            type: PendingOperationType,
            payload: T,
        ) {
            val now = clock.wallClock()
            pending.insert(
                PendingOperationEntity(
                    type = type,
                    payload = json.encodeToString(payload),
                    nextAttemptAt = now,
                    createdAt = now,
                ),
            )
        }

        private fun retryDelay(attempts: Int): Long =
            (RETRY_BASE_MILLIS * (1L shl min(attempts, MAX_BACKOFF_SHIFT))).coerceAtMost(RETRY_MAX_MILLIS)

        private companion object {
            val json = Json { ignoreUnknownKeys = true }
            const val RETRY_BASE_MILLIS = 15_000L
            const val RETRY_MAX_MILLIS = 6 * 60 * 60 * 1_000L
            const val MAX_BACKOFF_SHIFT = 10
        }
    }

private fun ConsumptionEntry.queued(): QueuedConsumption =
    QueuedConsumption(
        clientEventId = clientEventId,
        appLabel = appLabel,
        seconds = seconds,
        occurredAtWallClock = occurredAtWallClock,
    )

private fun QueuedConsumption.domain(): ConsumptionEntry =
    ConsumptionEntry(
        clientEventId = clientEventId,
        appLabel = appLabel,
        seconds = seconds,
        occurredAtWallClock = occurredAtWallClock,
    )

private fun GuardianEvent.queued(): QueuedGuardianEvent =
    QueuedGuardianEvent(
        type = type,
        permission = permission,
        required = required,
        occurredAtWallClock = occurredAtWallClock,
    )

private fun QueuedGuardianEvent.domain(): GuardianEvent =
    GuardianEvent(
        type = type,
        permission = permission,
        required = required,
        occurredAtWallClock = occurredAtWallClock,
    )

private fun AppResult<Standing>.mapToUnit(): AppResult<Unit> =
    when (this) {
        is AppResult.Success -> AppResult.Success(Unit)
        is AppResult.Failure -> AppResult.Failure(error)
    }

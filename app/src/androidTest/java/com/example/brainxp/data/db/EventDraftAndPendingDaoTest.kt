package com.example.brainxp.data.db

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventDraftAndPendingDaoTest : DbTest() {
    private suspend fun event(
        type: ActivityEventType,
        timestamp: Long,
        syncState: SyncState = SyncState.PENDING,
    ): Long =
        db.activityEventDao().insert(
            ActivityEventEntity(type = type, timestamp = timestamp, syncState = syncState),
        )

    private suspend fun operation(
        type: PendingOperationType,
        nextAttemptAt: Long,
        attempts: Int = 0,
    ): Long =
        db.pendingOperationDao().insert(
            PendingOperationEntity(
                type = type,
                payload = """{"k":"v"}""",
                attempts = attempts,
                nextAttemptAt = nextAttemptAt,
                createdAt = 0,
            ),
        )

    @Test
    fun onlyOperationsWhoseBackoffElapsedAreReadyForRetry() =
        runTest {
            val dao = db.pendingOperationDao()
            operation(PendingOperationType.SUBMIT_ANSWER, nextAttemptAt = 100)
            operation(PendingOperationType.UPLOAD_MATERIAL, nextAttemptAt = 500)
            operation(PendingOperationType.SEND_EVENTS, nextAttemptAt = 900)

            val ready = dao.findReadyForRetry(now = 500).map { it.type }

            assertEquals(
                listOf(PendingOperationType.SUBMIT_ANSWER, PendingOperationType.UPLOAD_MATERIAL),
                ready,
            )
        }

    @Test
    fun readyOperationsAreOrderedByDueTimeAndCapped() =
        runTest {
            val dao = db.pendingOperationDao()
            operation(PendingOperationType.SEND_EVENTS, nextAttemptAt = 300)
            operation(PendingOperationType.SUBMIT_ANSWER, nextAttemptAt = 100)
            operation(PendingOperationType.UPLOAD_MATERIAL, nextAttemptAt = 200)

            val ready = dao.findReadyForRetry(now = 1_000, limit = 2).map { it.nextAttemptAt }

            assertEquals(listOf(100L, 200L), ready)
        }

    @Test
    fun retryQueueCanBeFilteredByOperationType() =
        runTest {
            val dao = db.pendingOperationDao()
            operation(PendingOperationType.SUBMIT_ANSWER, nextAttemptAt = 100)
            operation(PendingOperationType.UPLOAD_MATERIAL, nextAttemptAt = 100)

            val answers = dao.findReadyForRetryByType(PendingOperationType.SUBMIT_ANSWER, now = 200)

            assertEquals(1, answers.size)
            assertEquals(PendingOperationType.SUBMIT_ANSWER, answers.single().type)
        }

    @Test
    fun recordingAnAttemptIncrementsAndPushesTheNextTryOut() =
        runTest {
            val dao = db.pendingOperationDao()
            val id = operation(PendingOperationType.SUBMIT_ANSWER, nextAttemptAt = 100)

            dao.recordAttempt(id, nextAttemptAt = 5_000)

            assertTrue(dao.findReadyForRetry(now = 1_000).isEmpty())
            val later = dao.findReadyForRetry(now = 6_000).single()
            assertEquals(1, later.attempts)
        }

    @Test
    fun exhaustedOperationsArePrunedAndOthersKept() =
        runTest {
            val dao = db.pendingOperationDao()
            operation(PendingOperationType.SUBMIT_ANSWER, nextAttemptAt = 100, attempts = 5)
            operation(PendingOperationType.UPLOAD_MATERIAL, nextAttemptAt = 100, attempts = 1)

            dao.deleteExhausted(maxAttempts = 5)

            assertEquals(1, dao.count())
            assertEquals(PendingOperationType.UPLOAD_MATERIAL, dao.findReadyForRetry(200).single().type)
        }

    @Test
    fun completedOperationIsRemovedFromTheQueue() =
        runTest {
            val dao = db.pendingOperationDao()
            val id = operation(PendingOperationType.SEND_EVENTS, nextAttemptAt = 100)

            dao.deleteById(id)

            assertEquals(0, dao.count())
        }
}

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
    fun activityLogIsNewestFirstAndLimited() =
        runTest {
            event(ActivityEventType.MATERIAL_ADDED, 100)
            event(ActivityEventType.REWARD_EARNED, 300)
            event(ActivityEventType.UNLOCK_STARTED, 200)

            val recent =
                db
                    .activityEventDao()
                    .observeRecent(2)
                    .first()
                    .map { it.timestamp }

            assertEquals(listOf(300L, 200L), recent)
        }

    @Test
    fun pendingEventsAreBatchedOldestFirst() =
        runTest {
            event(ActivityEventType.UNLOCK_STARTED, 300)
            event(ActivityEventType.UNLOCK_ENDED, 100)
            event(ActivityEventType.REWARD_EARNED, 200, syncState = SyncState.SYNCED)

            val batch = db.activityEventDao().findBySyncState(SyncState.PENDING, limit = 10)

            assertEquals(listOf(100L, 300L), batch.map { it.timestamp })
        }

    @Test
    fun markingABatchSyncedEmptiesTheQueue() =
        runTest {
            val first = event(ActivityEventType.UNLOCK_STARTED, 100)
            val second = event(ActivityEventType.UNLOCK_ENDED, 200)

            db.activityEventDao().updateSyncState(listOf(first, second), SyncState.SYNCED)

            assertTrue(db.activityEventDao().findBySyncState(SyncState.PENDING, 10).isEmpty())
        }

    @Test
    fun protectionEventsCanBeQueriedOnTheirOwn() =
        runTest {
            event(ActivityEventType.MATERIAL_ADDED, 100)
            event(ActivityEventType.PROTECTION_ANOMALY, 200)
            event(ActivityEventType.PROTECTION_ANOMALY, 300)

            val anomalies = db.activityEventDao().findByType(ActivityEventType.PROTECTION_ANOMALY)

            assertEquals(listOf(300L, 200L), anomalies.map { it.timestamp })
        }

    @Test
    fun prunedSyncedEventsLeavePendingOnesAlone() =
        runTest {
            event(ActivityEventType.UNLOCK_STARTED, 100, syncState = SyncState.SYNCED)
            event(ActivityEventType.UNLOCK_ENDED, 150, syncState = SyncState.PENDING)
            event(ActivityEventType.REWARD_EARNED, 900, syncState = SyncState.SYNCED)

            db.activityEventDao().deleteSyncedOlderThan(500)

            val remaining =
                db
                    .activityEventDao()
                    .observeRecent(10)
                    .first()
                    .map { it.timestamp }
            assertEquals(listOf(900L, 150L), remaining)
        }

    @Test
    fun draftPagesComeBackInPageOrder() =
        runTest {
            val dao = db.ocrDraftDao()
            dao.upsert(OcrDraftEntity("d1", pageIndex = 2, text = "third", updatedAt = 1))
            dao.upsert(OcrDraftEntity("d1", pageIndex = 0, text = "first", updatedAt = 1))
            dao.upsert(OcrDraftEntity("d1", pageIndex = 1, text = "second", updatedAt = 1))

            assertEquals(listOf("first", "second", "third"), dao.findForDraft("d1").map { it.text })
        }

    @Test
    fun autosaveOverwritesTheSamePageInsteadOfAppending() =
        runTest {
            val dao = db.ocrDraftDao()
            dao.upsert(OcrDraftEntity("d1", pageIndex = 0, text = "typo", updatedAt = 1))
            dao.upsert(OcrDraftEntity("d1", pageIndex = 0, text = "corrected", updatedAt = 2))

            val pages = dao.findForDraft("d1")
            assertEquals(1, pages.size)
            assertEquals("corrected", pages.single().text)
        }

    @Test
    fun draftsAreIsolatedFromEachOther() =
        runTest {
            val dao = db.ocrDraftDao()
            dao.upsert(OcrDraftEntity("d1", pageIndex = 0, text = "a", updatedAt = 1))
            dao.upsert(OcrDraftEntity("d2", pageIndex = 0, text = "b", updatedAt = 1))

            dao.deleteDraft("d1")

            assertTrue(dao.findForDraft("d1").isEmpty())
            assertEquals(listOf("d2"), dao.findDraftIds())
        }

    @Test
    fun draftCharCountSumsAllPagesForTheLengthWarning() =
        runTest {
            val dao = db.ocrDraftDao()
            dao.upsert(OcrDraftEntity("d1", pageIndex = 0, text = "12345", updatedAt = 1))
            dao.upsert(OcrDraftEntity("d1", pageIndex = 1, text = "678", updatedAt = 1))

            assertEquals(8, dao.charCountForDraft("d1"))
        }

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

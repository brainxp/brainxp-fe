package com.example.brainxp.data.repo

import com.example.brainxp.core.time.AppClock
import com.example.brainxp.data.db.NotificationDao
import com.example.brainxp.data.db.NotificationEntity
import com.example.brainxp.domain.model.NotificationKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeNotificationDao : NotificationDao {
    private val rows = MutableStateFlow<List<NotificationEntity>>(emptyList())

    override suspend fun upsert(notification: NotificationEntity) {
        rows.value = rows.value.filterNot { it.id == notification.id } + notification
    }

    override fun observeRecent(limit: Int): Flow<List<NotificationEntity>> =
        rows.map { all -> all.sortedByDescending { it.createdAt }.take(limit) }

    override fun observeUnreadCount(): Flow<Int> = rows.map { all -> all.count { it.readAt == null } }

    override suspend fun findFor(
        materialId: String,
        kind: String,
    ): NotificationEntity? = rows.value.firstOrNull { it.materialId == materialId && it.kind == kind }

    override suspend fun markRead(
        id: String,
        at: Long,
    ) {
        rows.value =
            rows.value.map { row ->
                if (row.id == id && row.readAt == null) row.copy(readAt = at) else row
            }
    }

    override suspend fun clearAll() {
        rows.value = emptyList()
    }
}

private class FixedClock(
    var now: Long,
) : AppClock {
    override fun elapsedRealtime(): Long = now

    override fun wallClock(): Long = now
}

class NotificationRepositoryTest {
    private val clock = FixedClock(1_000L)
    private val dao = FakeNotificationDao()
    private val repository = NotificationRepository(dao, clock)

    @Test
    fun `a recorded notification starts out unread`() =
        runTest {
            repository.record(NotificationKind.QUESTIONS_READY, "m1", "Hukum Newton", 10)

            val stored = repository.observe().first().single()

            assertTrue(stored.unread)
            assertEquals(1, repository.unreadCount().first())
        }

    @Test
    fun `reading the list leaves everything unread`() =
        runTest {
            repository.record(NotificationKind.QUESTIONS_READY, "m1", "Hukum Newton", 10)
            repository.record(NotificationKind.QUESTIONS_READY, "m2", "Gerak lurus", 8)

            repository.observe().first()

            assertEquals(2, repository.unreadCount().first())
        }

    @Test
    fun `opening one notification marks only that one read`() =
        runTest {
            val first = repository.record(NotificationKind.QUESTIONS_READY, "m1", "Hukum Newton", 10)
            repository.record(NotificationKind.QUESTIONS_READY, "m2", "Gerak lurus", 8)
            assertNotNull(first)

            clock.now = 2_000L
            repository.markRead(requireNotNull(first).id)

            val stored = repository.observe().first().associateBy { it.materialId }
            assertFalse(requireNotNull(stored["m1"]).unread)
            assertTrue(requireNotNull(stored["m2"]).unread)
            assertEquals(1, repository.unreadCount().first())
        }

    @Test
    fun `a retried background poll cannot announce the same material twice`() =
        runTest {
            val first = repository.record(NotificationKind.QUESTIONS_READY, "m1", "Hukum Newton", 10)
            val again = repository.record(NotificationKind.QUESTIONS_READY, "m1", "Hukum Newton", 10)

            assertNotNull(first)
            assertNull(again)
            assertEquals(1, repository.observe().first().size)
        }

    @Test
    fun `re-opening an already read notification keeps the first opened time`() =
        runTest {
            val record = requireNotNull(repository.record(NotificationKind.QUESTIONS_READY, "m1", "Judul", 4))

            clock.now = 2_000L
            repository.markRead(record.id)
            clock.now = 3_000L
            repository.markRead(record.id)

            assertEquals(
                2_000L,
                repository
                    .observe()
                    .first()
                    .single()
                    .readAt,
            )
        }

    @Test
    fun `a rejected material is a separate notification from a ready one`() =
        runTest {
            repository.record(NotificationKind.QUESTIONS_READY, "m1", "Judul", 4)
            val rejected = repository.record(NotificationKind.MATERIAL_REJECTED, "m1", "Judul", 0)

            assertNotNull(rejected)
            assertEquals(2, repository.observe().first().size)
        }
}

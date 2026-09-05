package com.example.brainxp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialDao {
    @Upsert
    suspend fun upsert(materials: List<MaterialEntity>)

    @Query("SELECT * FROM materials ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM materials WHERE id = :id")
    suspend fun findById(id: String): MaterialEntity?

    @Query("SELECT * FROM materials WHERE status = :status ORDER BY createdAt DESC")
    suspend fun findByStatus(status: String): List<MaterialEntity>

    @Query("DELETE FROM materials WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM materials WHERE id NOT IN (:keep)")
    suspend fun keepOnly(keep: List<String>)

    @Query("SELECT COUNT(*) FROM materials")
    suspend fun count(): Int
}

@Dao
interface QuestionSessionDao {
    @Upsert
    suspend fun upsert(session: QuestionSessionEntity)

    @Query(
        """
        SELECT * FROM question_sessions
        WHERE materialId = :materialId
        ORDER BY createdAt DESC
        """,
    )
    fun observeForMaterial(materialId: String): Flow<List<QuestionSessionEntity>>

    @Query(
        """
        SELECT * FROM question_sessions
        WHERE materialId = :materialId
        ORDER BY createdAt DESC
        """,
    )
    suspend fun findForMaterial(materialId: String): List<QuestionSessionEntity>

    @Query("SELECT * FROM question_sessions WHERE id = :id")
    suspend fun findById(id: String): QuestionSessionEntity?

    @Query("SELECT * FROM question_sessions ORDER BY createdAt DESC LIMIT :limit")
    suspend fun findRecent(limit: Int): List<QuestionSessionEntity>

    @Query("SELECT * FROM question_sessions WHERE status = :status ORDER BY createdAt ASC")
    suspend fun findByStatus(status: String): List<QuestionSessionEntity>

    @Query(
        """
        UPDATE question_sessions
        SET status = :status, score = :score, rewardSeconds = :rewardSeconds
        WHERE id = :id
        """,
    )
    suspend fun recordResult(
        id: String,
        status: String,
        score: Double?,
        rewardSeconds: Int?,
    )
}

@Dao
interface QuestionDao {
    @Upsert
    suspend fun upsert(questions: List<QuestionEntity>)

    @Query("SELECT * FROM questions WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    suspend fun findForSession(sessionId: String): List<QuestionEntity>

    @Query("SELECT COUNT(*) FROM questions WHERE sessionId = :sessionId")
    suspend fun countForSession(sessionId: String): Int

    @Query(
        """
        SELECT q.* FROM questions q
        LEFT JOIN answers a ON a.questionId = q.id
        WHERE q.sessionId = :sessionId AND a.questionId IS NULL
        ORDER BY q.orderIndex ASC
        LIMIT 1
        """,
    )
    suspend fun findNextUnanswered(sessionId: String): QuestionEntity?

    @Query("SELECT DISTINCT type FROM questions WHERE sessionId = :sessionId")
    suspend fun findTypesForSession(sessionId: String): List<String>
}

@Dao
interface AnswerDao {
    @Upsert
    suspend fun upsert(answer: AnswerEntity)

    @Query("SELECT * FROM answers WHERE sessionId = :sessionId ORDER BY answeredAt ASC")
    suspend fun findForSession(sessionId: String): List<AnswerEntity>

    @Query("SELECT * FROM answers WHERE questionId = :questionId")
    suspend fun findForQuestion(questionId: String): AnswerEntity?

    @Query("SELECT * FROM answers WHERE syncState = :state ORDER BY answeredAt ASC")
    suspend fun findBySyncState(state: SyncState): List<AnswerEntity>

    @Query("SELECT COUNT(*) FROM answers WHERE sessionId = :sessionId AND correct = 1")
    suspend fun countCorrectForSession(sessionId: String): Int

    @Query("UPDATE answers SET syncState = :state WHERE questionId = :questionId")
    suspend fun updateSyncState(
        questionId: String,
        state: SyncState,
    )
}

@Dao
interface UnlockSessionDao {
    @Upsert
    suspend fun upsert(session: UnlockSessionEntity)

    @Query("SELECT * FROM unlock_sessions WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun findActive(): UnlockSessionEntity?

    @Query("SELECT * FROM unlock_sessions WHERE status = 'ACTIVE' LIMIT 1")
    fun observeActive(): Flow<UnlockSessionEntity?>

    @Query("UPDATE unlock_sessions SET status = :status WHERE id = :id")
    suspend fun updateStatus(
        id: String,
        status: UnlockStatus,
    )

    @Query("UPDATE unlock_sessions SET status = :status WHERE status = 'ACTIVE'")
    suspend fun closeAllActive(status: UnlockStatus)

    @Query("SELECT COUNT(*) FROM unlock_sessions WHERE status = 'ACTIVE'")
    suspend fun countActive(): Int
}

@Dao
interface RestrictedAppDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoring(apps: List<RestrictedAppEntity>)

    @Upsert
    suspend fun upsert(app: RestrictedAppEntity)

    @Query("SELECT * FROM restricted_apps ORDER BY packageName ASC")
    fun observeAll(): Flow<List<RestrictedAppEntity>>

    @Query("SELECT packageName FROM restricted_apps WHERE enabled = 1 ORDER BY packageName ASC")
    suspend fun findEnabledPackages(): List<String>

    @Query("SELECT packageName FROM restricted_apps WHERE enabled = 1")
    fun observeEnabledPackages(): Flow<List<String>>

    @Query("UPDATE restricted_apps SET enabled = :enabled WHERE packageName = :packageName")
    suspend fun setEnabled(
        packageName: String,
        enabled: Boolean,
    )

    @Query("DELETE FROM restricted_apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)
}

@Dao
interface ActivityEventDao {
    @Insert
    suspend fun insert(event: ActivityEventEntity): Long

    @Query("SELECT * FROM activity_events ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ActivityEventEntity>>

    @Query("SELECT * FROM activity_events WHERE syncState = :state ORDER BY timestamp ASC LIMIT :limit")
    suspend fun findBySyncState(
        state: SyncState,
        limit: Int,
    ): List<ActivityEventEntity>

    @Query("SELECT * FROM activity_events WHERE type = :type ORDER BY timestamp DESC")
    suspend fun findByType(type: ActivityEventType): List<ActivityEventEntity>

    @Query("UPDATE activity_events SET syncState = :state WHERE id IN (:ids)")
    suspend fun updateSyncState(
        ids: List<Long>,
        state: SyncState,
    )

    @Query("DELETE FROM activity_events WHERE syncState = 'SYNCED' AND timestamp < :before")
    suspend fun deleteSyncedOlderThan(before: Long)
}

@Dao
interface PendingOperationDao {
    @Insert
    suspend fun insert(operation: PendingOperationEntity): Long

    @Query(
        """
        SELECT * FROM pending_operations
        WHERE nextAttemptAt <= :now
        ORDER BY nextAttemptAt ASC
        LIMIT :limit
        """,
    )
    suspend fun findReadyForRetry(
        now: Long,
        limit: Int = DEFAULT_BATCH,
    ): List<PendingOperationEntity>

    @Query(
        """
        SELECT * FROM pending_operations
        WHERE type = :type AND nextAttemptAt <= :now
        ORDER BY nextAttemptAt ASC
        """,
    )
    suspend fun findReadyForRetryByType(
        type: PendingOperationType,
        now: Long,
    ): List<PendingOperationEntity>

    @Query(
        """
        UPDATE pending_operations
        SET attempts = attempts + 1, nextAttemptAt = :nextAttemptAt
        WHERE id = :id
        """,
    )
    suspend fun recordAttempt(
        id: Long,
        nextAttemptAt: Long,
    )

    @Query("DELETE FROM pending_operations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM pending_operations")
    suspend fun count(): Int

    @Query("DELETE FROM pending_operations WHERE attempts >= :maxAttempts")
    suspend fun deleteExhausted(maxAttempts: Int)

    companion object {
        const val DEFAULT_BATCH = 20
    }
}

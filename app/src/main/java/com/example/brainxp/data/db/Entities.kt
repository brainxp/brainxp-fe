package com.example.brainxp.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "materials")
data class MaterialEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String,
    val charCount: Int,
    val status: String,
    val createdAt: Long,
    val sessionCount: Int = 0,
)

@Entity(
    tableName = "question_sessions",
    foreignKeys = [
        ForeignKey(
            entity = MaterialEntity::class,
            parentColumns = ["id"],
            childColumns = ["materialId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("materialId"), Index("createdAt")],
)
data class QuestionSessionEntity(
    @PrimaryKey val id: String,
    val materialId: String,
    val mode: String,
    val status: String,
    val score: Double? = null,
    val rewardSeconds: Int? = null,
    val createdAt: Long,
)

@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = QuestionSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class QuestionEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val orderIndex: Int,
    val type: String,
    val payload: String,
    val conceptIds: List<String> = emptyList(),
)

@Entity(
    tableName = "answers",
    foreignKeys = [
        ForeignKey(
            entity = QuestionSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index("syncState")],
)
data class AnswerEntity(
    @PrimaryKey val questionId: String,
    val sessionId: String,
    val answer: String,
    val correct: Boolean? = null,
    val explanation: String? = null,
    val answeredAt: Long,
    val syncState: SyncState = SyncState.PENDING,
)

@Entity(tableName = "unlock_sessions", indices = [Index("status")])
data class UnlockSessionEntity(
    @PrimaryKey val id: String,
    val budgetMillis: Long,
    val consumedByPackage: Map<String, Long>,
    val allowedPackages: List<String>,
    val status: UnlockStatus,
)

@Entity(tableName = "restricted_apps", indices = [Index("enabled")])
data class RestrictedAppEntity(
    @PrimaryKey val packageName: String,
    val enabled: Boolean = true,
    val addedAt: Long,
)

@Entity(tableName = "activity_events", indices = [Index("timestamp"), Index("syncState")])
data class ActivityEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: ActivityEventType,
    val timestamp: Long,
    val payload: String? = null,
    val syncState: SyncState = SyncState.PENDING,
)

@Entity(tableName = "ocr_drafts", primaryKeys = ["draftId", "pageIndex"])
data class OcrDraftEntity(
    val draftId: String,
    val pageIndex: Int,
    val text: String,
    val updatedAt: Long,
)

@Entity(tableName = "pending_operations", indices = [Index("nextAttemptAt")])
data class PendingOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: PendingOperationType,
    val payload: String,
    val attempts: Int = 0,
    val nextAttemptAt: Long,
    val createdAt: Long,
)

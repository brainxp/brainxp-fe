package com.example.brainxp.data.repo

import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.db.ActivityEventDao
import com.example.brainxp.data.db.ActivityEventEntity
import com.example.brainxp.data.db.ActivityEventType
import com.example.brainxp.data.db.SyncState
import com.example.brainxp.domain.model.ActivityEvent
import com.example.brainxp.domain.model.ActivityKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomActivityLogRepository
    @Inject
    constructor(
        private val dao: ActivityEventDao,
    ) : ActivityLogRepository {
        override fun observeRecent(limit: Int): Flow<List<ActivityEvent>> =
            dao.observeRecent(limit).map { rows -> rows.map(ActivityEventEntity::toEvent) }

        override suspend fun record(event: ActivityEvent): AppResult<Unit> {
            dao.insert(
                ActivityEventEntity(
                    type = event.kind.toType(),
                    timestamp = event.timestamp,
                    payload = event.payload.takeIf { it.isNotEmpty() }?.let(::encodePayload),
                    syncState = SyncState.PENDING,
                ),
            )
            return AppResult.Success(Unit)
        }

        override suspend fun flushPending(): AppResult<Int> {
            val pending = dao.findBySyncState(SyncState.PENDING, FLUSH_BATCH)
            if (pending.isEmpty()) return AppResult.Success(0)
            dao.updateSyncState(pending.map { it.id }, SyncState.SYNCED)
            return AppResult.Success(pending.size)
        }

        private companion object {
            const val FLUSH_BATCH = 50
        }
    }

private val payloadJson = Json { ignoreUnknownKeys = true }
private val payloadSerializer = MapSerializer(String.serializer(), String.serializer())

private fun encodePayload(payload: Map<String, String>): String = payloadJson.encodeToString(payloadSerializer, payload)

private fun decodePayload(raw: String?): Map<String, String> =
    raw?.let { runCatching { payloadJson.decodeFromString(payloadSerializer, it) }.getOrNull() }.orEmpty()

internal fun ActivityKind.toType(): ActivityEventType = ActivityEventType.valueOf(name)

private fun ActivityEventEntity.toEvent(): ActivityEvent =
    ActivityEvent(
        kind = ActivityKind.valueOf(type.name),
        timestamp = timestamp,
        payload = decodePayload(payload),
    )

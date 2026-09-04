package com.example.brainxp.data.db

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val STRING_LIST = ListSerializer(String.serializer())
private val MILLIS_BY_KEY = MapSerializer(String.serializer(), Long.serializer())

class JsonConverters {
    @TypeConverter
    fun stringListToJson(value: List<String>): String = Json.encodeToString(STRING_LIST, value)

    @TypeConverter
    fun jsonToStringList(value: String): List<String> =
        runCatching {
            Json.decodeFromString(STRING_LIST, value)
        }.getOrDefault(emptyList())

    @TypeConverter
    fun millisByKeyToJson(value: Map<String, Long>): String = Json.encodeToString(MILLIS_BY_KEY, value)

    @TypeConverter
    fun jsonToMillisByKey(value: String): Map<String, Long> =
        runCatching {
            Json.decodeFromString(MILLIS_BY_KEY, value)
        }.getOrDefault(emptyMap())
}

class Converters {
    @TypeConverter
    fun syncStateToString(value: SyncState): String = value.name

    @TypeConverter
    fun stringToSyncState(value: String): SyncState =
        runCatching {
            SyncState.valueOf(value)
        }.getOrDefault(SyncState.PENDING)

    @TypeConverter
    fun unlockStatusToString(value: UnlockStatus): String = value.name

    @TypeConverter
    fun stringToUnlockStatus(value: String): UnlockStatus =
        runCatching {
            UnlockStatus.valueOf(value)
        }.getOrDefault(UnlockStatus.ENDED)

    @TypeConverter
    fun activityEventTypeToString(value: ActivityEventType): String = value.name

    @TypeConverter
    fun stringToActivityEventType(value: String): ActivityEventType = ActivityEventType.valueOf(value)

    @TypeConverter
    fun pendingOperationTypeToString(value: PendingOperationType): String = value.name

    @TypeConverter
    fun stringToPendingOperationType(value: String): PendingOperationType = PendingOperationType.valueOf(value)
}

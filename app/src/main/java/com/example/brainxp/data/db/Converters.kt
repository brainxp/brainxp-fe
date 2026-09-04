package com.example.brainxp.data.db

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun stringListToJson(value: List<String>): String = Json.encodeToString(ListSerializer(String.serializer()), value)

    @TypeConverter
    fun jsonToStringList(value: String): List<String> =
        runCatching {
            Json.decodeFromString(ListSerializer(String.serializer()), value)
        }.getOrDefault(emptyList())

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

package com.example.brainxp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        MaterialEntity::class,
        QuestionSessionEntity::class,
        QuestionEntity::class,
        AnswerEntity::class,
        UnlockSessionEntity::class,
        RestrictedAppEntity::class,
        ActivityEventEntity::class,
        PendingOperationEntity::class,
        NotificationEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
@TypeConverters(Converters::class, JsonConverters::class)
abstract class BrainXPDatabase : RoomDatabase() {
    abstract fun materialDao(): MaterialDao

    abstract fun questionSessionDao(): QuestionSessionDao

    abstract fun questionDao(): QuestionDao

    abstract fun answerDao(): AnswerDao

    abstract fun unlockSessionDao(): UnlockSessionDao

    abstract fun restrictedAppDao(): RestrictedAppDao

    abstract fun activityEventDao(): ActivityEventDao

    abstract fun pendingOperationDao(): PendingOperationDao

    abstract fun notificationDao(): NotificationDao

    companion object {
        const val NAME = "brainxp.db"
    }
}

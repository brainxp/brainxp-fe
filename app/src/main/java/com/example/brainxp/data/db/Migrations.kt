package com.example.brainxp.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `unlock_sessions`")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `unlock_sessions` (" +
                    "`id` TEXT NOT NULL, " +
                    "`budgetMillis` INTEGER NOT NULL, " +
                    "`consumedByPackage` TEXT NOT NULL, " +
                    "`allowedPackages` TEXT NOT NULL, " +
                    "`status` TEXT NOT NULL, " +
                    "PRIMARY KEY(`id`))",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_unlock_sessions_status` ON `unlock_sessions` (`status`)")
        }
    }

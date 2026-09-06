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

val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `ocr_drafts`")
        }
    }

val MIGRATION_3_4 =
    object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `materials_new` (" +
                    "`id` TEXT NOT NULL, `title` TEXT NOT NULL, `type` TEXT NOT NULL, " +
                    "`status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                    "`sessionCount` INTEGER NOT NULL, `questionCount` INTEGER NOT NULL, " +
                    "`assessedLevel` TEXT, `declaredLevel` TEXT, `gateReason` TEXT, " +
                    "PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "INSERT INTO `materials_new` " +
                    "(`id`, `title`, `type`, `status`, `createdAt`, `sessionCount`, `questionCount`) " +
                    "SELECT `id`, `title`, `type`, `status`, `createdAt`, `sessionCount`, 0 FROM `materials`",
            )
            db.execSQL("DROP TABLE `materials`")
            db.execSQL("ALTER TABLE `materials_new` RENAME TO `materials`")
        }
    }

val MIGRATION_5_6 =
    object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `materials` ADD COLUMN `unfinishedSessionId` TEXT")
            db.execSQL("ALTER TABLE `materials` ADD COLUMN `unfinishedAnswered` INTEGER")
            db.execSQL("ALTER TABLE `materials` ADD COLUMN `unfinishedTotal` INTEGER")
        }
    }

val MIGRATION_4_5 =
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `materials` ADD COLUMN `topicSummary` TEXT")
        }
    }

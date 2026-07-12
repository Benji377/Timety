package io.github.benji377.timety.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migrations. The 2.0.0 release shipped schema version 1; every schema change from here on
 * needs a migration tested against the committed schema JSONs, since users update in place.
 */

/**
 * 2.1.0 schema additions: the `quick_habits` table (interval reminders), the `recurring_tasks`
 * / `recurring_occurrences` tables (recurring tasks with their completion log), the
 * `day_ratings` table (end-of-day quality ratings), and the `goals` / `goal_entries` tables
 * (quantified deadline goals with their progress log).
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `quick_habits` (" +
                "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `intervalMinutes` INTEGER NOT NULL, " +
                "`startMinuteOfDay` INTEGER, `endMinuteOfDay` INTEGER, `targetWeekdays` TEXT, " +
                "`isEnabled` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `recurring_tasks` (" +
                "`id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, " +
                "`category` TEXT NOT NULL, " +
                "`dueDate` INTEGER NOT NULL, `unit` TEXT NOT NULL, `interval` INTEGER NOT NULL, " +
                "`daysOfWeek` TEXT, `monthlyMode` TEXT NOT NULL, `monthlyDay` INTEGER, " +
                "`monthlyOrdinal` INTEGER, `monthlyWeekday` INTEGER, " +
                "`reminderOffsetsMinutes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `recurring_occurrences` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`recurringTaskId` TEXT NOT NULL, `completedAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`recurringTaskId`) REFERENCES `recurring_tasks`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_recurring_occurrences_recurringTaskId` " +
                "ON `recurring_occurrences` (`recurringTaskId`)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `day_ratings` (" +
                "`dayKey` TEXT NOT NULL, `rating` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, PRIMARY KEY(`dayKey`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `goals` (" +
                "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, " +
                "`colorValue` INTEGER NOT NULL, `iconCodePoint` INTEGER, " +
                "`targetValue` INTEGER NOT NULL, `unitLabel` TEXT NOT NULL, " +
                "`targetDate` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`completedAt` INTEGER, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `goal_entries` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`goalId` TEXT NOT NULL, `value` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, " +
                "FOREIGN KEY(`goalId`) REFERENCES `goals`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_goal_entries_goalId` ON `goal_entries` (`goalId`)"
        )
    }
}

/** All migrations, in order, to register on the Room builder. */
val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)

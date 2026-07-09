package io.github.benji377.timety.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migrations. The 2.0.0 release shipped schema version 1; every schema change from here on
 * needs a migration tested against the committed schema JSONs, since users update in place.
 */

/** Adds the `quick_habits` table introduced in 2.1.0 for interval reminders. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `quick_habits` (" +
                "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `intervalMinutes` INTEGER NOT NULL, " +
                "`startMinuteOfDay` INTEGER, `endMinuteOfDay` INTEGER, `targetWeekdays` TEXT, " +
                "`isEnabled` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
    }
}

/** All migrations, in order, to register on the Room builder. */
val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)

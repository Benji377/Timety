package io.github.benji377.timety.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Replays the committed schema JSONs to prove that a 2.0.0 database (version 1) upgrades cleanly to
 * 2.1.0 (version 2) without wiping user data.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TimetyDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2_addsNewTablesAndKeepsData() {
        val dbName = "migration-test"

        // Create the released v1 database and seed a habit that must survive the upgrade.
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO habits (id, name, frequency, createdAt, colorValue) " +
                    "VALUES ('h1', 'Read', 0, 0, 123)"
            )
            close()
        }

        // runMigrationsAndValidate throws if the resulting schema doesn't match 2.json exactly.
        val db = helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2)

        db.query("SELECT name FROM habits WHERE id = 'h1'").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("Read", cursor.getString(0))
        }
        // The new quick-habit, recurring-task, day-rating and goal tables exist and are queryable.
        listOf(
            "quick_habits", "recurring_tasks", "recurring_occurrences", "day_ratings",
            "goals", "goal_entries",
        ).forEach { table ->
            db.query("SELECT COUNT(*) FROM $table").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
        db.close()
    }

    @Test
    fun migrate2To3_addsSortOrderAndBackfillsFromCreatedAt() {
        val dbName = "migration-test-2-3"

        // Seed three habits with distinct createdAt values on a v2 database (no sortOrder column
        // yet); the migration must backfill sortOrder from the current createdAt DESC order.
        helper.createDatabase(dbName, 2).apply {
            execSQL(
                "INSERT INTO habits (id, name, frequency, createdAt, colorValue) " +
                    "VALUES ('oldest', 'Oldest', 0, 100, 111)"
            )
            execSQL(
                "INSERT INTO habits (id, name, frequency, createdAt, colorValue) " +
                    "VALUES ('middle', 'Middle', 0, 200, 222)"
            )
            execSQL(
                "INSERT INTO habits (id, name, frequency, createdAt, colorValue) " +
                    "VALUES ('newest', 'Newest', 0, 300, 333)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 3, true, MIGRATION_2_3)

        db.query("SELECT id, sortOrder FROM habits ORDER BY sortOrder ASC").use { cursor ->
            assertEquals(3, cursor.count)
            val orderedIds = generateSequence { if (cursor.moveToNext()) cursor.getString(0) else null }
                .toList()
            assertEquals(listOf("newest", "middle", "oldest"), orderedIds)
        }
        db.close()
    }
}

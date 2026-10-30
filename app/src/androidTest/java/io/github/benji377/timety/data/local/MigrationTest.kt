package io.github.benji377.timety.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Replays the committed schema JSONs to prove each migration upgrades cleanly without wiping user data. */
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

    @Test
    fun migrate3To4_convertsReminderTimeAndKeepsCompletions() {
        val dbName = "migration-test-3-4"

        helper.createDatabase(dbName, 3).apply {
            execSQL(
                "INSERT INTO habits (id, name, frequency, targetTimeMinutes, createdAt, colorValue, sortOrder) " +
                    "VALUES ('timed', 'Timed', 0, 480, 0, 1, 0)"
            )
            execSQL(
                "INSERT INTO habits (id, name, frequency, createdAt, colorValue, sortOrder) " +
                    "VALUES ('untimed', 'Untimed', 0, 0, 2, 1)"
            )
            execSQL("INSERT INTO habit_completions (habitId, completionDate) VALUES ('timed', 100)")
            execSQL("INSERT INTO habit_completions (habitId, completionDate) VALUES ('timed', 200)")
            execSQL("INSERT INTO habit_completions (habitId, completionDate) VALUES ('untimed', 300)")
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 4, true, MIGRATION_3_4)

        db.query("SELECT id, reminderTimes FROM habits ORDER BY sortOrder").use { cursor ->
            assertEquals(2, cursor.count)
            cursor.moveToFirst()
            assertEquals("[480]", cursor.getString(1))
            cursor.moveToNext()
            assertTrue(cursor.isNull(1))
        }
        // Rebuilding `habits` must not take its completions down with it.
        db.query("SELECT COUNT(*) FROM habit_completions").use { cursor ->
            cursor.moveToFirst()
            assertEquals(3, cursor.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrate4To5_addsNullablePlaceAndKeepsTasks() {
        val dbName = "migration-test-4-5"

        helper.createDatabase(dbName, 4).apply {
            execSQL(
                "INSERT INTO tasks (id, title, description, location, priority, category, size, " +
                    "isCompleted, createdAt, reminders) " +
                    "VALUES ('t1', 'Visit', '', 'Central Park', 'MEDIUM', '', 'MEDIUM', 0, 0, '')"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 5, true, MIGRATION_4_5)

        // Existing tasks keep their location text and have no stored place.
        db.query("SELECT location, place FROM tasks WHERE id = 't1'").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("Central Park", cursor.getString(0))
            assertTrue(cursor.isNull(1))
        }
        db.close()
    }

    @Test
    fun migrate5To6_dropsXpColumnsAndKeepsProfile_andAddsRecurringColumns() {
        val dbName = "migration-test-5-6"

        helper.createDatabase(dbName, 5).apply {
            execSQL(
                "INSERT INTO user_profile (id, name, profileImagePath, accountCreated, " +
                    "unlockedAchievements, totalXp) VALUES (1, 'Benji', '/avatar.jpg', 42, 'a|||b', 900)"
            )
            execSQL(
                "INSERT INTO recurring_tasks (id, title, description, category, dueDate, unit, interval, " +
                    "monthlyMode, reminderOffsetsMinutes, createdAt) " +
                    "VALUES ('r1', 'Rent', '', '', 1000, 'MONTH', 1, 'DAY_OF_MONTH', '[]', 1)"
            )
            execSQL("INSERT INTO recurring_occurrences (id, recurringTaskId, completedAt) VALUES (7, 'r1', 500)")
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 6, true, MIGRATION_5_6)

        db.query("SELECT name, profileImagePath, accountCreated FROM user_profile WHERE id = 1").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("Benji", cursor.getString(0))
            assertEquals("/avatar.jpg", cursor.getString(1))
            assertEquals(42L, cursor.getLong(2))
        }
        // Existing recurring rows keep their data; the new columns start empty.
        db.query("SELECT dueDate, timeOfDayMinutes FROM recurring_tasks WHERE id = 'r1'").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(1000L, cursor.getLong(0))
            assertTrue(cursor.isNull(1))
        }
        db.query("SELECT completedAt, previousDueDate FROM recurring_occurrences WHERE id = 7").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(500L, cursor.getLong(0))
            assertTrue(cursor.isNull(1))
        }
        db.close()
    }

    @Test
    fun migrate5To6_numbersSubtasksPerTaskInInsertionOrder() {
        val dbName = "migration-test-5-6-subtasks"

        helper.createDatabase(dbName, 5).apply {
            listOf("t1", "t2").forEach { id ->
                execSQL(
                    "INSERT INTO tasks (id, title, description, location, priority, category, size, " +
                        "isCompleted, createdAt, reminders) " +
                        "VALUES ('$id', 'Task', '', '', 'MEDIUM', '', 'MEDIUM', 0, 0, '')"
                )
            }
            // Interleaved across tasks so numbering must restart per task.
            listOf("s1" to "t1", "s2" to "t2", "s3" to "t1", "s4" to "t1", "s5" to "t2").forEach { (id, taskId) ->
                execSQL(
                    "INSERT INTO subtasks (id, taskId, title, isCompleted) VALUES ('$id', '$taskId', '$id', 0)"
                )
            }
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 6, true, MIGRATION_5_6)

        db.query("SELECT id, sortOrder FROM subtasks ORDER BY taskId, sortOrder").use { cursor ->
            val rows = buildList {
                while (cursor.moveToNext()) add(cursor.getString(0) to cursor.getInt(1))
            }
            assertEquals(
                listOf("s1" to 0, "s3" to 1, "s4" to 2, "s2" to 0, "s5" to 1),
                rows,
            )
        }
        db.close()
    }
}

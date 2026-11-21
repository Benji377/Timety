package io.github.benji377.timety.services.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.data.model.task.RecurrenceUnit
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.RecurringTaskWithOccurrences
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.task.TaskWithSubtasks
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

// org.json is an Android class, so these run under Robolectric.
@RunWith(AndroidJUnit4::class)
class PinBackupTest {
    private val pinnedAt = Instant.parse("2026-11-20T08:00:00Z")
    private val createdAt = Instant.parse("2026-11-01T00:00:00Z")

    @Test
    fun pinsSurviveARoundTrip() {
        val task = TaskEntity(id = "t1", title = "Call the bank", createdAt = createdAt, pinnedAt = pinnedAt)
        val recurring = RecurringTaskEntity(
            id = "r1", title = "Bins", dueDate = createdAt, unit = RecurrenceUnit.WEEK,
            createdAt = createdAt, pinnedAt = pinnedAt,
        )
        val habit = habit(stackName = null)

        assertEquals(pinnedAt, taskFromBackup(TaskWithSubtasks(task, emptyList()).toBackupJson()).task.pinnedAt)
        assertEquals(
            pinnedAt,
            recurringTaskFromBackup(RecurringTaskWithOccurrences(recurring, emptyList()).toBackupJson()).task.pinnedAt,
        )
        assertEquals(pinnedAt, habitFromBackup(HabitWithCompletions(habit, emptyList()).toBackupJson()).habit.pinnedAt)
    }

    @Test
    fun aDoneTaskOrAStackedHabit_restoresUnpinned() {
        val json = JSONObject("""{"id": "t1", "title": "Call", "isCompleted": true, "pinnedAt": "$pinnedAt"}""")
        assertNull(taskFromBackup(json).task.pinnedAt)

        val stacked = HabitWithCompletions(habit(stackName = "Morning"), emptyList()).toBackupJson()
        assertNull(habitFromBackup(stacked).habit.pinnedAt)
    }

    @Test
    fun anOlderBackup_restoresUnpinned() {
        assertNull(taskFromBackup(JSONObject("""{"id": "t1", "title": "Call"}""")).task.pinnedAt)
        assertNull(
            recurringTaskFromBackup(
                JSONObject("""{"id": "r1", "title": "Bins", "dueDate": "2026-11-21T09:00:00Z", "unit": "WEEK"}""")
            ).task.pinnedAt
        )
    }

    private fun habit(stackName: String?) = HabitEntity(
        id = "h1",
        name = "Stretch",
        frequency = HabitFrequency.DAILY,
        createdAt = createdAt,
        colorValue = 0,
        stackName = stackName,
        pinnedAt = pinnedAt,
    )
}

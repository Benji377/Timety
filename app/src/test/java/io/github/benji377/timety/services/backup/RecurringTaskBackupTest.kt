package io.github.benji377.timety.services.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.benji377.timety.data.model.task.OccurrenceStatus
import io.github.benji377.timety.data.model.task.Priority
import io.github.benji377.timety.data.model.task.RecurrenceUnit
import io.github.benji377.timety.data.model.task.RecurringOccurrenceEntity
import io.github.benji377.timety.data.model.task.RecurringSubtaskEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.RecurringTaskWithOccurrences
import io.github.benji377.timety.data.model.task.TaskPlace
import io.github.benji377.timety.data.model.task.TaskSize
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

// org.json is an Android class, so these run under Robolectric.
@RunWith(AndroidJUnit4::class)
class RecurringTaskBackupTest {
    private val task = RecurringTaskEntity(
        id = "r1",
        title = "Water the plants",
        priority = Priority.HIGH,
        size = TaskSize.SMALL,
        location = "Balcony",
        place = TaskPlace(name = "Balcony", details = "Main St 4", latitude = 46.5, longitude = 11.35),
        dueDate = Instant.parse("2026-11-21T09:00:00Z"),
        unit = RecurrenceUnit.WEEK,
        createdAt = Instant.parse("2026-11-01T00:00:00Z"),
    )

    @Test
    fun priorityEffortAndLocation_surviveARoundTrip() {
        val restored = recurringTaskFromBackup(RecurringTaskWithOccurrences(task, emptyList()).toBackupJson()).task

        assertEquals(Priority.HIGH, restored.priority)
        assertEquals(TaskSize.SMALL, restored.size)
        assertEquals("Balcony", restored.location)
        assertEquals(task.place, restored.place)
    }

    @Test
    fun theChecklistAndHistorySnapshots_surviveARoundTrip() {
        val occurrences = listOf(
            RecurringOccurrenceEntity(
                recurringTaskId = "r1",
                completedAt = Instant.parse("2026-11-14T10:00:00Z"),
                previousDueDate = Instant.parse("2026-11-14T09:00:00Z"),
                subtasksDone = 2,
                subtasksTotal = 3,
            ),
            RecurringOccurrenceEntity(recurringTaskId = "r1", completedAt = Instant.parse("2026-11-07T10:00:00Z")),
            RecurringOccurrenceEntity(
                recurringTaskId = "r1",
                completedAt = Instant.parse("2026-10-31T10:00:00Z"),
                status = OccurrenceStatus.SKIPPED,
            ),
        )
        val subtasks = listOf(
            RecurringSubtaskEntity(id = "a", recurringTaskId = "r1", title = "Kitchen", isCompleted = true, sortOrder = 1),
            RecurringSubtaskEntity(id = "b", recurringTaskId = "r1", title = "Floors", sortOrder = 0),
        )

        val restored = recurringTaskFromBackup(RecurringTaskWithOccurrences(task, occurrences, subtasks).toBackupJson())

        assertEquals(listOf("Floors", "Kitchen"), restored.subtasks.map { it.title })
        assertEquals(listOf(false, true), restored.subtasks.map { it.isCompleted })
        val withSnapshot = restored.log.single { it.subtasksTotal != null }
        assertEquals(2, withSnapshot.subtasksDone)
        assertEquals(3, withSnapshot.subtasksTotal)
        assertEquals(occurrences[0].previousDueDate, withSnapshot.previousDueDate)
        assertEquals(
            listOf(OccurrenceStatus.COMPLETED, OccurrenceStatus.COMPLETED, OccurrenceStatus.SKIPPED),
            restored.log.sortedByDescending { it.completedAt }.map { it.status },
        )
    }

    @Test
    fun anOlderBackup_withBareTimestamps_keepsItsHistory() {
        val json = JSONObject(
            """{"id": "r1", "title": "Bins", "dueDate": "2026-11-21T09:00:00Z", "unit": "WEEK",
               "occurrences": ["2026-11-14T10:00:00Z", "not a date"]}"""
        )

        val restored = recurringTaskFromBackup(json)

        assertEquals(Instant.parse("2026-11-14T10:00:00Z"), restored.log.single().completedAt)
        assertNull(restored.log.single().subtasksTotal)
        assertEquals("an old backup only logged completions", OccurrenceStatus.COMPLETED, restored.log.single().status)
        assertTrue(restored.subtasks.isEmpty())
    }

    @Test
    fun aSnapshotThatDoesNotAddUp_isDropped() {
        val json = JSONObject(
            """{"id": "r1", "title": "Bins", "dueDate": "2026-11-21T09:00:00Z", "unit": "WEEK",
               "occurrences": [{"completedAt": "2026-11-14T10:00:00Z", "subtasksDone": 5, "subtasksTotal": 3}]}"""
        )

        val occurrence = recurringTaskFromBackup(json).log.single()

        assertNull(occurrence.subtasksDone)
        assertNull(occurrence.subtasksTotal)
    }

    @Test
    fun anOlderBackupWithoutTheFields_getsANewTasksDefaults() {
        val json = JSONObject(
            """{"id": "r1", "title": "Bins", "dueDate": "2026-11-21T09:00:00Z", "unit": "WEEK"}"""
        )

        val restored = recurringTaskFromBackup(json).task

        assertEquals(Priority.MEDIUM, restored.priority)
        assertEquals(TaskSize.MEDIUM, restored.size)
        assertEquals("", restored.location)
        assertNull(restored.place)
    }
}

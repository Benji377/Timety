package io.github.benji377.timety.services.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.benji377.timety.data.model.task.Priority
import io.github.benji377.timety.data.model.task.RecurrenceUnit
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.RecurringTaskWithOccurrences
import io.github.benji377.timety.data.model.task.TaskPlace
import io.github.benji377.timety.data.model.task.TaskSize
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

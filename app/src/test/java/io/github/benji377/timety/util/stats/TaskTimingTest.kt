package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.task.RecurrenceUnit
import io.github.benji377.timety.data.model.task.RecurringOccurrenceEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.RecurringTaskWithOccurrences
import io.github.benji377.timety.data.model.task.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class TaskTimingTest {
    private val zone = ZoneOffset.UTC
    private val monday = LocalDate.of(2026, 10, 19)
    private val week = monday..monday.plusDays(6)

    private fun at(day: LocalDate, hour: Int = 12): Instant = day.atTime(hour, 0).toInstant(zone)

    private fun task(
        completedAt: Instant?,
        due: Instant? = null,
        createdAt: Instant = at(monday.minusDays(30)),
    ) = TaskEntity(
        id = "t",
        title = "t",
        dueDate = due,
        isCompleted = completedAt != null,
        completedAt = completedAt,
        createdAt = createdAt,
    )

    private fun recurring(vararg occurrences: Pair<Instant, Instant?>) = RecurringTaskWithOccurrences(
        task = RecurringTaskEntity(
            id = "r",
            title = "r",
            dueDate = at(monday.plusDays(30)),
            unit = RecurrenceUnit.WEEK,
            createdAt = at(monday.minusDays(30)),
        ),
        occurrences = occurrences.map { (completedAt, due) ->
            RecurringOccurrenceEntity(recurringTaskId = "r", completedAt = completedAt, previousDueDate = due)
        },
    )

    @Test
    fun dueDateRate_comparesByCalendarDay() {
        val rate = TaskTiming.dueDateRate(
            listOf(
                // Later the same day still counts as on time.
                task(at(monday, 23), due = at(monday, 9)),
                task(at(monday.plusDays(1)), due = at(monday)),
            ),
            emptyList(),
            week,
            zone,
        )
        assertEquals(DueDateRate(1, 2), rate)
        assertEquals(50, rate.percent)
    }

    @Test
    fun dueDateRate_leavesOutTasksWithoutDueDateAndOutsideTheRange() {
        val rate = TaskTiming.dueDateRate(
            listOf(
                task(at(monday)),
                task(at(monday.minusDays(1)), due = at(monday)),
                task(null, due = at(monday)),
            ),
            emptyList(),
            week,
            zone,
        )
        assertEquals(DueDateRate(0, 0), rate)
        assertNull(rate.percent)
    }

    @Test
    fun dueDateRate_countsRecurringOccurrencesWithARecordedDueDate() {
        val rate = TaskTiming.dueDateRate(
            emptyList(),
            listOf(
                recurring(
                    at(monday) to at(monday),
                    at(monday.plusDays(2)) to at(monday.plusDays(1)),
                    // Backdated: no recorded due date.
                    at(monday.plusDays(3)) to null,
                ),
            ),
            week,
            zone,
        )
        assertEquals(DueDateRate(1, 2), rate)
    }

    @Test
    fun medianDays_oddAndEvenCounts() {
        fun finished(days: Long) = task(at(monday), createdAt = at(monday.minusDays(days)))
        assertEquals(
            2.0,
            TaskTiming.medianDaysToFinish(listOf(finished(1), finished(2), finished(9)), week, zone)!!,
            0.001,
        )
        assertEquals(
            1.5,
            TaskTiming.medianDaysToFinish(listOf(finished(1), finished(2)), week, zone)!!,
            0.001,
        )
    }

    @Test
    fun medianDays_ignoresOtherPeriodsAndOpenTasks_nullWhenNoneLeft() {
        val tasks = listOf(task(at(monday.minusDays(2))), task(null))
        assertNull(TaskTiming.medianDaysToFinish(tasks, week, zone))
    }

    @Test
    fun medianDays_backdatedCompletionCountsAsZero() {
        val tasks = listOf(task(at(monday), createdAt = at(monday.plusDays(1))))
        assertEquals(0.0, TaskTiming.medianDaysToFinish(tasks, week, zone)!!, 0.001)
    }
}

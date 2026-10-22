package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.task.RecurrenceUnit
import io.github.benji377.timety.data.model.task.RecurringOccurrenceEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.RecurringTaskWithOccurrences
import io.github.benji377.timety.data.model.task.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class WeeklySummaryTest {

    private val zone: ZoneId = ZoneOffset.UTC

    // Monday Oct 19 2026 to Sunday Oct 25 2026.
    private val monday = LocalDate.of(2026, 10, 19)
    private val sunday = monday.plusDays(6)

    private fun at(day: LocalDate, hour: Int, minute: Int = 0): Instant =
        day.atTime(hour, minute).atZone(zone).toInstant()

    private fun task(id: String, due: Instant?, completedAt: Instant? = null) = TaskEntity(
        id = id,
        title = id,
        dueDate = due,
        isCompleted = completedAt != null,
        completedAt = completedAt,
        createdAt = at(monday.minusDays(30), 9),
    )

    private fun recurring(id: String, nextDue: Instant, vararg completions: Instant) =
        RecurringTaskWithOccurrences(
            task = RecurringTaskEntity(
                id = id,
                title = id,
                dueDate = nextDue,
                unit = RecurrenceUnit.WEEK,
                createdAt = at(monday.minusDays(30), 9),
            ),
            occurrences = completions.map {
                RecurringOccurrenceEntity(recurringTaskId = id, completedAt = it)
            },
        )

    private fun session(start: Instant, minutes: Int) = FocusSessionEntity(
        id = "s-${start.epochSecond}",
        modeId = "mode",
        startTime = start,
        endTime = start.plusSeconds(minutes * 60L),
        totalSecondsFocused = minutes * 60,
    )

    private fun summarize(
        tasks: List<TaskEntity> = emptyList(),
        recurring: List<RecurringTaskWithOccurrences> = emptyList(),
        habits: List<Instant> = emptyList(),
        sessions: List<FocusSessionEntity> = emptyList(),
    ) = WeeklySummary.of(monday, tasks, recurring, habits, sessions, zone)

    @Test
    fun tasks_dueAndCompletedCountedSeparately() {
        val summary = summarize(
            tasks = listOf(
                task("due-open", at(monday, 10)),
                task("due-done", at(sunday, 10), completedAt = at(sunday, 9)),
                // Due last week but finished this week: completed here, due there.
                task("late", at(monday.minusDays(3), 10), completedAt = at(monday, 12)),
                task("next-week", at(sunday.plusDays(1), 10)),
                task("no-due-done", null, completedAt = at(monday.plusDays(2), 8)),
            )
        )
        assertEquals(2, summary.tasksDue)
        assertEquals(3, summary.tasksCompleted)
    }

    @Test
    fun recurring_completionAndNextDueInSameWeek_isNotDoubleCountedAsDue() {
        // A daily-style task done on Monday whose next due date is Tuesday of the same week.
        val summary = summarize(
            recurring = listOf(recurring("r", at(monday.plusDays(1), 9), at(monday, 9)))
        )
        assertEquals(1, summary.tasksDue)
        assertEquals(1, summary.tasksCompleted)
    }

    @Test
    fun habits_onlyThisWeeksCheckInsCount() {
        val summary = summarize(
            habits = listOf(at(monday, 7), at(sunday, 23, 59), at(monday.minusDays(1), 7))
        )
        assertEquals(2, summary.habitCheckIns)
    }

    @Test
    fun focus_sessionsByStartDay_minutesSplitAcrossWeekBoundaries() {
        val summary = summarize(
            sessions = listOf(
                session(at(monday, 9), 45),
                // Starts last Sunday 23:00, runs 2h: one hour belongs to this week.
                session(at(monday.minusDays(1), 23), 120),
                // Starts this Sunday 23:30, runs 1h: only the first 30 minutes are this week's.
                session(at(sunday, 23, 30), 60),
            )
        )
        assertEquals(2, summary.focusSessions)
        assertEquals(45 + 60 + 30, summary.focusMinutes)
    }

    @Test
    fun emptyWeek_isAllZeros() {
        assertEquals(WeeklySummary(0, 0, 0, 0, 0), summarize())
    }
}

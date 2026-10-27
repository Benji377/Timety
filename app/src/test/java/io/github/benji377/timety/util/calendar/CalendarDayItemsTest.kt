package io.github.benji377.timety.util.calendar

import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.data.model.task.Priority
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.task.TaskWithSubtasks
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class CalendarDayItemsTest {
    private val zone = ZoneOffset.UTC
    private val monday = LocalDate.of(2026, 10, 26)
    private fun noonOf(day: LocalDate): Instant = day.atTime(12, 0).toInstant(zone)

    private fun habit(id: String, frequency: HabitFrequency, weekdays: String? = null, doneOn: LocalDate? = null) =
        HabitWithCompletions(
            HabitEntity(
                id = id, name = id, frequency = frequency, targetWeekdays = weekdays,
                createdAt = Instant.EPOCH, colorValue = 0,
            ),
            listOfNotNull(doneOn?.let { HabitCompletionEntity(habitId = id, completionDate = noonOf(it)) }),
        )

    private fun task(id: String, due: LocalDate, priority: Priority) = TaskWithSubtasks(
        TaskEntity(id = id, title = id, dueDate = noonOf(due), priority = priority, createdAt = Instant.EPOCH),
        emptyList(),
    )

    private fun itemsOn(
        day: LocalDate,
        tasks: List<TaskWithSubtasks> = emptyList(),
        habits: List<HabitWithCompletions> = emptyList(),
    ) = CalendarDayItems.of(day, tasks, emptyList(), habits, emptyList(), zone)

    @Test
    fun tasks_dueThatDay_highestPriorityFirst() {
        val items = itemsOn(
            monday,
            tasks = listOf(
                task("low", monday, Priority.LOW),
                task("tomorrow", monday.plusDays(1), Priority.VERY_HIGH),
                task("high", monday, Priority.HIGH),
            ),
        )
        assertEquals(listOf("high", "low"), items.tasks.map { it.task.id })
    }

    @Test
    fun habits_scheduledFirst_thenOthersDoneThatDay() {
        val habits = listOf(
            habit("monthlyDone", HabitFrequency.MONTHLY_FLEXIBLE, doneOn = monday),
            habit("monthly", HabitFrequency.MONTHLY_FLEXIBLE),
            habit("mondays", HabitFrequency.WEEKLY_EXACT, weekdays = "[1]"),
            habit("tuesdays", HabitFrequency.WEEKLY_EXACT, weekdays = "[2]"),
            habit("daily", HabitFrequency.DAILY, doneOn = monday),
        )
        assertEquals(
            listOf("mondays", "daily", "monthlyDone"),
            itemsOn(monday, habits = habits).habits.map { it.habit.id },
        )
    }
}

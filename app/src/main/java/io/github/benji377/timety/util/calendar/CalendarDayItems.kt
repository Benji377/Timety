package io.github.benji377.timety.util.calendar

import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.data.model.task.RecurringOccurrenceEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.RecurringTaskWithOccurrences
import io.github.benji377.timety.data.model.task.TaskWithSubtasks
import io.github.benji377.timety.util.habit.HabitUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** What the calendar lists under one selected day. */
data class CalendarDayItems(
    /** Tasks due that day, highest priority first. */
    val tasks: List<TaskWithSubtasks>,
    /**
     * Recurring tasks whose next due date is that day. Completing one advances its due date, so
     * it moves on to the next cycle while [recurringDone] keeps the day marked.
     */
    val recurringDue: List<RecurringTaskEntity>,
    /** Recurring occurrences completed that day, in completion order. */
    val recurringDone: List<Pair<RecurringTaskEntity, RecurringOccurrenceEntity>>,
    /** Habits scheduled that day, then any others completed on it. */
    val habits: List<HabitWithCompletions>,
    /** Focus sessions started that day, in start order. */
    val sessions: List<FocusSessionEntity>,
) {
    companion object {
        fun of(
            day: LocalDate,
            tasks: List<TaskWithSubtasks>,
            recurring: List<RecurringTaskWithOccurrences>,
            habits: List<HabitWithCompletions>,
            sessions: List<FocusSessionEntity>,
            zone: ZoneId,
        ): CalendarDayItems {
            fun Instant.isOnDay() = atZone(zone).toLocalDate() == day
            return CalendarDayItems(
                tasks = tasks.filter { it.task.dueDate?.isOnDay() == true }
                    .sortedByDescending { it.task.priority.ordinal },
                recurringDue = recurring.map { it.task }
                    .filter { it.dueDate.isOnDay() }
                    .sortedBy { it.dueDate },
                recurringDone = recurring.flatMap { item ->
                    item.occurrences.filter { it.completedAt.isOnDay() }.map { item.task to it }
                }.sortedBy { it.second.completedAt },
                habits = (habits.filter { isScheduledOn(it, day) } + habits.filter { HabitUtils.isCompletedOn(it, day) })
                    .distinctBy { it.habit.id },
                sessions = sessions.filter { it.startTime.isOnDay() }.sortedBy { it.startTime },
            )
        }

        /** Monthly flexible habits have no due days, so they show only on days they were done. */
        private fun isScheduledOn(hwc: HabitWithCompletions, date: LocalDate): Boolean =
            when (hwc.habit.frequency) {
                HabitFrequency.DAILY, HabitFrequency.WEEKLY_FLEXIBLE -> true
                HabitFrequency.WEEKLY_EXACT -> date.dayOfWeek.value in HabitUtils.parseWeekdays(hwc.habit.targetWeekdays)
                HabitFrequency.MONTHLY_FLEXIBLE -> false
            }
    }
}

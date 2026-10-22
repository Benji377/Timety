package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.task.RecurringTaskWithOccurrences
import io.github.benji377.timety.data.model.task.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


/** What happened in one Monday-to-Sunday week, as shown by the calendar's weekly column and report. */
data class WeeklySummary(
    /** One-off tasks due this week, plus recurring tasks whose next due date falls in it. */
    val tasksDue: Int,
    /** One-off tasks completed this week, plus recurring occurrences logged in it. */
    val tasksCompleted: Int,
    val habitCheckIns: Int,
    /** Sessions that started this week. */
    val focusSessions: Int,
    /** Focused minutes that fell in this week; a session crossing midnight is split by day. */
    val focusMinutes: Int,
) {
    companion object {
        /**
         * Summarizes the week starting on [weekStart]. Recurring tasks count once for their next
         * due date and once per logged completion, never twice for the same completion.
         */
        fun of(
            weekStart: LocalDate,
            tasks: List<TaskEntity>,
            recurring: List<RecurringTaskWithOccurrences>,
            habitCompletions: List<Instant>,
            sessions: List<FocusSessionEntity>,
            zone: ZoneId,
        ): WeeklySummary {
            val weekEnd = weekStart.plusDays(6)
            fun inWeek(day: LocalDate) = !day.isBefore(weekStart) && !day.isAfter(weekEnd)
            fun inWeek(instant: Instant) = inWeek(instant.atZone(zone).toLocalDate())

            val weekSessions = sessions.filter { inWeek(it.startTime) }
            // Neighbouring days are included so a session that started last Sunday night still
            // contributes the minutes it spent past midnight to this week.
            val focusMinutes = FocusSessionDayStats
                .minutesByDay(
                    sessions.filter {
                        val day = it.startTime.atZone(zone).toLocalDate()
                        !day.isBefore(weekStart.minusDays(1)) && !day.isAfter(weekEnd)
                    },
                    zone
                )
                .filterKeys { inWeek(it) }
                .values.sum()

            return WeeklySummary(
                tasksDue = tasks.count { it.dueDate != null && inWeek(it.dueDate) } +
                    recurring.count { inWeek(it.task.dueDate) },
                tasksCompleted = tasks.count { it.isCompleted && it.completedAt != null && inWeek(it.completedAt) } +
                    recurring.sumOf { item -> item.occurrences.count { inWeek(it.completedAt) } },
                habitCheckIns = habitCompletions.count { inWeek(it) },
                focusSessions = weekSessions.size,
                focusMinutes = focusMinutes,
            )
        }
    }
}

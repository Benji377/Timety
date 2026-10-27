package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.task.RecurringTaskWithOccurrences
import io.github.benji377.timety.data.model.task.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** A calendar report's span: a Monday-to-Sunday week or a calendar month. */
enum class ReportPeriod {
    WEEK,
    MONTH;

    /** Last day of the period starting on [start]. */
    fun endOf(start: LocalDate): LocalDate = when (this) {
        WEEK -> start.plusDays(6)
        MONTH -> start.plusMonths(1).minusDays(1)
    }

    /** First day of the period before the one starting on [start]. */
    fun previousStart(start: LocalDate): LocalDate = when (this) {
        WEEK -> start.minusWeeks(1)
        MONTH -> start.minusMonths(1)
    }

    /**
     * The days of the previous period to compare the one starting on [start] against. While that
     * period is still running on [today], only as many days as have elapsed so far, so an
     * unfinished month isn't judged against a whole one; once it is over, the full previous one.
     */
    fun comparisonRange(start: LocalDate, today: LocalDate): ClosedRange<LocalDate> {
        val previousStart = previousStart(start)
        val previousEnd = endOf(previousStart)
        if (today.isBefore(start) || today.isAfter(endOf(start))) return previousStart..previousEnd
        val elapsedDays = ChronoUnit.DAYS.between(start, today)
        // March 31 has no February counterpart, so the comparison stops at February's end.
        return previousStart..minOf(previousStart.plusDays(elapsedDays), previousEnd)
    }

    /**
     * The moment in the [comparisonRange] that matches [now], so its last day counts only up to
     * the same time of day as today does. Null when that range is compared in full.
     */
    fun comparisonCutoff(start: LocalDate, now: ZonedDateTime): Instant? {
        val today = now.toLocalDate()
        if (today.isBefore(start) || today.isAfter(endOf(start))) return null
        val previousStart = previousStart(start)
        val day = previousStart.plusDays(ChronoUnit.DAYS.between(start, today))
        if (day.isAfter(endOf(previousStart))) return null
        return day.atTime(now.toLocalTime()).atZone(now.zone).toInstant()
    }
}

/** What happened between two days, as shown by the calendar's weekly column and reports. */
data class PeriodSummary(
    /** One-off tasks due in the range, plus recurring tasks whose next due date falls in it. */
    val tasksDue: Int,
    /** One-off tasks completed in the range, plus recurring occurrences logged in it. */
    val tasksCompleted: Int,
    val habitCheckIns: Int,
    /** Sessions that started in the range. */
    val focusSessions: Int,
    /** Focused minutes that fell in the range; a session crossing midnight is split by day. */
    val focusMinutes: Int,
) {
    companion object {
        /**
         * Summarizes [range], both ends inclusive, or only up to [until] when given. Recurring
         * tasks count once for their next due date and once per logged completion, never twice
         * for the same completion.
         */
        fun of(
            range: ClosedRange<LocalDate>,
            tasks: List<TaskEntity>,
            recurring: List<RecurringTaskWithOccurrences>,
            habitCompletions: List<Instant>,
            sessions: List<FocusSessionEntity>,
            zone: ZoneId,
            until: Instant? = null,
        ): PeriodSummary {
            // The range's bounds as instants, so each item is a comparison rather than a time-zone
            // conversion: a calendar month summarizes every completion once per week row.
            val start = range.start.atStartOfDay(zone).toInstant()
            val rangeEnd = range.endInclusive.plusDays(1).atStartOfDay(zone).toInstant()
            val end = until?.let { minOf(it, rangeEnd) } ?: rangeEnd
            fun inRange(instant: Instant) = !instant.isBefore(start) && instant.isBefore(end)

            // The day before is included so a session that started just before midnight still
            // contributes the minutes it spent inside the range.
            val focusMinutes = FocusSessionDayStats
                .minutesByDay(
                    sessions.filter {
                        it.startTime.isBefore(end) &&
                            it.startTime.atZone(zone).toLocalDate() in range.start.minusDays(1)..range.endInclusive
                    },
                    zone
                )
                .filterKeys { it in range }
                .values.sum()

            return PeriodSummary(
                tasksDue = tasks.count { it.dueDate != null && inRange(it.dueDate) } +
                    recurring.count { inRange(it.task.dueDate) },
                tasksCompleted = tasks.count { it.isCompleted && it.completedAt != null && inRange(it.completedAt) } +
                    recurring.sumOf { item -> item.occurrences.count { inRange(it.completedAt) } },
                habitCheckIns = habitCompletions.count { inRange(it) },
                focusSessions = sessions.count { inRange(it.startTime) },
                focusMinutes = focusMinutes,
            )
        }
    }
}

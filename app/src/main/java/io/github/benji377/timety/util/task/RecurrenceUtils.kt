package io.github.benji377.timety.util.task

import io.github.benji377.timety.data.model.task.MonthlyMode
import io.github.benji377.timety.data.model.task.RecurrenceUnit
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.util.datetime.AppDateUtils
import io.github.benji377.timety.util.habit.HabitUtils
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * How a recurring task's next occurrence relates to now: actionable states first, then
 * [SCHEDULED] for anything beyond the upcoming horizon (not yet worth surfacing or completing).
 */
enum class RecurringStatus {
    OVERDUE,
    DUE_TODAY,
    UPCOMING,
    SCHEDULED
}

/** Schedule-anchored recurrence math for recurring tasks. Pure and unit-testable. */
object RecurrenceUtils {

    /**
     * Buckets [task]'s next due date against [now]: before now = [RecurringStatus.OVERDUE],
     * later today = [RecurringStatus.DUE_TODAY], within [horizonDays] = [RecurringStatus.UPCOMING],
     * beyond = [RecurringStatus.SCHEDULED].
     */
    fun statusOf(
        task: RecurringTaskEntity,
        now: Instant,
        horizonDays: Int,
        zone: ZoneId = ZoneId.systemDefault(),
    ): RecurringStatus {
        val today = now.atZone(zone).toLocalDate()
        val dueDay = task.dueDate.atZone(zone).toLocalDate()
        return when {
            task.dueDate.isBefore(now) -> RecurringStatus.OVERDUE
            dueDay == today -> RecurringStatus.DUE_TODAY
            !dueDay.isAfter(today.plusDays(horizonDays.toLong())) -> RecurringStatus.UPCOMING
            else -> RecurringStatus.SCHEDULED
        }
    }

    /**
     * The next due date strictly after both [task]'s current due date and [now]: it walks the
     * task's occurrence dates forward and skips everything not in the future, so completing a
     * long-overdue task jumps straight to the next real occurrence instead of firing every missed
     * one. The time of day is carried over from the current due date.
     */
    fun nextDueDate(
        task: RecurringTaskEntity,
        now: Instant,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Instant {
        val zonedDue = task.dueDate.atZone(zone)
        val anchor = zonedDue.toLocalDate()
        val time = zonedDue.toLocalTime()
        var date = anchor
        var guard = 0
        while (guard < MAX_STEPS) {
            date = nextOccurrenceDate(date, task, anchor)
            // atZone resolves DST-gap local times instead of throwing.
            val instant = date.atTime(time).atZone(zone).toInstant()
            if (instant.isAfter(now)) return instant
            guard++
        }
        return date.atTime(time).atZone(zone).toInstant()
    }

    /** The first occurrence date strictly after [after], with week/month cycles aligned to [anchor]. */
    private fun nextOccurrenceDate(
        after: LocalDate,
        task: RecurringTaskEntity,
        anchor: LocalDate,
    ): LocalDate {
        val interval = task.interval.coerceAtLeast(1)
        return when (task.unit) {
            RecurrenceUnit.WEEK -> nextWeekly(after, task, anchor, interval)
            RecurrenceUnit.MONTH -> nextMonthly(after, task, anchor, interval)
            // plusYears clamps Feb 29 to Feb 28 in non-leap years.
            RecurrenceUnit.YEAR -> after.plusYears(interval.toLong())
        }
    }

    private fun nextWeekly(
        after: LocalDate,
        task: RecurringTaskEntity,
        anchor: LocalDate,
        interval: Int,
    ): LocalDate {
        val days = HabitUtils.parseWeekdays(task.daysOfWeek)
            .ifEmpty { setOf(anchor.dayOfWeek.value) }
        val anchorWeek = AppDateUtils.startOfWeekMonday(anchor)
        var date = after.plusDays(1)
        // Every aligned week contains at least one selected day, so one interval-cycle plus the
        // partial current week is enough to find a match.
        repeat(7 * (interval + 1)) {
            val weeksFromAnchor =
                ChronoUnit.WEEKS.between(anchorWeek, AppDateUtils.startOfWeekMonday(date))
            if (date.dayOfWeek.value in days && weeksFromAnchor % interval == 0L) return date
            date = date.plusDays(1)
        }
        return date
    }

    private fun nextMonthly(
        after: LocalDate,
        task: RecurringTaskEntity,
        anchor: LocalDate,
        interval: Int,
    ): LocalDate {
        val anchorMonth = YearMonth.from(anchor)
        var month = YearMonth.from(after)
        // An aligned month comes up every [interval] months and at most one of them can have its
        // occurrence not-after [after], so two cycles always contain a match.
        repeat(2 * interval + 2) {
            if (ChronoUnit.MONTHS.between(anchorMonth, month) % interval == 0L) {
                val date = occurrenceInMonth(month, task, anchor)
                if (date.isAfter(after)) return date
            }
            month = month.plusMonths(1)
        }
        return occurrenceInMonth(month, task, anchor)
    }

    /** The occurrence date the task's monthly rule lands on inside [month]. */
    private fun occurrenceInMonth(
        month: YearMonth,
        task: RecurringTaskEntity,
        anchor: LocalDate,
    ): LocalDate = when (task.monthlyMode) {
        MonthlyMode.DAY_OF_MONTH -> {
            val day = (task.monthlyDay ?: anchor.dayOfMonth).coerceIn(1, month.lengthOfMonth())
            month.atDay(day)
        }

        MonthlyMode.NTH_WEEKDAY -> {
            val weekday = DayOfWeek.of(task.monthlyWeekday ?: anchor.dayOfWeek.value)
            val ordinal = task.monthlyOrdinal ?: ordinalInMonth(anchor)
            if (ordinal == RecurringTaskEntity.LAST_ORDINAL) {
                month.atEndOfMonth().with(TemporalAdjusters.previousOrSame(weekday))
            } else {
                val date = month.atDay(1)
                    .with(TemporalAdjusters.nextOrSame(weekday))
                    .plusWeeks((ordinal - 1).toLong())
                // A 5th occurrence doesn't exist in every month; clamp to the last one.
                if (YearMonth.from(date) == month) date
                else month.atEndOfMonth().with(TemporalAdjusters.previousOrSame(weekday))
            }
        }
    }

    /** Which occurrence of its weekday [date] is within its month: the 14th is always the 2nd. */
    fun ordinalInMonth(date: LocalDate): Int = (date.dayOfMonth - 1) / 7 + 1

    /** Whether [date] is the last occurrence of its weekday within its month. */
    fun isLastWeekdayOfMonth(date: LocalDate): Boolean =
        date.plusWeeks(1).month != date.month

    private const val MAX_STEPS = 100_000
}

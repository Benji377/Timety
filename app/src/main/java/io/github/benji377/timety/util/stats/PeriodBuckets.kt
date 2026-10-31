package io.github.benji377.timety.util.stats

import io.github.benji377.timety.util.datetime.AppDateUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Splits a report period into the ranges its bar chart draws, and fills them with values. */
object PeriodBuckets {
    /**
     * The bars of the period starting on [start]: 7 days for a week, Monday-based weeks clipped to
     * the month for a month, and 12 months for a year.
     */
    fun of(period: ReportPeriod, start: LocalDate): List<ClosedRange<LocalDate>> = when (period) {
        ReportPeriod.WEEK -> (0L..6L).map { offset -> start.plusDays(offset).let { it..it } }
        ReportPeriod.MONTH -> weeksOfMonth(start, period.endOf(start))
        ReportPeriod.YEAR -> (0L..11L).map { offset ->
            start.plusMonths(offset).let { it..ReportPeriod.MONTH.endOf(it) }
        }
    }

    /** The sum of [valuesByDay] in each of [buckets]. */
    fun sum(buckets: List<ClosedRange<LocalDate>>, valuesByDay: Map<LocalDate, Int>): List<Int> {
        val totals = IntArray(buckets.size)
        valuesByDay.forEach { (day, value) ->
            val index = buckets.indexOfFirst { day in it }
            if (index >= 0) totals[index] += value
        }
        return totals.toList()
    }

    /** How many of [instants] fall in each of [buckets]. */
    fun count(
        buckets: List<ClosedRange<LocalDate>>,
        instants: List<Instant>,
        zone: ZoneId,
    ): List<Int> = sum(buckets, instants.groupingBy { it.atZone(zone).toLocalDate() }.eachCount())

    private fun weeksOfMonth(start: LocalDate, end: LocalDate): List<ClosedRange<LocalDate>> {
        val weeks = mutableListOf<ClosedRange<LocalDate>>()
        var weekStart = start
        while (!weekStart.isAfter(end)) {
            val sunday = AppDateUtils.startOfWeekMonday(weekStart).plusDays(6)
            val weekEnd = minOf(sunday, end)
            weeks += weekStart..weekEnd
            weekStart = weekEnd.plusDays(1)
        }
        return weeks
    }
}

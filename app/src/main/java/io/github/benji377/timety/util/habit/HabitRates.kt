package io.github.benji377.timety.util.habit

import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.util.datetime.AppDateUtils
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Check-ins made against those expected. [percent] is null when none were expected yet. */
data class HabitRate(val done: Int, val expected: Int) {
    val percent: Int? get() = if (expected == 0) null else (done * 100.0 / expected).roundToInt()
}

/**
 * How well habits kept to their schedule in a date range. Only days up to today and from the
 * habit's first day count, so a running period or a new habit isn't judged on days it never had.
 */
object HabitRates {
    private const val DAYS_PER_WEEK = 7
    private const val ROUNDING_TOLERANCE = 1e-9

    /** The days [item] was checked in on. */
    fun completionDays(item: HabitWithCompletions, zone: ZoneId): List<LocalDate> =
        item.completions.map { it.completionDate.atZone(zone).toLocalDate() }

    /** The combined rate of [habits] in [range]. */
    fun total(
        habits: List<HabitWithCompletions>,
        range: ClosedRange<LocalDate>,
        today: LocalDate,
        zone: ZoneId,
    ): HabitRate = total(habits.map { of(it.habit, completionDays(it, zone), range, today, zone) })

    /** The rate of [habit] in [range], given the days in [completions]. */
    fun of(
        habit: HabitEntity,
        completions: List<LocalDate>,
        range: ClosedRange<LocalDate>,
        today: LocalDate,
        zone: ZoneId,
    ): HabitRate {
        val createdOn = habit.createdAt.atZone(zone).toLocalDate()
        val firstDay = minOf(createdOn, completions.minOrNull() ?: createdOn)
        val from = maxOf(range.start, firstDay)
        val to = minOf(range.endInclusive, today)
        if (from.isAfter(to)) return HabitRate(0, 0)

        val doneDays = completions.filterTo(HashSet()) { it in from..to }
        return when (habit.frequency) {
            HabitFrequency.DAILY -> HabitRate(doneDays.size, daysBetween(from, to))
            HabitFrequency.WEEKLY_EXACT -> exactWeekdays(habit, doneDays, from, to)
            HabitFrequency.WEEKLY_FLEXIBLE -> flexible(
                habit, doneDays, from, to,
                periodStart = AppDateUtils::startOfWeekMonday,
                periodEnd = { it.plusDays(DAYS_PER_WEEK - 1L) },
            )

            HabitFrequency.MONTHLY_FLEXIBLE -> flexible(
                habit, doneDays, from, to,
                periodStart = { it.withDayOfMonth(1) },
                periodEnd = { it.plusMonths(1).minusDays(1) },
            )
        }
    }

    /** All [rates] taken together, weighing each check-in equally. */
    fun total(rates: Collection<HabitRate>): HabitRate =
        HabitRate(rates.sumOf { it.done }, rates.sumOf { it.expected })

    private fun exactWeekdays(
        habit: HabitEntity,
        doneDays: Set<LocalDate>,
        from: LocalDate,
        to: LocalDate,
    ): HabitRate {
        val weekdays = HabitUtils.parseWeekdays(habit.targetWeekdays)
            .filterTo(HashSet()) { it in 1..DAYS_PER_WEEK }
            .ifEmpty { (1..DAYS_PER_WEEK).toSet() }
        val expected = generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to) }
            .count { it.dayOfWeek.value in weekdays }
        return HabitRate(doneDays.count { it.dayOfWeek.value in weekdays }, expected)
    }

    // Each week or month expects its target in proportion to the days of it in the range, which
    // also covers a week split by a month's edge. Early check-ins never read above 100%.
    private fun flexible(
        habit: HabitEntity,
        doneDays: Set<LocalDate>,
        from: LocalDate,
        to: LocalDate,
        periodStart: (LocalDate) -> LocalDate,
        periodEnd: (LocalDate) -> LocalDate,
    ): HabitRate {
        val target = habit.targetCount ?: 1
        var expected = 0.0
        var done = 0
        var start = periodStart(from)
        while (!start.isAfter(to)) {
            val end = periodEnd(start)
            val overlapFrom = maxOf(start, from)
            val overlapTo = minOf(end, to)
            expected += target.toDouble() * daysBetween(overlapFrom, overlapTo) / daysBetween(start, end)
            done += minOf(doneDays.count { it in overlapFrom..overlapTo }, target)
            start = end.plusDays(1)
        }
        return HabitRate(done, maxOf(ceil(expected - ROUNDING_TOLERANCE).toInt(), done))
    }

    private fun daysBetween(from: LocalDate, to: LocalDate): Int =
        ChronoUnit.DAYS.between(from, to).toInt() + 1
}

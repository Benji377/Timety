package io.github.benji377.timety.util.habit

import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.util.datetime.AppDateUtils
import io.github.benji377.timety.util.stats.StreakCalculator
import java.time.LocalDate

/**
 * Per-habit streaks, counted in the steps the habit's frequency is scheduled in: days for daily
 * habits, scheduled days for exact-weekday habits, and weeks or months whose target was met for
 * flexible habits. Steps are numbered consecutively so [StreakCalculator]'s never-miss-twice rule applies
 * unchanged: a Monday/Friday habit done every Monday and Friday never shows a gap. History is
 * numbered under the habit's current schedule, so changing it re-reads past completions.
 */
object HabitStreaks {
    /** The current and best streak of [habit] given its [completions], as of [today]. */
    fun streaks(habit: HabitEntity, completions: List<LocalDate>, today: LocalDate): Pair<Int, Int> {
        val met = metSteps(habit, completions)
        return StreakCalculator.currentStreak(met, currentStep(habit, today)) to
            StreakCalculator.bestStreak(met)
    }

    /** The steps [habit] met, numbered as described on [HabitStreaks]. */
    internal fun metSteps(habit: HabitEntity, completions: List<LocalDate>): Set<Long> {
        val weekdays = scheduledWeekdays(habit)
        return when (habit.frequency) {
            HabitFrequency.DAILY -> completions.mapTo(mutableSetOf()) { it.toEpochDay() }
            // Completions on unscheduled days don't count towards the streak.
            HabitFrequency.WEEKLY_EXACT -> completions
                .filter { it.dayOfWeek.value in weekdays }
                .mapTo(mutableSetOf()) {
                    weekIndex(it) * weekdays.size + weekdays.indexOf(it.dayOfWeek.value)
                }

            HabitFrequency.WEEKLY_FLEXIBLE -> periodsMet(completions, habit) { weekIndex(it) }
            HabitFrequency.MONTHLY_FLEXIBLE -> periodsMet(completions, habit) { monthIndex(it) }
        }
    }

    /**
     * The step [today] falls in, which is still in progress and so never counts as a miss. For an
     * exact-weekday habit on an unscheduled day it's the next scheduled day, so the last scheduled
     * day before it already counts.
     */
    internal fun currentStep(habit: HabitEntity, today: LocalDate): Long {
        val weekdays = scheduledWeekdays(habit)
        return when (habit.frequency) {
            HabitFrequency.DAILY -> today.toEpochDay()
            HabitFrequency.WEEKLY_EXACT ->
                weekIndex(today) * weekdays.size + weekdays.count { it < today.dayOfWeek.value }

            HabitFrequency.WEEKLY_FLEXIBLE -> weekIndex(today)
            HabitFrequency.MONTHLY_FLEXIBLE -> monthIndex(today)
        }
    }

    // An exact-weekday habit always has at least one weekday; all seven keeps a corrupt row
    // from dividing the weeks into zero steps.
    private fun scheduledWeekdays(habit: HabitEntity): List<Int> =
        HabitUtils.parseWeekdays(habit.targetWeekdays).filter { it in 1..7 }.sorted()
            .ifEmpty { (1..7).toList() }

    private fun periodsMet(
        completions: List<LocalDate>,
        habit: HabitEntity,
        periodOf: (LocalDate) -> Long,
    ): Set<Long> {
        val target = habit.targetCount ?: 1
        return completions.groupingBy(periodOf).eachCount().filterValues { it >= target }.keys
    }

    // Mondays are seven epoch days apart, so this numbers Monday-based weeks consecutively.
    private fun weekIndex(date: LocalDate): Long =
        Math.floorDiv(AppDateUtils.startOfWeekMonday(date).toEpochDay(), 7L)

    private fun monthIndex(date: LocalDate): Long = date.year * 12L + date.monthValue - 1
}

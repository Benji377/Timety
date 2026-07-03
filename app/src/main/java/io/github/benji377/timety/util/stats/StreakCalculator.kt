package io.github.benji377.timety.util.stats

import io.github.benji377.timety.util.datetime.AppDateUtils
import java.time.LocalDate

/** Result holder mirroring Dart's `({int current, int highest})` record. */
data class StreakResult(val current: Int, val highest: Int)

/** Utility for calculating habit and activity streaks. Mirrors streak_calculator.dart. */
object StreakCalculator {

    /** Calculates the longest consecutive streak of active days. */
    fun calculateBestStreak(completions: Iterable<LocalDate>): Int {
        val sortedUniqueDates = sortedUniqueDayKeys(completions)
        if (sortedUniqueDates.isEmpty()) return 0

        var highest = 1
        var currentRun = 1

        for (i in 1 until sortedUniqueDates.size) {
            val prev = LocalDate.parse(sortedUniqueDates[i - 1])
            val current = LocalDate.parse(sortedUniqueDates[i])

            if (java.time.temporal.ChronoUnit.DAYS.between(prev, current) == 1L) {
                currentRun++
                if (currentRun > highest) highest = currentRun
            } else {
                currentRun = 1
            }
        }

        return highest
    }

    /** Calculates the current ongoing consecutive streak of active days. */
    fun calculateCurrentStreak(completions: Iterable<LocalDate>): Int {
        val dayKeys = completions.map { AppDateUtils.dayKey(it) }.toSet()
        if (dayKeys.isEmpty()) return 0

        var checkDate = LocalDate.now()

        if (!dayKeys.contains(AppDateUtils.dayKey(checkDate))) {
            val yesterday = checkDate.minusDays(1)
            if (!dayKeys.contains(AppDateUtils.dayKey(yesterday))) return 0
            checkDate = yesterday
        }

        var current = 0
        while (dayKeys.contains(AppDateUtils.dayKey(checkDate))) {
            current++
            checkDate = checkDate.minusDays(1)
        }

        return current
    }

    /** Calculates both the current and best streaks simultaneously. */
    fun calculateBoth(dates: Iterable<LocalDate>): StreakResult {
        return StreakResult(
            current = calculateCurrentStreak(dates),
            highest = calculateBestStreak(dates),
        )
    }

    private fun sortedUniqueDayKeys(dates: Iterable<LocalDate>): List<String> {
        return dates.map { AppDateUtils.dayKey(it) }.toSortedSet().toList()
    }
}

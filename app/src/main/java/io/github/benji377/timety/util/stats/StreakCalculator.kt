package io.github.benji377.timety.util.stats

import io.github.benji377.timety.util.datetime.AppDateUtils
import java.time.LocalDate


/** Computes habit-completion streaks (consecutive-day runs) from a set of completion dates. */
object StreakCalculator {


    /** Longest run of consecutive calendar days with a completion, anywhere in [completions]. */
    fun calculateBestStreak(completions: Iterable<LocalDate>): Int {
        val sortedUniqueDates = sortedUniqueDayKeys(completions)
        if (sortedUniqueDates.isEmpty()) return 0

        var highest = 1
        var currentRun = 1

        // Dates are sorted and deduplicated, so a run only continues when consecutive entries
        // are exactly one day apart; any gap resets the run.
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


    /**
     * Current streak ending today. If today has no completion yet, the streak still counts as
     * active as long as yesterday was completed (so the streak isn't lost until the day passes
     * with no completion); otherwise it walks backward day by day while completions exist.
     */
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


    private fun sortedUniqueDayKeys(dates: Iterable<LocalDate>): List<String> {
        return dates.map { AppDateUtils.dayKey(it) }.toSortedSet().toList()
    }
}

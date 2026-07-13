package io.github.benji377.timety.util.stats

import io.github.benji377.timety.util.datetime.AppDateUtils
import io.github.benji377.timety.util.stats.StreakCalculator.currentStreak
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
     * The forgiving current streak, following the Atomic Habits "never miss twice" rule: a single
     * isolated missed day does not break the run (it just doesn't count), only two consecutive
     * missed days do.
     *
     * @property length completed days in the run; bridged misses do not count.
     * @property streakDayKeys day keys of the completed days in the run.
     * @property bridgedDayKeys day keys of the isolated missed days bridged inside the run (the "bends").
     * @property atRisk true when yesterday was missed and today is not done yet — the next miss breaks it.
     */
    data class CurrentStreak(
        val length: Int,
        val streakDayKeys: Set<String>,
        val bridgedDayKeys: Set<String>,
        val atRisk: Boolean,
    ) {
        companion object {
            val EMPTY = CurrentStreak(0, emptySet(), emptySet(), false)
        }
    }

    /**
     * Walks backward from today applying the never-miss-twice rule. An incomplete *today* is never
     * counted as a miss (the day hasn't ended), so the walk starts at yesterday in that case.
     */
    fun currentStreak(completions: Iterable<LocalDate>): CurrentStreak {
        val dayKeys = completions.map { AppDateUtils.dayKey(it) }.toSet()
        if (dayKeys.isEmpty()) return CurrentStreak.EMPTY

        val today = LocalDate.now()
        val earliest = LocalDate.parse(dayKeys.min())
        fun done(day: LocalDate) = dayKeys.contains(AppDateUtils.dayKey(day))

        // Today leniency: an incomplete today isn't a miss, so begin the walk at yesterday.
        var checkDate = if (done(today)) today else today.minusDays(1)

        val completed = mutableSetOf<String>()
        val bridged = mutableSetOf<String>()
        val pendingMisses = mutableListOf<String>()
        var consecutiveMisses = 0

        while (!checkDate.isBefore(earliest)) {
            val key = AppDateUtils.dayKey(checkDate)
            if (dayKeys.contains(key)) {
                completed.add(key)
                // A miss only counts as a bend once an older completion is found to sandwich it.
                bridged.addAll(pendingMisses)
                pendingMisses.clear()
                consecutiveMisses = 0
            } else {
                consecutiveMisses++
                if (consecutiveMisses >= 2) break
                pendingMisses.add(key)
            }
            checkDate = checkDate.minusDays(1)
        }

        val atRisk = completed.isNotEmpty() && !done(today) && !done(today.minusDays(1))
        return CurrentStreak(completed.size, completed, bridged, atRisk)
    }

    /** Length of the forgiving [currentStreak]; used by the profile and per-habit stats. */
    fun calculateCurrentStreak(completions: Iterable<LocalDate>): Int =
        currentStreak(completions).length


    private fun sortedUniqueDayKeys(dates: Iterable<LocalDate>): List<String> {
        return dates.map { AppDateUtils.dayKey(it) }.toSortedSet().toList()
    }
}

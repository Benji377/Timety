package io.github.benji377.timety.util.stats

import io.github.benji377.timety.util.stats.StreakCalculator.currentStreak
import java.time.LocalDate


/** Computes habit-completion streaks (consecutive-day runs) from a set of completion dates. */
object StreakCalculator {


    /**
     * Longest run anywhere in [completions], under the same never-miss-twice rule as
     * [currentStreak]: one missed day is bridged (and not counted), two in a row end the run.
     */
    fun calculateBestStreak(completions: Iterable<LocalDate>): Int {
        val sortedUniqueDates = completions.toSortedSet().toList()
        if (sortedUniqueDates.isEmpty()) return 0

        var highest = 1
        var currentRun = 1

        // Dates are sorted and deduplicated: a run continues across a gap of at most one missed day.
        for (i in 1 until sortedUniqueDates.size) {
            val gap = sortedUniqueDates[i].toEpochDay() - sortedUniqueDates[i - 1].toEpochDay()
            if (gap <= 2L) {
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
     * @property streakDays the completed days in the run.
     * @property bridgedDays the isolated missed days bridged inside the run (the "bends").
     * @property atRisk true when yesterday was missed and today is not done yet — the next miss breaks it.
     */
    data class CurrentStreak(
        val length: Int,
        val streakDays: Set<LocalDate>,
        val bridgedDays: Set<LocalDate>,
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
        val days = completions.toSet()
        if (days.isEmpty()) return CurrentStreak.EMPTY

        val today = LocalDate.now()
        val earliest = days.min()
        fun done(day: LocalDate) = day in days

        // Today leniency: an incomplete today isn't a miss, so begin the walk at yesterday.
        var checkDate = if (done(today)) today else today.minusDays(1)

        val completed = mutableSetOf<LocalDate>()
        val bridged = mutableSetOf<LocalDate>()
        val pendingMisses = mutableListOf<LocalDate>()
        var consecutiveMisses = 0

        while (!checkDate.isBefore(earliest)) {
            if (done(checkDate)) {
                completed.add(checkDate)
                // A miss only counts as a bend once an older completion is found to sandwich it.
                bridged.addAll(pendingMisses)
                pendingMisses.clear()
                consecutiveMisses = 0
            } else {
                consecutiveMisses++
                if (consecutiveMisses >= 2) break
                pendingMisses.add(checkDate)
            }
            checkDate = checkDate.minusDays(1)
        }

        val atRisk = completed.isNotEmpty() && !done(today) && !done(today.minusDays(1))
        return CurrentStreak(completed.size, completed, bridged, atRisk)
    }

    /** Length of the forgiving [currentStreak]; used by the profile and per-habit stats. */
    fun calculateCurrentStreak(completions: Iterable<LocalDate>): Int =
        currentStreak(completions).length

}

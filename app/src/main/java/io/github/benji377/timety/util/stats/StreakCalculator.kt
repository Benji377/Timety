package io.github.benji377.timety.util.stats

import io.github.benji377.timety.util.stats.StreakCalculator.currentStreak
import java.time.LocalDate


/**
 * Computes never-miss-twice streaks. The rule runs over numbered steps (consecutive integers, one
 * per day, scheduled day, week, or month); the [LocalDate] overloads use one step per day.
 */
object StreakCalculator {


    /**
     * Longest run anywhere in [completions], under the same never-miss-twice rule as
     * [currentStreak]: one missed day is bridged (and not counted), two in a row end the run.
     */
    fun calculateBestStreak(completions: Iterable<LocalDate>): Int =
        bestStreakOfSteps(completions.map { it.toEpochDay() })


    /** [calculateBestStreak] over numbered steps: a run continues across at most one missed step. */
    fun bestStreakOfSteps(metSteps: Iterable<Long>): Int {
        val sortedSteps = metSteps.toSortedSet().toList()
        if (sortedSteps.isEmpty()) return 0

        var highest = 1
        var currentRun = 1

        // Steps are sorted and deduplicated: a run continues across a gap of at most one missed step.
        for (i in 1 until sortedSteps.size) {
            val gap = sortedSteps[i] - sortedSteps[i - 1]
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

    /** A [CurrentStreak] over numbered steps rather than days. */
    data class StepStreak(
        val completed: Set<Long>,
        val bridged: Set<Long>,
        val atRisk: Boolean,
    )

    /**
     * Walks backward from today applying the never-miss-twice rule. An incomplete *today* is never
     * counted as a miss (the day hasn't ended), so the walk starts at yesterday in that case.
     */
    fun currentStreak(completions: Iterable<LocalDate>): CurrentStreak {
        val streak = currentStreakOfSteps(
            completions.map { it.toEpochDay() },
            LocalDate.now().toEpochDay()
        )
        if (streak.completed.isEmpty()) return CurrentStreak.EMPTY
        return CurrentStreak(
            length = streak.completed.size,
            streakDays = streak.completed.mapTo(mutableSetOf()) { LocalDate.ofEpochDay(it) },
            bridgedDays = streak.bridged.mapTo(mutableSetOf()) { LocalDate.ofEpochDay(it) },
            atRisk = streak.atRisk,
        )
    }

    /**
     * [currentStreak] over numbered steps. [currentStep] is the step still in progress, which is
     * never counted as a miss, so the walk starts at the step before it when it isn't met yet.
     */
    fun currentStreakOfSteps(metSteps: Iterable<Long>, currentStep: Long): StepStreak {
        val steps = metSteps.toSet()
        if (steps.isEmpty()) return StepStreak(emptySet(), emptySet(), false)

        val earliest = steps.min()
        fun done(step: Long) = step in steps

        // Leniency for the step in progress: an incomplete one isn't a miss, so begin before it.
        var checkStep = if (done(currentStep)) currentStep else currentStep - 1

        val completed = mutableSetOf<Long>()
        val bridged = mutableSetOf<Long>()
        val pendingMisses = mutableListOf<Long>()
        var consecutiveMisses = 0

        while (checkStep >= earliest) {
            if (done(checkStep)) {
                completed.add(checkStep)
                // A miss only counts as a bend once an older completion is found to sandwich it.
                bridged.addAll(pendingMisses)
                pendingMisses.clear()
                consecutiveMisses = 0
            } else {
                consecutiveMisses++
                if (consecutiveMisses >= 2) break
                pendingMisses.add(checkStep)
            }
            checkStep--
        }

        val atRisk = completed.isNotEmpty() && !done(currentStep) && !done(currentStep - 1)
        return StepStreak(completed, bridged, atRisk)
    }

    /** Length of the forgiving [currentStreak]. */
    fun calculateCurrentStreak(completions: Iterable<LocalDate>): Int =
        currentStreak(completions).length

}

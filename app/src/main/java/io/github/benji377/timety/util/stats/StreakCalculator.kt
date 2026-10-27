package io.github.benji377.timety.util.stats


/**
 * Computes never-miss-twice streaks over numbered steps (consecutive integers, one per day,
 * scheduled day, week, or month): a single missed step is bridged without counting, two missed
 * steps in a row end the run.
 */
object StreakCalculator {


    /** Longest run anywhere in [metSteps]. */
    fun bestStreak(metSteps: Iterable<Long>): Int {
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
     * Met steps in the run ending at [currentStep]. The step in progress is never counted as a
     * miss, so the walk starts at the step before it when it isn't met yet.
     */
    fun currentStreak(metSteps: Iterable<Long>, currentStep: Long): Int {
        val steps = metSteps.toSet()
        if (steps.isEmpty()) return 0

        val earliest = steps.min()
        var checkStep = if (currentStep in steps) currentStep else currentStep - 1
        var length = 0
        var consecutiveMisses = 0

        while (checkStep >= earliest) {
            if (checkStep in steps) {
                length++
                consecutiveMisses = 0
            } else {
                consecutiveMisses++
                if (consecutiveMisses >= 2) break
            }
            checkStep--
        }

        return length
    }
}

package io.github.benji377.timety.util.stats

import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {

    // Step 0 is the step in progress; -n is n steps ago.
    private fun stepsAgo(vararg n: Long): List<Long> = n.map { -it }

    private fun current(steps: List<Long>) = StreakCalculator.currentStreak(steps, 0L)

    // Current streak — forgiving "never miss twice" rule.

    @Test
    fun empty_isZero() {
        assertEquals(0, current(emptyList()))
    }

    @Test
    fun currentStepOnly_isOne() {
        assertEquals(1, current(stepsAgo(0)))
    }

    @Test
    fun consecutiveSteps_countAll() {
        assertEquals(5, current(stepsAgo(0, 1, 2, 3, 4)))
    }

    @Test
    fun unmetCurrentStepButPreviousMet_streakSurvives() {
        assertEquals(3, current(stepsAgo(1, 2, 3)))
    }

    @Test
    fun isolatedMiss_isBridgedButNotCounted() {
        assertEquals(4, current(stepsAgo(0, 2, 3, 4)))
    }

    @Test
    fun singleMissBeforeUnmetCurrentStep_streakSurvives() {
        assertEquals(1, current(stepsAgo(2)))
    }

    @Test
    fun twoConsecutiveMisses_breakTheStreak() {
        assertEquals(1, current(stepsAgo(0, 3, 4)))
        assertEquals(0, current(stepsAgo(3)))
    }

    // Best streak follows the same never-miss-twice rule as the current streak.

    @Test
    fun bestStreak_bridgesASingleMiss_likeTheCurrentStreak() {
        val steps = stepsAgo(0, 2, 3)
        assertEquals(3, current(steps))
        assertEquals(3, StreakCalculator.bestStreak(steps))
    }

    @Test
    fun bestStreak_endsAtTwoConsecutiveMisses() {
        // Runs of 2 (steps 10, 9) and 4 (steps 6, 5, 3, 2), split by the missed steps 8 and 7.
        assertEquals(4, StreakCalculator.bestStreak(stepsAgo(10, 9, 6, 5, 3, 2)))
    }

    @Test
    fun bestStreak_isNeverBelowTheCurrentStreak() {
        val steps = stepsAgo(0, 2, 4, 6)
        assertEquals(4, current(steps))
        assertEquals(4, StreakCalculator.bestStreak(steps))
    }
}

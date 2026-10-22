package io.github.benji377.timety.util.stats

import io.github.benji377.timety.util.datetime.AppDateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    private fun daysAgo(n: Long): LocalDate = LocalDate.now().minusDays(n)
    private fun key(n: Long): String = AppDateUtils.dayKey(daysAgo(n))

    // Current streak — forgiving "never miss twice" rule.

    @Test
    fun empty_isZero() {
        assertEquals(0, StreakCalculator.calculateCurrentStreak(emptyList()))
    }

    @Test
    fun todayOnly_isOne() {
        assertEquals(1, StreakCalculator.calculateCurrentStreak(listOf(daysAgo(0))))
    }

    @Test
    fun consecutiveDays_countAll() {
        val dates = (0L..4L).map { daysAgo(it) }
        assertEquals(5, StreakCalculator.calculateCurrentStreak(dates))
    }

    @Test
    fun incompleteTodayButYesterdayDone_streakSurvives() {
        // No completion today, but yesterday and before -> streak still active (today leniency).
        val dates = listOf(daysAgo(1), daysAgo(2), daysAgo(3))
        assertEquals(3, StreakCalculator.calculateCurrentStreak(dates))
    }

    @Test
    fun isolatedMiss_bridgesTheStreak() {
        // today done, yesterday MISSED, then three done -> one bend, four completed days.
        val dates = listOf(daysAgo(0), daysAgo(2), daysAgo(3), daysAgo(4))
        val result = StreakCalculator.currentStreak(dates)
        assertEquals(4, result.length)
        assertTrue(result.bridgedDayKeys.contains(key(1)))
        assertFalse(result.streakDayKeys.contains(key(1)))
    }

    @Test
    fun twoConsecutiveMisses_breakTheStreak() {
        // today done, then days 1 and 2 both missed -> only today counts.
        val dates = listOf(daysAgo(0), daysAgo(3), daysAgo(4))
        assertEquals(1, StreakCalculator.calculateCurrentStreak(dates))
    }

    // At-risk detection — the "don't miss twice" brink.

    @Test
    fun atRisk_whenYesterdayMissedAndTodayNotDone() {
        // today not done, yesterday not done, but the day before was -> streak alive and on the brink.
        val dates = listOf(daysAgo(2), daysAgo(3))
        val result = StreakCalculator.currentStreak(dates)
        assertTrue(result.length > 0)
        assertTrue(result.atRisk)
        assertTrue(result.bridgedDayKeys.contains(key(1)))
    }

    @Test
    fun notAtRisk_whenTodayDone() {
        val result = StreakCalculator.currentStreak(listOf(daysAgo(0), daysAgo(1)))
        assertFalse(result.atRisk)
    }

    @Test
    fun notAtRisk_whenYesterdayDone() {
        val result = StreakCalculator.currentStreak(listOf(daysAgo(1), daysAgo(2)))
        assertFalse(result.atRisk)
    }

    // Best streak follows the same never-miss-twice rule as the current streak.

    @Test
    fun bestStreak_bridgesASingleMiss_likeTheCurrentStreak() {
        val dates = listOf(daysAgo(0), daysAgo(2), daysAgo(3))
        assertEquals(3, StreakCalculator.calculateCurrentStreak(dates))
        assertEquals(3, StreakCalculator.calculateBestStreak(dates))
    }

    @Test
    fun bestStreak_endsAtTwoConsecutiveMisses() {
        // Runs of 2 (days 10, 9) and 4 (days 6, 5, 3, 2), split by the missed days 8 and 7.
        val dates = listOf(daysAgo(10), daysAgo(9), daysAgo(6), daysAgo(5), daysAgo(3), daysAgo(2))
        assertEquals(4, StreakCalculator.calculateBestStreak(dates))
    }

    @Test
    fun bestStreak_isNeverBelowTheCurrentStreak() {
        // Every other day: the old strict rule gave "current 4, best 1".
        val dates = listOf(daysAgo(0), daysAgo(2), daysAgo(4), daysAgo(6))
        assertEquals(4, StreakCalculator.calculateCurrentStreak(dates))
        assertEquals(4, StreakCalculator.calculateBestStreak(dates))
    }
}

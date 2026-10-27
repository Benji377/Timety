package io.github.benji377.timety.util.habit

import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class HabitStreaksTest {

    // A Wednesday; its Monday-based week starts on Oct 26.
    private val today = LocalDate.of(2026, 10, 28)

    private fun habit(
        frequency: HabitFrequency,
        targetCount: Int? = null,
        targetWeekdays: String? = null,
    ) = HabitEntity(
        id = "h1",
        name = "Test Habit",
        frequency = frequency,
        targetCount = targetCount,
        targetWeekdays = targetWeekdays,
        createdAt = Instant.EPOCH,
        colorValue = 0,
    )

    private fun oct(vararg days: Int) = days.map { LocalDate.of(2026, 10, it) }

    @Test
    fun daily_countsDaysWithTheNeverMissTwiceRule() {
        // Oct 25 is missed and bridged; today is still open.
        val streaks = HabitStreaks.streaks(habit(HabitFrequency.DAILY), oct(23, 24, 26, 27), today)
        assertEquals(4 to 4, streaks)
    }

    @Test
    fun weeklyExact_countsOnlyScheduledDays() {
        // Every Monday and Friday for three weeks: the gaps between them aren't misses.
        val mondayFriday = habit(HabitFrequency.WEEKLY_EXACT, targetWeekdays = "[1,5]")
        val streaks = HabitStreaks.streaks(mondayFriday, oct(12, 16, 19, 23, 26), today)
        assertEquals(5 to 5, streaks)
    }

    @Test
    fun weeklyExact_twoMissedScheduledDaysBreakTheStreak() {
        // Monday 19 and Friday 23 are both missed.
        val mondayFriday = habit(HabitFrequency.WEEKLY_EXACT, targetWeekdays = "[1,5]")
        val streaks = HabitStreaks.streaks(mondayFriday, oct(12, 16, 26), today)
        assertEquals(1 to 2, streaks)
    }

    @Test
    fun weeklyExact_ignoresCompletionsOnUnscheduledDays() {
        val mondayFriday = habit(HabitFrequency.WEEKLY_EXACT, targetWeekdays = "[1,5]")
        assertEquals(0 to 0, HabitStreaks.streaks(mondayFriday, oct(21, 28), today))
    }

    @Test
    fun weeklyExact_lastScheduledDayCountsOnAnUnscheduledToday() {
        // Today is Wednesday, so Monday 26 is already over: missing it and Friday 23 ends the run.
        val mondayFriday = habit(HabitFrequency.WEEKLY_EXACT, targetWeekdays = "[1,5]")
        assertEquals(0 to 2, HabitStreaks.streaks(mondayFriday, oct(16, 19), today))
    }

    @Test
    fun weeklyExact_openScheduledTodayIsNotAMiss() {
        // Today is Friday 30, scheduled but not done yet.
        val mondayFriday = habit(HabitFrequency.WEEKLY_EXACT, targetWeekdays = "[1,5]")
        val streaks = HabitStreaks.streaks(mondayFriday, oct(12, 16, 19, 23, 26), LocalDate.of(2026, 10, 30))
        assertEquals(5 to 5, streaks)
    }

    @Test
    fun weeklyExact_afterTheWeeksLastScheduledDayItCounts() {
        // Today is Saturday 31: Friday 30 and Monday 26 were both missed.
        val mondayFriday = habit(HabitFrequency.WEEKLY_EXACT, targetWeekdays = "[1,5]")
        val streaks = HabitStreaks.streaks(mondayFriday, oct(23), LocalDate.of(2026, 10, 31))
        assertEquals(0 to 1, streaks)
    }

    @Test
    fun weeklyFlexible_countsWeeksThatMetTheTarget() {
        // Weeks of Oct 5 and Oct 19 met 2x; the week of Oct 12 (1x) is bridged; this week is open.
        val twiceAWeek = habit(HabitFrequency.WEEKLY_FLEXIBLE, targetCount = 2)
        val streaks = HabitStreaks.streaks(twiceAWeek, oct(5, 7, 14, 20, 24, 26), today)
        assertEquals(2 to 2, streaks)
    }

    @Test
    fun weeklyFlexible_metCurrentWeekCounts() {
        val twiceAWeek = habit(HabitFrequency.WEEKLY_FLEXIBLE, targetCount = 2)
        val streaks = HabitStreaks.streaks(twiceAWeek, oct(20, 24, 26, 27), today)
        assertEquals(2 to 2, streaks)
    }

    @Test
    fun weeklyFlexible_twoMissedWeeksBreakTheStreak() {
        // The weeks of Oct 12 and Oct 19 fall short.
        val twiceAWeek = habit(HabitFrequency.WEEKLY_FLEXIBLE, targetCount = 2)
        val streaks = HabitStreaks.streaks(twiceAWeek, oct(5, 7, 14, 20, 26, 27), today)
        assertEquals(1 to 1, streaks)
    }

    @Test
    fun monthlyFlexible_countsMonthsThatMetTheTarget() {
        // August and October met 2x; September (1x) is bridged.
        val twiceAMonth = habit(HabitFrequency.MONTHLY_FLEXIBLE, targetCount = 2)
        val dates = listOf(
            LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 20),
            LocalDate.of(2026, 9, 14),
        ) + oct(1, 27)
        assertEquals(2 to 2, HabitStreaks.streaks(twiceAMonth, dates, today))
    }

    @Test
    fun monthlyFlexible_openMonthAndShortFirstMonthAreNotMisses() {
        // July (the first month, 1x) falls short; October has 1 of 2 so far.
        val twiceAMonth = habit(HabitFrequency.MONTHLY_FLEXIBLE, targetCount = 2)
        val dates = listOf(
            LocalDate.of(2026, 7, 31),
            LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 20),
            LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
        ) + oct(2)
        assertEquals(2 to 2, HabitStreaks.streaks(twiceAMonth, dates, today))
    }

    @Test
    fun monthlyFlexible_monthsRunAcrossTheYearEnd() {
        val twiceAMonth = habit(HabitFrequency.MONTHLY_FLEXIBLE, targetCount = 2)
        val dates = listOf(
            LocalDate.of(2025, 12, 5), LocalDate.of(2025, 12, 30),
            LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 9),
        )
        assertEquals(2 to 2, HabitStreaks.streaks(twiceAMonth, dates, LocalDate.of(2026, 1, 15)))
    }

    @Test
    fun monthlyFlexible_twoMissedMonthsBreakTheStreak() {
        // August and September fall short; October is met.
        val twiceAMonth = habit(HabitFrequency.MONTHLY_FLEXIBLE, targetCount = 2)
        val dates = listOf(
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2),
            LocalDate.of(2026, 8, 3),
        ) + oct(1, 2)
        assertEquals(1 to 1, HabitStreaks.streaks(twiceAMonth, dates, today))
    }
}

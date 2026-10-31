package io.github.benji377.timety.util.habit

import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class HabitRatesTest {
    private val zone = ZoneOffset.UTC

    // Monday Oct 19 2026 to Sunday Oct 25 2026.
    private val monday = LocalDate.of(2026, 10, 19)
    private val sunday = monday.plusDays(6)
    private val week = monday..sunday
    private val afterwards = LocalDate.of(2026, 12, 1)

    private fun habit(
        frequency: HabitFrequency,
        targetCount: Int? = null,
        targetWeekdays: String? = null,
        createdAt: Instant = Instant.EPOCH,
    ) = HabitEntity(
        id = "h",
        name = "h",
        frequency = frequency,
        targetCount = targetCount,
        targetWeekdays = targetWeekdays,
        createdAt = createdAt,
        colorValue = 0,
    )

    private fun oct(vararg days: Int) = days.map { LocalDate.of(2026, 10, it) }

    private fun rate(
        habit: HabitEntity,
        completions: List<LocalDate>,
        range: ClosedRange<LocalDate> = week,
        today: LocalDate = afterwards,
    ) = HabitRates.of(habit, completions, range, today, zone)

    @Test
    fun daily_finishedWeek_expectsEveryDay() {
        val result = rate(habit(HabitFrequency.DAILY), oct(19, 20, 21, 22, 23, 24))
        assertEquals(HabitRate(6, 7), result)
        assertEquals(86, result.percent)
    }

    @Test
    fun daily_runningWeek_expectsOnlyDaysSoFar() {
        val result = rate(habit(HabitFrequency.DAILY), oct(19, 20), today = monday.plusDays(2))
        assertEquals(HabitRate(2, 3), result)
    }

    @Test
    fun daily_countsSeveralCheckInsOnADayOnce() {
        val result = rate(habit(HabitFrequency.DAILY), oct(19, 19, 19))
        assertEquals(HabitRate(1, 7), result)
    }

    @Test
    fun exactWeekdays_finishedWeek_ignoresOffScheduleCheckIns() {
        // Monday, Wednesday, Friday; the Tuesday check-in doesn't count.
        val result = rate(habit(HabitFrequency.WEEKLY_EXACT, targetWeekdays = "[1,3,5]"), oct(19, 20, 21))
        assertEquals(HabitRate(2, 3), result)
    }

    @Test
    fun exactWeekdays_runningWeek_expectsOnlyScheduledDaysSoFar() {
        val result = rate(
            habit(HabitFrequency.WEEKLY_EXACT, targetWeekdays = "[1,3,5]"),
            oct(19),
            today = monday.plusDays(2),
        )
        assertEquals(HabitRate(1, 2), result)
    }

    @Test
    fun weeklyFlexible_finishedWeek_expectsTheTarget_andCapsExtras() {
        val habit = habit(HabitFrequency.WEEKLY_FLEXIBLE, targetCount = 3)
        assertEquals(HabitRate(2, 3), rate(habit, oct(19, 20)))
        assertEquals(HabitRate(3, 3), rate(habit, oct(19, 20, 21, 22, 23)))
    }

    @Test
    fun weeklyFlexible_runningWeek_expectsInProportion() {
        val habit = habit(HabitFrequency.WEEKLY_FLEXIBLE, targetCount = 4)
        // Monday: 4 x 1/7 rounds up to 1.
        assertEquals(HabitRate(0, 1), rate(habit, emptyList(), today = monday))
        // Thursday: 4 x 4/7 rounds up to 3.
        assertEquals(HabitRate(1, 3), rate(habit, oct(19), today = monday.plusDays(3)))
    }

    @Test
    fun weeklyFlexible_earlyCheckIns_neverReadAbove100Percent() {
        val habit = habit(HabitFrequency.WEEKLY_FLEXIBLE, targetCount = 4)
        val result = rate(habit, oct(19, 19, 20), range = monday..sunday, today = monday.plusDays(1))
        assertEquals(HabitRate(2, 2), result)
        assertEquals(100, result.percent)
    }

    @Test
    fun weeklyFlexible_inAMonth_splitsWeeksAtItsEdges() {
        val habit = habit(HabitFrequency.WEEKLY_FLEXIBLE, targetCount = 7)
        val october = LocalDate.of(2026, 10, 1)..LocalDate.of(2026, 10, 31)
        // A target of one a day expects exactly the 31 days of the month.
        assertEquals(31, rate(habit, emptyList(), range = october).expected)
    }

    @Test
    fun monthlyFlexible_finishedMonth_expectsTheTarget() {
        val habit = habit(HabitFrequency.MONTHLY_FLEXIBLE, targetCount = 8)
        val october = LocalDate.of(2026, 10, 1)..LocalDate.of(2026, 10, 31)
        assertEquals(HabitRate(2, 8), rate(habit, oct(1, 2), range = october))
    }

    @Test
    fun monthlyFlexible_runningMonth_expectsInProportion() {
        val habit = habit(HabitFrequency.MONTHLY_FLEXIBLE, targetCount = 8)
        val october = LocalDate.of(2026, 10, 1)..LocalDate.of(2026, 10, 31)
        // Oct 16: 8 x 16/31 rounds up to 5.
        val result = rate(habit, oct(1, 2), range = october, today = LocalDate.of(2026, 10, 16))
        assertEquals(HabitRate(2, 5), result)
    }

    @Test
    fun monthlyFlexible_inAWeek_expectsItsShareOfTheMonth() {
        val habit = habit(HabitFrequency.MONTHLY_FLEXIBLE, targetCount = 8)
        // 8 x 7/31 rounds up to 2.
        assertEquals(HabitRate(1, 2), rate(habit, oct(20)))
    }

    @Test
    fun newHabit_countsFromItsCreationDay() {
        val created = monday.plusDays(4).atStartOfDay(zone).toInstant()
        val result = rate(habit(HabitFrequency.DAILY, createdAt = created), oct(23))
        assertEquals(HabitRate(1, 3), result)
    }

    @Test
    fun backfilledCheckIns_extendTheHabitsFirstDay() {
        val created = monday.plusDays(4).atStartOfDay(zone).toInstant()
        val result = rate(habit(HabitFrequency.DAILY, createdAt = created), oct(20, 23))
        assertEquals(HabitRate(2, 6), result)
    }

    @Test
    fun futurePeriodAndHabitNotStartedYet_expectNothing() {
        val future = rate(habit(HabitFrequency.DAILY), emptyList(), today = monday.minusDays(1))
        assertEquals(HabitRate(0, 0), future)
        assertNull(future.percent)
    }

    @Test
    fun total_weighsEachCheckInEqually() {
        val total = HabitRates.total(listOf(HabitRate(6, 7), HabitRate(1, 1), HabitRate(0, 0)))
        assertEquals(HabitRate(7, 8), total)
        assertEquals(88, total.percent)
    }

    @Test
    fun total_ofHabits_addsTheirRatesFromTheirCompletions() {
        val daily = HabitWithCompletions(
            habit(HabitFrequency.DAILY),
            listOf(
                HabitCompletionEntity(habitId = "h", completionDate = monday.atTime(9, 0).toInstant(zone)),
                HabitCompletionEntity(habitId = "h", completionDate = monday.plusDays(1).atTime(9, 0).toInstant(zone)),
            ),
        )
        val idle = HabitWithCompletions(habit(HabitFrequency.DAILY), emptyList())
        assertEquals(HabitRate(2, 14), HabitRates.total(listOf(daily, idle), week, afterwards, zone))
        assertEquals(listOf(monday, monday.plusDays(1)), HabitRates.completionDays(daily, zone))
    }
}

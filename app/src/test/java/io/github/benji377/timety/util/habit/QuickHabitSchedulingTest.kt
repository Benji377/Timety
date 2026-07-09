package io.github.benji377.timety.util.habit

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class QuickHabitSchedulingTest {

    private val zone = ZoneId.of("UTC")
    private val day = LocalDate.of(2026, 7, 9)

    private fun at(hour: Int, minute: Int = 0): Instant =
        day.atStartOfDay(zone).plusHours(hour.toLong()).plusMinutes(minute.toLong()).toInstant()

    private fun next(
        now: Instant,
        interval: Int,
        start: Int?,
        end: Int?,
        weekdays: Set<Int> = emptySet(),
    ): Instant =
        QuickHabitScheduling.nextTrigger(now, interval, start, end, weekdays, zone)

    @Test
    fun allDay_stepsToNextIntervalBoundary() {
        // Every 2h anchored to midnight; from 09:15 the next boundary is 10:00.
        assertEquals(at(10), next(at(9, 15), 120, null, null))
    }

    @Test
    fun allDay_exactBoundaryAdvancesToFollowing() {
        // At exactly 10:00 the chain moves on to 12:00, never re-firing the same instant.
        assertEquals(at(12), next(at(10), 120, null, null))
    }

    @Test
    fun windowed_beforeWindowOpensFiresAtStart() {
        assertEquals(at(8), next(at(7), 120, 8 * 60, 20 * 60))
    }

    @Test
    fun windowed_insideWindowStepsFromStart() {
        assertEquals(at(10), next(at(9, 15), 120, 8 * 60, 20 * 60))
    }

    @Test
    fun windowed_lastOccurrenceIsInclusiveOfEnd() {
        // 08:00 + 6*2h = 20:00 == window end, which is still a valid nag time.
        assertEquals(at(20), next(at(19, 30), 120, 8 * 60, 20 * 60))
    }

    @Test
    fun windowed_afterEndRollsToNextDayStart() {
        val expected = day.plusDays(1).atStartOfDay(zone).plusHours(8).toInstant()
        assertEquals(expected, next(at(20, 30), 120, 8 * 60, 20 * 60))
    }

    @Test
    fun windowed_atEndRollsToNextDayStart() {
        val expected = day.plusDays(1).atStartOfDay(zone).plusHours(8).toInstant()
        assertEquals(expected, next(at(20), 120, 8 * 60, 20 * 60))
    }

    @Test
    fun oddInterval_thatDoesNotDivideDay() {
        // Every 90 min from midnight: 00:00, 01:30, 03:00, ... from 02:00 the next is 03:00.
        assertEquals(at(3), next(at(2), 90, null, null))
    }

    @Test
    fun weekdays_allowedTodayReturnsNaive() {
        val today = day.dayOfWeek.value
        assertEquals(at(10), next(at(9, 15), 120, 8 * 60, 20 * 60, setOf(today)))
    }

    @Test
    fun weekdays_excludedTodayRollsToNextAllowedDayStart() {
        // Today is excluded; only tomorrow's weekday is allowed, so fire at tomorrow's 08:00.
        val tomorrow = day.plusDays(1).dayOfWeek.value
        val expected = day.plusDays(1).atStartOfDay(zone).plusHours(8).toInstant()
        assertEquals(expected, next(at(9, 15), 120, 8 * 60, 20 * 60, setOf(tomorrow)))
    }

    @Test
    fun weekdays_skipsSeveralDaysToNextAllowed() {
        // Allow only the weekday three days out; the chain jumps straight there (all-day window).
        val target = day.plusDays(3).dayOfWeek.value
        val expected = day.plusDays(3).atStartOfDay(zone).toInstant()
        assertEquals(expected, next(at(9, 15), 120, null, null, setOf(target)))
    }
}

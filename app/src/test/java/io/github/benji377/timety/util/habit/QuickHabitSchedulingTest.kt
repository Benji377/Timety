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

    @Test
    fun overnight_lateEveningStaysInsideTheWindow() {
        // 22:00 to 06:00 every 2h: from 23:15 the next is 00:00 tomorrow, not tomorrow's 22:00.
        val expected = day.plusDays(1).atStartOfDay(zone).toInstant()
        assertEquals(expected, next(at(23, 15), 120, 22 * 60, 6 * 60))
    }

    @Test
    fun overnight_earlyMorningContinuesYesterdaysWindow() {
        // 03:30 is inside the window that opened yesterday at 22:00; the next step is 04:00.
        assertEquals(at(4), next(at(3, 30), 120, 22 * 60, 6 * 60))
    }

    @Test
    fun overnight_daytimeWaitsForTonightsStart() {
        assertEquals(at(22), next(at(9), 120, 22 * 60, 6 * 60))
    }

    @Test
    fun overnight_weekdayIsTheDayTheWindowOpens() {
        // Only today's weekday: tonight's window runs into tomorrow morning, then pauses a week.
        val today = day.dayOfWeek.value
        val expected = day.plusDays(1).atStartOfDay(zone).plusHours(2).toInstant()
        assertEquals(expected, next(day.plusDays(1).atStartOfDay(zone).toInstant(), 120, 22 * 60, 6 * 60, setOf(today)))
    }

    @Test
    fun dstSpringForward_windowStartIsStillEightOClock() {
        // Berlin skips 02:00 to 03:00 on 29 March 2026; the window still opens at 08:00 local.
        val berlin = ZoneId.of("Europe/Berlin")
        val dstDay = LocalDate.of(2026, 3, 29)
        val now = dstDay.atTime(6, 0).atZone(berlin).toInstant()
        val expected = dstDay.atTime(8, 0).atZone(berlin).toInstant()
        assertEquals(expected, QuickHabitScheduling.nextTrigger(now, 120, 8 * 60, 20 * 60, emptySet(), berlin))
    }

    @Test
    fun dstFallBack_stepsKeepTheirClockTimes() {
        // Berlin repeats 02:00 to 03:00 on 25 October 2026; 10:00 is still followed by 12:00 local.
        val berlin = ZoneId.of("Europe/Berlin")
        val dstDay = LocalDate.of(2026, 10, 25)
        val now = dstDay.atTime(10, 0).atZone(berlin).toInstant()
        val expected = dstDay.atTime(12, 0).atZone(berlin).toInstant()
        assertEquals(expected, QuickHabitScheduling.nextTrigger(now, 120, 8 * 60, 20 * 60, emptySet(), berlin))
    }

    @Test
    fun windowLength_coversAllDayOvernightAndEqualEdges() {
        assertEquals(24 * 60, QuickHabitScheduling.windowLengthMinutes(null, null))
        assertEquals(12 * 60, QuickHabitScheduling.windowLengthMinutes(8 * 60, 20 * 60))
        assertEquals(8 * 60, QuickHabitScheduling.windowLengthMinutes(22 * 60, 6 * 60))
        assertEquals(24 * 60, QuickHabitScheduling.windowLengthMinutes(9 * 60, 9 * 60))
    }
}

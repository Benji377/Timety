package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class FocusSessionDayStatsTest {

    private val zone: ZoneId = ZoneOffset.UTC
    private val day1 = LocalDate.of(2026, 10, 18)
    private val day2 = LocalDate.of(2026, 10, 19)

    private fun session(start: Instant, end: Instant?): FocusSessionEntity {
        val seconds = (end ?: start).let { (it.epochSecond - start.epochSecond).toInt() }
        return FocusSessionEntity(
            id = "s-${start.epochSecond}",
            modeId = "mode",
            startTime = start,
            endTime = end,
            totalSecondsFocused = seconds,
        )
    }

    private fun at(day: LocalDate, hour: Int, minute: Int = 0): Instant =
        day.atTime(hour, minute).atZone(zone).toInstant()

    @Test
    fun sameDaySession_allOnStartDay() {
        val s = session(at(day1, 10), at(day1, 11))
        assertEquals(mapOf(day1 to 60), FocusSessionDayStats.minutesByDay(s, zone))
    }

    @Test
    fun crossMidnightSession_splitsEvenly() {
        // 10pm -> 2am: two hours each side of midnight.
        val s = session(at(day1, 22), at(day2, 2))
        assertEquals(mapOf(day1 to 120, day2 to 120), FocusSessionDayStats.minutesByDay(s, zone))
    }

    @Test
    fun crossMidnightSession_splitsProportionally() {
        // 11pm -> 2am: 1h on day1, 2h on day2.
        val s = session(at(day1, 23), at(day2, 2))
        assertEquals(mapOf(day1 to 60, day2 to 120), FocusSessionDayStats.minutesByDay(s, zone))
    }

    @Test
    fun endingExactlyAtMidnight_staysOnStartDay() {
        val s = session(at(day1, 22), at(day2, 0))
        assertEquals(mapOf(day1 to 120), FocusSessionDayStats.minutesByDay(s, zone))
    }

    @Test
    fun openSession_usesLoggedLengthNotWallClock() {
        // No stored end: the span is taken from the 30 minutes logged, not the live wall clock, so
        // a session started at 23:45 is split 15/15 around midnight regardless of when stats render.
        val s = session(at(day1, 23, 45), end = null).copy(totalSecondsFocused = 30 * 60)
        assertEquals(mapOf(day1 to 15, day2 to 15), FocusSessionDayStats.minutesByDay(s, zone))
    }

    @Test
    fun perDayPartsSumToWholeMinuteTotal() {
        // An odd span whose per-day floors don't divide evenly still sums back to the total.
        val s = session(at(day1, 23, 30), at(day2, 1, 1)) // 91 minutes total
        val byDay = FocusSessionDayStats.minutesByDay(s, zone)
        assertEquals(91, byDay.values.sum())
    }
}

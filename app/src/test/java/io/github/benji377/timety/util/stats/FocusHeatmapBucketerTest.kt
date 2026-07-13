package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class FocusHeatmapBucketerTest {

    private val zone: ZoneId = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 7, 12) // a Sunday

    private fun sessionOn(date: LocalDate, minutes: Int): FocusSessionEntity =
        FocusSessionEntity(
            id = "s-$date-$minutes-${System.nanoTime()}",
            modeId = "mode",
            startTime = date.atStartOfDay(zone).plusHours(10).toInstant(),
            totalSecondsFocused = minutes * 60,
        )

    // Grid shape.

    @Test
    fun weeksAreWholeMondayStartWeeks() {
        val grid = FocusHeatmapBucketer.grid(emptyList(), today = today, zone = zone)
        grid.weeks.forEach { week ->
            assertEquals(7, week.days.size)
            assertEquals(java.time.DayOfWeek.MONDAY, week.days.first().date.dayOfWeek)
            assertEquals(java.time.DayOfWeek.SUNDAY, week.days.last().date.dayOfWeek)
        }
    }

    @Test
    fun gridSpans53CompleteWeeks() {
        val grid = FocusHeatmapBucketer.grid(emptyList(), today = today, zone = zone)
        assertEquals(53, grid.weeks.size)
    }

    @Test
    fun lastDayInGrid_isToday() {
        val grid = FocusHeatmapBucketer.grid(emptyList(), today = today, zone = zone)
        val lastInRange = grid.weeks.flatMap { it.days }.last { it.inRange }
        assertEquals(today, lastInRange.date)
    }

    @Test
    fun firstWeek_isAlwaysCompleteNeverPartial() {
        // The oldest column must never be a mostly-blank partial week - only the newest
        // (rightmost) column is allowed to have not-yet-happened padding days.
        val grid = FocusHeatmapBucketer.grid(emptyList(), today = today, zone = zone)
        val firstWeek = grid.weeks.first()
        assertTrue(firstWeek.days.all { it.inRange })
    }

    @Test
    fun futureDaysPastToday_areNotInRange() {
        // Grid extends to the Sunday of today's week; if today isn't Sunday, later days in that
        // last column are in the future and must not be counted.
        val notSunday = LocalDate.of(2026, 7, 8) // a Wednesday
        val grid = FocusHeatmapBucketer.grid(emptyList(), today = notSunday, zone = zone)
        val lastWeek = grid.weeks.last()
        lastWeek.days.filter { it.date.isAfter(notSunday) }.forEach {
            assertTrue(!it.inRange)
        }
        // ...and every earlier week, including the first, stays fully in range regardless.
        grid.weeks.dropLast(1).forEach { week ->
            assertTrue(week.days.all { it.inRange })
        }
    }

    // Minute aggregation.

    @Test
    fun sumsMultipleSessionsOnSameDay() {
        val sessions = listOf(sessionOn(today, 20), sessionOn(today, 25))
        val grid = FocusHeatmapBucketer.grid(sessions, today = today, zone = zone)
        val todayCell = grid.weeks.flatMap { it.days }.first { it.date == today }
        assertEquals(45, todayCell.minutes)
    }

    @Test
    fun sessionOutsideRange_isIgnoredForMinutesButGridStillSized() {
        val tooOld = today.minusDays(400)
        val sessions = listOf(sessionOn(tooOld, 999))
        val grid = FocusHeatmapBucketer.grid(sessions, today = today, zone = zone)
        val totalMinutes = grid.weeks.flatMap { it.days }.sumOf { it.minutes }
        assertEquals(0, totalMinutes)
    }

    // intensityLevel().

    @Test
    fun zeroMinutes_isLevelZero() {
        val grid = FocusHeatmapBucketer.grid(listOf(sessionOn(today, 30)), today = today, zone = zone)
        assertEquals(0, grid.intensityLevel(0))
    }

    @Test
    fun noActiveDays_everyLevelIsZero() {
        val grid = FocusHeatmapBucketer.grid(emptyList(), today = today, zone = zone)
        assertEquals(0, grid.intensityLevel(45))
    }

    @Test
    fun intensityLevels_climbWithQuartiles() {
        // Four distinct active days spanning a range, one session each on different days.
        val d0 = today
        val d1 = today.minusDays(1)
        val d2 = today.minusDays(2)
        val d3 = today.minusDays(3)
        val sessions = listOf(
            sessionOn(d3, 10),
            sessionOn(d2, 20),
            sessionOn(d1, 30),
            sessionOn(d0, 40),
        )
        val grid = FocusHeatmapBucketer.grid(sessions, today = today, zone = zone)
        val levelD3 = grid.intensityLevel(10)
        val levelD0 = grid.intensityLevel(40)
        assertTrue(levelD0 > levelD3)
        assertTrue(levelD0 <= 4)
        assertTrue(levelD3 >= 1)
    }

    @Test
    fun uniformActiveDays_allMapToSameLevel() {
        val sessions = listOf(
            sessionOn(today, 25),
            sessionOn(today.minusDays(1), 25),
            sessionOn(today.minusDays(2), 25),
        )
        val grid = FocusHeatmapBucketer.grid(sessions, today = today, zone = zone)
        assertEquals(grid.intensityLevel(25), grid.intensityLevel(25))
        assertTrue(grid.intensityLevel(25) in 1..4)
    }

    @Test
    fun singleActiveDay_lightUserStillGetsColor() {
        val sessions = listOf(sessionOn(today, 15))
        val grid = FocusHeatmapBucketer.grid(sessions, today = today, zone = zone)
        assertTrue(grid.intensityLevel(15) >= 1)
    }
}

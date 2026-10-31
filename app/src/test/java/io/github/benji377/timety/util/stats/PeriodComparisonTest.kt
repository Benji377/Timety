package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class PeriodComparisonTest {
    private val zone = ZoneOffset.UTC
    private val monday = LocalDate.of(2026, 10, 26)
    private val wednesday = monday.plusDays(2)

    private fun session(day: LocalDate, hour: Int, minutes: Int): FocusSessionEntity {
        val start = day.atTime(hour, 0).toInstant(zone)
        return FocusSessionEntity(
            id = "$day-$hour",
            modeId = "m",
            startTime = start,
            endTime = start.plusSeconds(minutes * 60L),
            totalSecondsFocused = minutes * 60,
        )
    }

    @Test
    fun isRunning_coversThePeriodOnly() {
        assertTrue(ReportPeriod.WEEK.isRunning(monday, monday))
        assertTrue(ReportPeriod.WEEK.isRunning(monday, monday.plusDays(6)))
        assertFalse(ReportPeriod.WEEK.isRunning(monday, monday.minusDays(1)))
        assertFalse(ReportPeriod.WEEK.isRunning(monday, monday.plusDays(7)))
    }

    @Test
    fun comparison_ofARunningWeek_stopsAtTheSameMomentLastWeek() {
        val now = wednesday.atTime(12, 0).atZone(zone)
        val sessions = listOf(
            session(monday.minusDays(7), 9, 30),
            session(wednesday.minusDays(7), 9, 20),
            session(wednesday.minusDays(7), 15, 500),
            session(wednesday.minusDays(6), 9, 500),
        )
        val summary = PeriodSummary.comparison(ReportPeriod.WEEK, monday, now, sessions = sessions)
        assertEquals(50, summary.focusMinutes)
    }

    @Test
    fun comparison_ofAFinishedWeek_usesTheWholeWeekBefore() {
        val now = monday.plusDays(14).atTime(12, 0).atZone(zone)
        val sessions = listOf(
            session(monday.minusDays(7), 9, 30),
            session(monday.minusDays(1), 15, 40),
            session(monday, 9, 500),
        )
        val summary = PeriodSummary.comparison(ReportPeriod.WEEK, monday, now, sessions = sessions)
        assertEquals(70, summary.focusMinutes)
    }
}

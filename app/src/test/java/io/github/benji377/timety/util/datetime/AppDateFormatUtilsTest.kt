package io.github.benji377.timety.util.datetime

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class AppDateFormatUtilsTest {

    @Test
    fun testFormatDuration() {
        assertEquals("00:00", AppDateFormatUtils.formatDuration(0))
        assertEquals("00:59", AppDateFormatUtils.formatDuration(59))
        assertEquals("01:00", AppDateFormatUtils.formatDuration(60))
        assertEquals("25:00", AppDateFormatUtils.formatDuration(25 * 60))
        // Durations over an hour stay in minutes, matching the gauge display.
        assertEquals("90:00", AppDateFormatUtils.formatDuration(90 * 60))
    }

    @Test
    fun testFormatTimeOfDay24Hour() {
        assertEquals("00:00", AppDateFormatUtils.formatTimeOfDay(0, 0, use24Hour = true, locale = Locale.US))
        assertEquals("13:05", AppDateFormatUtils.formatTimeOfDay(13, 5, use24Hour = true, locale = Locale.US))
        assertEquals("23:59", AppDateFormatUtils.formatTimeOfDay(23, 59, use24Hour = true, locale = Locale.US))
    }

    @Test
    fun testFormatDateWithExplicitPattern() {
        val date = LocalDate.of(2026, 7, 5)
        assertEquals("05/07/2026", AppDateFormatUtils.formatDate(date, "dd/MM/yyyy", Locale.US))
        assertEquals("2026-07-05", AppDateFormatUtils.formatDate(date, "yyyy-MM-dd", Locale.US))
    }

    @Test
    fun testFormatShortDateAndMonthYear() {
        val date = LocalDate.of(2026, 7, 5)
        assertEquals("Jul 5", AppDateFormatUtils.formatShortDate(date, Locale.US))
        assertEquals("July 2026", AppDateFormatUtils.formatMonthYear(date, Locale.US))
    }
}

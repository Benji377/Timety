package io.github.benji377.timety.ui.components.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class DatePickerMillisTest {

    private fun utcMidnight(date: LocalDate) =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun earlyMorningAheadOfUtc_staysOnTheLocalDay() {
        // 00:30 in Rome is still the previous day in UTC.
        val zone = ZoneId.of("Europe/Rome")
        val instant = LocalDateTime.of(2026, 10, 23, 0, 30).atZone(zone).toInstant()
        assertEquals(utcMidnight(LocalDate.of(2026, 10, 23)), datePickerMillis(instant, zone))
    }

    @Test
    fun lateEveningBehindUtc_staysOnTheLocalDay() {
        // 23:59 in New York is already the next day in UTC.
        val zone = ZoneId.of("America/New_York")
        val instant = LocalDateTime.of(2026, 10, 22, 23, 59).atZone(zone).toInstant()
        assertEquals(utcMidnight(LocalDate.of(2026, 10, 22)), datePickerMillis(instant, zone))
    }
}

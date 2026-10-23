package io.github.benji377.timety.util.datetime

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class AppDateUtilsTest {

    @Test
    fun dayKey_isZeroPaddedIsoDate() {
        assertEquals("2026-03-07", AppDateUtils.dayKey(LocalDate.of(2026, 3, 7)))
        assertEquals("0999-12-31", AppDateUtils.dayKey(LocalDate.of(999, 12, 31)))
    }

    @Test
    fun dayKey_ignoresTheDefaultLocale() {
        // Keys are stored in the database and backups, so another script's digits must never leak in.
        val original = Locale.getDefault()
        try {
            for (tag in listOf("fa-IR", "ar-EG", "de-DE")) {
                Locale.setDefault(Locale.forLanguageTag(tag))
                assertEquals("2026-10-23", AppDateUtils.dayKey(LocalDate.of(2026, 10, 23)))
            }
        } finally {
            Locale.setDefault(original)
        }
    }
}

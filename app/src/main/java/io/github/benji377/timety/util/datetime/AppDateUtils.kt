package io.github.benji377.timety.util.datetime

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Small calendar-date helpers for week boundaries and day keys. */
object AppDateUtils {
    /** Returns the Monday that starts the week containing [date]. */
    fun startOfWeekMonday(date: LocalDate): LocalDate {
        return date.minusDays((date.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    }

    /**
     * Canonical `yyyy-MM-dd` key for a date, stored for day ratings and in backups. ISO formatting
     * ignores the locale, so the key never picks up another script's digits. In memory, compare
     * [LocalDate]s instead: building keys is far slower.
     */
    fun dayKey(date: LocalDate): String = date.toString()

    /** Short localized weekday name, e.g. "Mon", for an ISO weekday number (1 = Monday). */
    fun weekdayToStringShort(locale: Locale, weekday: Int): String {
        return DayOfWeek.of(weekday).getDisplayName(TextStyle.SHORT, locale)
    }
}

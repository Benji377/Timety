package io.github.benji377.timety.util.datetime

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale


/** Small calendar-date helpers used for week boundaries, range checks, and day comparisons. */
object AppDateUtils {


    /** Compares only the calendar date, ignoring time and zone; `null` never matches. */
    fun isSameDay(a: LocalDate?, b: LocalDate?): Boolean {
        if (a == null || b == null) return false
        return a.year == b.year && a.monthValue == b.monthValue && a.dayOfMonth == b.dayOfMonth
    }


    /** Returns the Monday that starts the week containing [date]. */
    fun startOfWeekMonday(date: LocalDate): LocalDate {
        return date.minusDays((date.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    }


    fun isWithinInclusive(
        value: LocalDate,
        startInclusive: LocalDate,
        endInclusive: LocalDate
    ): Boolean {
        return !value.isBefore(startInclusive) && !value.isAfter(endInclusive)
    }


    /** Canonical `yyyy-MM-dd` key for a date, used to compare/deduplicate dates as strings. */
    fun dayKey(date: LocalDate): String {
        return "%04d-%02d-%02d".format(date.year, date.monthValue, date.dayOfMonth)
    }


    /** Short localized weekday name, e.g. "Mon", for an ISO weekday number (1 = Monday). */
    fun weekdayToStringShort(locale: Locale, weekday: Int): String {
        return DayOfWeek.of(weekday).getDisplayName(TextStyle.SHORT, locale)
    }
}

package io.github.benji377.timety.util.datetime

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Utility functions for date and time comparisons and manipulations. Mirrors date_utils.dart. */
object AppDateUtils {

    /** Checks if two dates represent the same calendar day. */
    fun isSameDay(a: LocalDate?, b: LocalDate?): Boolean {
        if (a == null || b == null) return false
        return a.year == b.year && a.monthValue == b.monthValue && a.dayOfMonth == b.dayOfMonth
    }

    /** Returns the date representing the Monday of the week for the given date. */
    fun startOfWeekMonday(date: LocalDate): LocalDate {
        return date.minusDays((date.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    }

    /** Checks if a date falls within an inclusive range. */
    fun isWithinInclusive(value: LocalDate, startInclusive: LocalDate, endInclusive: LocalDate): Boolean {
        return !value.isBefore(startInclusive) && !value.isAfter(endInclusive)
    }

    /** Generates a unique string key (YYYY-MM-DD) for a given date. */
    fun dayKey(date: LocalDate): String {
        return "%04d-%02d-%02d".format(date.year, date.monthValue, date.dayOfMonth)
    }

    /** Converts a weekday integer (1=Mon..7=Sun) to its short localized string representation. */
    fun weekdayToStringShort(locale: Locale, weekday: Int): String {
        return DayOfWeek.of(weekday).getDisplayName(TextStyle.SHORT, locale)
    }
}

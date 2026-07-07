package io.github.benji377.timety.util.datetime

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale


object AppDateUtils {


    fun isSameDay(a: LocalDate?, b: LocalDate?): Boolean {
        if (a == null || b == null) return false
        return a.year == b.year && a.monthValue == b.monthValue && a.dayOfMonth == b.dayOfMonth
    }


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


    fun dayKey(date: LocalDate): String {
        return "%04d-%02d-%02d".format(date.year, date.monthValue, date.dayOfMonth)
    }


    fun weekdayToStringShort(locale: Locale, weekday: Int): String {
        return DayOfWeek.of(weekday).getDisplayName(TextStyle.SHORT, locale)
    }
}

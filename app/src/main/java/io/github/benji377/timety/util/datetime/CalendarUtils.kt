package io.github.benji377.timety.util.datetime

import java.time.DayOfWeek
import java.time.LocalDate


/** Builds the Monday-start week grid used by the calendar month view. */
object CalendarUtils {


    /**
     * Splits [month] into full Monday-to-Sunday weeks that together cover every day of the
     * month, padded with the trailing/leading days of adjacent months so each week has 7 days.
     */
    fun generateWeeks(month: LocalDate): List<List<LocalDate>> {
        val firstDayOfMonth = LocalDate.of(month.year, month.monthValue, 1)
        val lastDayOfMonth = firstDayOfMonth.plusMonths(1).minusDays(1)

        val offsetToMonday = firstDayOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value
        var currentDay = firstDayOfMonth.minusDays(offsetToMonday.toLong())

        val weeks = mutableListOf<List<LocalDate>>()

        // currentDay is always a Monday here; keep adding full weeks until that week start has
        // passed the end of the month. Inclusive comparison: a month ending on a Monday still
        // needs the week starting on that Monday, or its last day would be missing.
        while (!currentDay.isAfter(lastDayOfMonth)) {
            val week = mutableListOf<LocalDate>()
            for (i in 0 until 7) {
                week.add(currentDay)
                currentDay = currentDay.plusDays(1)
            }
            weeks.add(week)
        }

        return weeks
    }


}

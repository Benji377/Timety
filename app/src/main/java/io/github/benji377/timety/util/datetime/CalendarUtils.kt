package io.github.benji377.timety.util.datetime

import java.time.DayOfWeek
import java.time.LocalDate


object CalendarUtils {


    fun generateWeeks(month: LocalDate): List<List<LocalDate>> {
        val firstDayOfMonth = LocalDate.of(month.year, month.monthValue, 1)
        val lastDayOfMonth = firstDayOfMonth.plusMonths(1).minusDays(1)

        val offsetToMonday = firstDayOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value
        var currentDay = firstDayOfMonth.minusDays(offsetToMonday.toLong())

        val weeks = mutableListOf<List<LocalDate>>()

        while (currentDay.isBefore(lastDayOfMonth) || currentDay.dayOfWeek != DayOfWeek.MONDAY) {
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

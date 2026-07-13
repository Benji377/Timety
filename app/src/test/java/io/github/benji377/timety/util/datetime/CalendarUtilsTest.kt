package io.github.benji377.timety.util.datetime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class CalendarUtilsTest {

    private fun assertCoversWholeMonth(month: LocalDate) {
        val weeks = CalendarUtils.generateWeeks(month)
        val days = weeks.flatten()
        val yearMonth = YearMonth.from(month)
        for (day in 1..yearMonth.lengthOfMonth()) {
            assertTrue(
                "missing ${yearMonth.atDay(day)}",
                days.contains(yearMonth.atDay(day))
            )
        }
        weeks.forEach { week ->
            assertEquals(7, week.size)
            assertEquals(DayOfWeek.MONDAY, week.first().dayOfWeek)
            assertEquals(DayOfWeek.SUNDAY, week.last().dayOfWeek)
        }
    }

    @Test
    fun monthEndingOnMonday_includesItsLastDay() {
        // August 2026 ends on Monday the 31st, which needs one extra week row.
        assertCoversWholeMonth(LocalDate.of(2026, 8, 1))
    }

    @Test
    fun monthEndingOnSunday_hasNoTrailingPadding() {
        // May 2026 ends on Sunday the 31st, so the grid ends exactly on the month's last day.
        val weeks = CalendarUtils.generateWeeks(LocalDate.of(2026, 5, 1))
        assertEquals(LocalDate.of(2026, 5, 31), weeks.last().last())
        assertCoversWholeMonth(LocalDate.of(2026, 5, 1))
    }

    @Test
    fun monthStartingOnMonday_hasNoLeadingPadding() {
        // June 2026 starts on Monday the 1st.
        val weeks = CalendarUtils.generateWeeks(LocalDate.of(2026, 6, 1))
        assertEquals(LocalDate.of(2026, 6, 1), weeks.first().first())
        assertCoversWholeMonth(LocalDate.of(2026, 6, 1))
    }

    @Test
    fun february_nonLeapYearStartingMidWeek_isCovered() {
        assertCoversWholeMonth(LocalDate.of(2026, 2, 1))
    }

    @Test
    fun everyMonthOfTheYear_isFullyCovered() {
        for (m in 1..12) assertCoversWholeMonth(LocalDate.of(2026, m, 1))
    }
}

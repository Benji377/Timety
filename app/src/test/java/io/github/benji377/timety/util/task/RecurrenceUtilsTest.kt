package io.github.benji377.timety.util.task

import io.github.benji377.timety.data.model.task.MonthlyMode
import io.github.benji377.timety.data.model.task.RecurrenceUnit
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class RecurrenceUtilsTest {

    private val zone = ZoneId.of("UTC")

    private fun at(y: Int, mo: Int, d: Int, h: Int = 9): Instant =
        LocalDateTime.of(y, mo, d, h, 0).atZone(zone).toInstant()

    private fun task(
        due: Instant,
        unit: RecurrenceUnit,
        interval: Int = 1,
        daysOfWeek: String? = null,
        monthlyMode: MonthlyMode = MonthlyMode.DAY_OF_MONTH,
        monthlyDay: Int? = null,
        monthlyOrdinal: Int? = null,
        monthlyWeekday: Int? = null,
    ) = RecurringTaskEntity(
        id = "t1",
        title = "Test",
        dueDate = due,
        unit = unit,
        interval = interval,
        daysOfWeek = daysOfWeek,
        monthlyMode = monthlyMode,
        monthlyDay = monthlyDay,
        monthlyOrdinal = monthlyOrdinal,
        monthlyWeekday = monthlyWeekday,
        createdAt = due,
    )

    // 2026-07-09 is a Thursday.

    @Test
    fun weekly_singleDay_advancesOneWeek() {
        val t = task(at(2026, 7, 9), RecurrenceUnit.WEEK, daysOfWeek = "[4]")
        assertEquals(at(2026, 7, 16), RecurrenceUtils.nextDueDate(t, at(2026, 7, 9, 12), zone))
    }

    @Test
    fun weekly_emptyDays_fallsBackToDueWeekday() {
        val t = task(at(2026, 7, 9), RecurrenceUnit.WEEK)
        assertEquals(at(2026, 7, 16), RecurrenceUtils.nextDueDate(t, at(2026, 7, 9, 12), zone))
    }

    @Test
    fun weekly_multiDay_picksNextSelectedDay() {
        // Due Friday Jul 10, repeating Mon+Fri: the next occurrence is Monday Jul 13,
        // even when completed early (now is still Thursday).
        val t = task(at(2026, 7, 10), RecurrenceUnit.WEEK, daysOfWeek = "[1,5]")
        assertEquals(at(2026, 7, 13), RecurrenceUtils.nextDueDate(t, at(2026, 7, 9), zone))
    }

    @Test
    fun weekly_intervalTwo_staysWeekAligned() {
        // Anchored in the week of Jul 6: the Thursday one week later is skipped.
        val t = task(at(2026, 7, 9), RecurrenceUnit.WEEK, interval = 2, daysOfWeek = "[4]")
        assertEquals(at(2026, 7, 23), RecurrenceUtils.nextDueDate(t, at(2026, 7, 9, 12), zone))
    }

    @Test
    fun weekly_overdue_skipsPastAllMissedOccurrences() {
        // Due five Thursdays ago, completed today after 09:00: lands next Thursday, not the past ones.
        val t = task(at(2026, 6, 4), RecurrenceUnit.WEEK, daysOfWeek = "[4]")
        val next = RecurrenceUtils.nextDueDate(t, at(2026, 7, 9, 12), zone)
        assertEquals(at(2026, 7, 16), next)
        assertTrue(next.isAfter(at(2026, 7, 9, 12)))
    }

    @Test
    fun monthly_dayOfMonth_futureDueStillAdvancesOnce() {
        val t = task(at(2026, 7, 20), RecurrenceUnit.MONTH, monthlyDay = 20)
        assertEquals(at(2026, 8, 20), RecurrenceUtils.nextDueDate(t, at(2026, 7, 9), zone))
    }

    @Test
    fun monthly_day31_clampsButKeepsIntent() {
        // Jan 31 -> Feb 28 (clamped) -> Mar 31 again, because the intended day is stored.
        val jan = task(at(2026, 1, 31), RecurrenceUnit.MONTH, monthlyDay = 31)
        assertEquals(at(2026, 2, 28), RecurrenceUtils.nextDueDate(jan, at(2026, 2, 1), zone))

        val feb = jan.copy(dueDate = at(2026, 2, 28))
        assertEquals(at(2026, 3, 31), RecurrenceUtils.nextDueDate(feb, at(2026, 3, 1), zone))
    }

    @Test
    fun monthly_secondFriday() {
        // Jul 10 is the second Friday of July 2026; the next one is Aug 14.
        val t = task(
            at(2026, 7, 10),
            RecurrenceUnit.MONTH,
            monthlyMode = MonthlyMode.NTH_WEEKDAY,
            monthlyOrdinal = 2,
            monthlyWeekday = 5,
        )
        assertEquals(at(2026, 8, 14), RecurrenceUtils.nextDueDate(t, at(2026, 7, 10, 12), zone))
    }

    @Test
    fun monthly_lastFriday() {
        // Jul 31 is the last Friday of July 2026; the next one is Aug 28.
        val t = task(
            at(2026, 7, 31),
            RecurrenceUnit.MONTH,
            monthlyMode = MonthlyMode.NTH_WEEKDAY,
            monthlyOrdinal = RecurringTaskEntity.LAST_ORDINAL,
            monthlyWeekday = 5,
        )
        assertEquals(at(2026, 8, 28), RecurrenceUtils.nextDueDate(t, at(2026, 8, 1), zone))
    }

    @Test
    fun monthly_intervalThree_staysMonthAligned() {
        val t = task(at(2026, 7, 3), RecurrenceUnit.MONTH, interval = 3, monthlyDay = 3)
        assertEquals(at(2026, 10, 3), RecurrenceUtils.nextDueDate(t, at(2026, 7, 4), zone))
    }

    @Test
    fun yearly_advancesOneYear() {
        val t = task(at(2026, 7, 9), RecurrenceUnit.YEAR)
        assertEquals(at(2027, 7, 9), RecurrenceUtils.nextDueDate(t, at(2026, 7, 9, 12), zone))
    }
}

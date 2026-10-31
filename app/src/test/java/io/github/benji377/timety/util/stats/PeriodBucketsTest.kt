package io.github.benji377.timety.util.stats

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class PeriodBucketsTest {
    private fun date(month: Int, day: Int, year: Int = 2026) = LocalDate.of(year, month, day)

    @Test
    fun week_isSevenSingleDays() {
        val buckets = PeriodBuckets.of(ReportPeriod.WEEK, date(10, 26))
        assertEquals(7, buckets.size)
        assertEquals(date(10, 26)..date(10, 26), buckets.first())
        assertEquals(date(11, 1)..date(11, 1), buckets.last())
    }

    @Test
    fun month_splitsIntoMondayWeeksClippedToTheMonth() {
        // October 2026 starts on a Thursday and ends on a Saturday.
        val buckets = PeriodBuckets.of(ReportPeriod.MONTH, date(10, 1))
        assertEquals(
            listOf(
                date(10, 1)..date(10, 4),
                date(10, 5)..date(10, 11),
                date(10, 12)..date(10, 18),
                date(10, 19)..date(10, 25),
                date(10, 26)..date(10, 31),
            ),
            buckets,
        )
    }

    @Test
    fun year_isTwelveWholeMonths() {
        val buckets = PeriodBuckets.of(ReportPeriod.YEAR, date(1, 1, 2028))
        assertEquals(12, buckets.size)
        assertEquals(date(2, 1, 2028)..date(2, 29, 2028), buckets[1])
        assertEquals(date(12, 1, 2028)..date(12, 31, 2028), buckets[11])
    }

    @Test
    fun sum_addsValuesToTheirBucket_andIgnoresOtherDays() {
        val buckets = PeriodBuckets.of(ReportPeriod.MONTH, date(10, 1))
        val totals = PeriodBuckets.sum(
            buckets,
            mapOf(date(10, 2) to 10, date(10, 4) to 5, date(10, 31) to 7, date(11, 1) to 99),
        )
        assertEquals(listOf(15, 0, 0, 0, 7), totals)
    }

    @Test
    fun count_countsInstantsPerBucket() {
        val buckets = PeriodBuckets.of(ReportPeriod.WEEK, date(10, 26))
        val instant = date(10, 27).atTime(9, 0).toInstant(ZoneOffset.UTC)
        assertEquals(
            listOf(0, 2, 0, 0, 0, 0, 0),
            PeriodBuckets.count(buckets, listOf(instant, instant), ZoneOffset.UTC),
        )
    }

    @Test
    fun yearPeriod_startsAndEndsOnTheCalendarYear() {
        assertEquals(date(1, 1), ReportPeriod.YEAR.startOf(date(10, 31)))
        assertEquals(date(12, 31), ReportPeriod.YEAR.endOf(date(1, 1)))
        assertEquals(date(1, 1, 2027), ReportPeriod.YEAR.nextStart(date(1, 1)))
    }

    @Test
    fun yearComparison_matchesTheCalendarDateAcrossALeapYear() {
        // Day 305 of leap 2028 would land on Nov 1 2027 if counted by elapsed days.
        val range = ReportPeriod.YEAR.comparisonRange(date(1, 1, 2028), date(10, 31, 2028))
        assertEquals(date(1, 1, 2027)..date(10, 31, 2027), range)
    }

    @Test
    fun yearComparison_mapsLeapDayToFebruaryEnd() {
        val range = ReportPeriod.YEAR.comparisonRange(date(1, 1, 2028), date(2, 29, 2028))
        assertEquals(date(1, 1, 2027)..date(2, 28, 2027), range)
    }

    @Test
    fun yearComparison_finishedYearComparesInFull() {
        val range = ReportPeriod.YEAR.comparisonRange(date(1, 1, 2025), date(10, 31, 2026))
        assertEquals(date(1, 1, 2024)..date(12, 31, 2024), range)
    }

    @Test
    fun monthComparison_keepsItsLikeForLikeRange() {
        val range = ReportPeriod.MONTH.comparisonRange(date(3, 1), date(3, 31))
        assertEquals(date(2, 1)..date(2, 28), range)
    }
}

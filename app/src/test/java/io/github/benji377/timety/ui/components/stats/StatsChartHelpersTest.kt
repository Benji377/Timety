package io.github.benji377.timety.ui.components.stats

import io.github.benji377.timety.util.stats.PeriodBuckets
import io.github.benji377.timety.util.stats.ReportPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class StatsChartHelpersTest {
    private val monday = LocalDate.of(2026, 10, 26)

    @Test
    fun dayIndexAt_picksTheNearestDayAndStaysInBounds() {
        // 1000 wide with 100 padding: seven days 133.3 apart from x = 100.
        assertEquals(0, dayIndexAt(100f, 1000, 100f, 7))
        assertEquals(1, dayIndexAt(190f, 1000, 100f, 7))
        assertEquals(6, dayIndexAt(900f, 1000, 100f, 7))
        assertEquals(0, dayIndexAt(-50f, 1000, 100f, 7))
        assertEquals(6, dayIndexAt(1000f, 1000, 100f, 7))
    }

    @Test
    fun axisLabels_followThePeriod() {
        val week = PeriodBuckets.of(ReportPeriod.WEEK, monday)
        assertEquals("Mon", bucketAxisLabel(ReportPeriod.WEEK, week.first(), Locale.ENGLISH))
        val month = PeriodBuckets.of(ReportPeriod.MONTH, LocalDate.of(2026, 10, 1))
        assertEquals("1", bucketAxisLabel(ReportPeriod.MONTH, month.first(), Locale.ENGLISH))
        assertEquals("5", bucketAxisLabel(ReportPeriod.MONTH, month[1], Locale.ENGLISH))
        val year = PeriodBuckets.of(ReportPeriod.YEAR, LocalDate.of(2026, 1, 1))
        assertEquals("Dec", bucketAxisLabel(ReportPeriod.YEAR, year.last(), Locale.ENGLISH))
    }

    @Test
    fun currentBucket_isFoundOnlyInsideThePeriod() {
        val week = PeriodBuckets.of(ReportPeriod.WEEK, monday)
        assertEquals(2, currentBucketIndex(week, monday.plusDays(2)))
        assertNull(currentBucketIndex(week, monday.minusDays(1)))
    }
}

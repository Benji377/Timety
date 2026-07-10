package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.user.DayRating
import io.github.benji377.timety.data.model.user.DayRatingEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class DayQualityCalculatorTest {

    private fun rating(dayKey: String, rating: DayRating) =
        DayRatingEntity(dayKey = dayKey, rating = rating.value, createdAt = Instant.EPOCH)

    @Test
    fun averageRating_emptyList_isNull() {
        assertNull(DayQualityCalculator.averageRating(emptyList()))
    }

    @Test
    fun averageRating_meansOverTheScale() {
        val ratings = listOf(
            rating("2026-07-01", DayRating.BAD),
            rating("2026-07-02", DayRating.GREAT),
        )
        assertEquals(2.0, DayQualityCalculator.averageRating(ratings)!!, 0.0001)
    }

    @Test
    fun buckets_bestRatingFirst_skippingUnusedRatings() {
        val ratings = listOf(
            rating("2026-07-01", DayRating.BAD),
            rating("2026-07-02", DayRating.GREAT),
        )
        val buckets = DayQualityCalculator.buckets(ratings, emptyMap(), emptyMap())
        assertEquals(listOf(DayRating.GREAT, DayRating.BAD), buckets.map { it.rating })
    }

    @Test
    fun buckets_averagesActivityPerRating_missingDaysCountAsZero() {
        val ratings = listOf(
            rating("2026-07-01", DayRating.GREAT),
            rating("2026-07-02", DayRating.GREAT),
            rating("2026-07-03", DayRating.BAD),
        )
        val focus = mapOf("2026-07-01" to 90, "2026-07-03" to 10)
        val tasks = mapOf("2026-07-01" to 4, "2026-07-02" to 2)

        val buckets = DayQualityCalculator.buckets(ratings, focus, tasks)

        val great = buckets.first { it.rating == DayRating.GREAT }
        assertEquals(2, great.dayCount)
        assertEquals(45, great.avgFocusMinutes) // (90 + 0) / 2
        assertEquals(3, great.avgTasksCompleted) // (4 + 2) / 2

        val bad = buckets.first { it.rating == DayRating.BAD }
        assertEquals(1, bad.dayCount)
        assertEquals(10, bad.avgFocusMinutes)
        assertEquals(0, bad.avgTasksCompleted)
    }

    @Test
    fun buckets_unknownRatingValues_areIgnored() {
        val ratings = listOf(
            rating("2026-07-01", DayRating.OK),
            DayRatingEntity(dayKey = "2026-07-02", rating = 99, createdAt = Instant.EPOCH),
        )
        val buckets = DayQualityCalculator.buckets(ratings, emptyMap(), emptyMap())
        assertEquals(1, buckets.size)
        assertEquals(DayRating.OK, buckets.single().rating)
    }
}

package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.user.DayRating
import io.github.benji377.timety.data.model.user.DayRatingEntity
import kotlin.math.roundToInt

/** Averages of what happened on the days rated with one [rating]. */
data class DayQualityBucket(
    val rating: DayRating,
    val dayCount: Int,
    val avgFocusMinutes: Int,
    val avgTasksCompleted: Int,
)

/** Aggregates end-of-day ratings against per-day activity. Pure and unit-testable. */
object DayQualityCalculator {

    /** Mean of all ratings on the 1..3 scale; null when nothing is rated. */
    fun averageRating(ratings: List<DayRatingEntity>): Double? {
        if (ratings.isEmpty()) return null
        return ratings.sumOf { it.rating }.toDouble() / ratings.size
    }

    /**
     * Per-rating averages of focus minutes and completed tasks across the rated days, best
     * rating first, skipping ratings no day carries. The activity maps are keyed by the same
     * `yyyy-MM-dd` day key the ratings use; days absent from a map count as zero activity.
     */
    fun buckets(
        ratings: List<DayRatingEntity>,
        focusMinutesByDay: Map<String, Int>,
        tasksCompletedByDay: Map<String, Int>,
    ): List<DayQualityBucket> {
        val byRating = ratings.groupBy { DayRating.fromValue(it.rating) }
        return DayRating.entries
            .sortedByDescending { it.value }
            .mapNotNull { rating ->
                val days = byRating[rating].orEmpty()
                if (days.isEmpty()) return@mapNotNull null
                DayQualityBucket(
                    rating = rating,
                    dayCount = days.size,
                    avgFocusMinutes = days.map { focusMinutesByDay[it.dayKey] ?: 0 }
                        .average().roundToInt(),
                    avgTasksCompleted = days.map { tasksCompletedByDay[it.dayKey] ?: 0 }
                        .average().roundToInt(),
                )
            }
    }
}

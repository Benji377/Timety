package io.github.benji377.timety.data.model.user

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/** The three-step day quality scale; [value] is what's stored and averaged. */
enum class DayRating(val value: Int) {
    BAD(1),
    OK(2),
    GREAT(3);

    companion object {
        fun fromValue(value: Int): DayRating? = entries.find { it.value == value }
    }
}

/**
 * One rating of how a day went, keyed by the day it rates (`yyyy-MM-dd`,
 * [io.github.benji377.timety.util.datetime.AppDateUtils.dayKey]). Re-rating a day replaces the row.
 */
@Entity(tableName = "day_ratings")
data class DayRatingEntity(
    @PrimaryKey
    val dayKey: String,
    /** A [DayRating.value]; stored as its Int so ratings can be averaged in queries. */
    val rating: Int,
    val createdAt: Instant,
)

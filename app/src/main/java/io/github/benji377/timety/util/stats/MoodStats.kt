package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.user.DayRating
import io.github.benji377.timety.data.model.user.DayRatingEntity
import java.time.LocalDate
import java.time.LocalTime

/** Pure helpers behind the profile's mood calendar and distribution bar. */
object MoodStats {

    /** Each rated day's mood; rows with an unreadable key or rating are skipped. */
    fun byDay(ratings: List<DayRatingEntity>): Map<LocalDate, DayRating> =
        ratings.mapNotNull { row ->
            val day = runCatching { LocalDate.parse(row.dayKey) }.getOrNull()
            val rating = DayRating.fromValue(row.rating)
            if (day != null && rating != null) day to rating else null
        }.toMap()

    /**
     * Each mood's share of [ratings] as whole percentages that always add up to 100 (largest
     * remainder), so the bar's labels never read 33 + 33 + 33. Empty when nothing is rated.
     */
    fun percentages(ratings: Collection<DayRating>): Map<DayRating, Int> {
        if (ratings.isEmpty()) return emptyMap()
        val total = ratings.size
        val counts = DayRating.entries.associateWith { r -> ratings.count { it == r } }
        val floors = counts.mapValues { (_, n) -> n * 100 / total }
        val leftover = 100 - floors.values.sum()
        // Ties go to the better mood, matching the bar's left-to-right order.
        val byRemainder = compareByDescending<DayRating> { counts.getValue(it) * 100 % total }
            .thenByDescending { it.value }
        val bonus = counts.keys
            .sortedWith(byRemainder)
            .take(leftover)
            .toSet()
        return floors.mapValues { (r, p) -> if (r in bonus) p + 1 else p }
    }

    /** Whether to ask for today's rating: from the evening checkup time on, until today is rated. */
    fun shouldPromptToday(ratedToday: Boolean, now: LocalTime, checkupTime: LocalTime): Boolean =
        !ratedToday && !now.isBefore(checkupTime)
}

package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.user.DayRating
import io.github.benji377.timety.data.model.user.DayRatingEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class MoodStatsTest {
    private fun row(key: String, value: Int) = DayRatingEntity(key, value, Instant.EPOCH)

    @Test
    fun byDay_mapsKeysToMoods_andSkipsBrokenRows() {
        val moods = MoodStats.byDay(
            listOf(row("2026-10-01", 3), row("2026-10-02", 1), row("not-a-day", 2), row("2026-10-03", 9))
        )
        assertEquals(
            mapOf(LocalDate.of(2026, 10, 1) to DayRating.GREAT, LocalDate.of(2026, 10, 2) to DayRating.BAD),
            moods,
        )
    }

    @Test
    fun percentages_emptyWhenNothingRated() {
        assertEquals(emptyMap<DayRating, Int>(), MoodStats.percentages(emptyList()))
    }

    @Test
    fun percentages_alwaysAddUpTo100() {
        val thirds = MoodStats.percentages(listOf(DayRating.BAD, DayRating.OK, DayRating.GREAT))
        assertEquals(100, thirds.values.sum())
        // The leftover point goes to the better mood on a tie.
        assertEquals(34, thirds[DayRating.GREAT])

        val sevenths = MoodStats.percentages(List(4) { DayRating.GREAT } + List(2) { DayRating.OK } + DayRating.BAD)
        assertEquals(mapOf(DayRating.GREAT to 57, DayRating.OK to 29, DayRating.BAD to 14), sevenths)
    }

    @Test
    fun percentages_unratedMoodsAreZero() {
        assertEquals(
            mapOf(DayRating.BAD to 0, DayRating.OK to 0, DayRating.GREAT to 100),
            MoodStats.percentages(listOf(DayRating.GREAT, DayRating.GREAT)),
        )
    }

    @Test
    fun shouldPromptToday_onlyFromTheCheckupTime_untilRated() {
        val checkup = LocalTime.of(20, 0)
        assertFalse(MoodStats.shouldPromptToday(false, LocalTime.of(19, 59), checkup))
        assertTrue(MoodStats.shouldPromptToday(false, LocalTime.of(20, 0), checkup))
        assertFalse(MoodStats.shouldPromptToday(true, LocalTime.of(22, 0), checkup))
    }
}

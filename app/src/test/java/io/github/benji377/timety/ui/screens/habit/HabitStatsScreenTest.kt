package io.github.benji377.timety.ui.screens.habit

import io.github.benji377.timety.util.stats.StreakCalculator.calculateBestStreak
import io.github.benji377.timety.util.stats.StreakCalculator.calculateCurrentStreak
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HabitStatsScreenTest {

    @Test
    fun testCalculateCurrentStreak() {
        val today = LocalDate.now()

        // No completions
        assertEquals(0, calculateCurrentStreak(emptyList()))

        // Completed today
        assertEquals(1, calculateCurrentStreak(listOf(today)))

        // Completed yesterday and today
        assertEquals(
            2,
            calculateCurrentStreak(listOf(today, today.minusDays(1)))
        )

        // Completed yesterday but not today (streak is maintained)
        assertEquals(
            1,
            calculateCurrentStreak(listOf(today.minusDays(1)))
        )

        // Completed 2 days ago (streak broken)
        assertEquals(
            0,
            calculateCurrentStreak(listOf(today.minusDays(2)))
        )
    }

    @Test
    fun testCalculateBestStreak() {
        val today = LocalDate.now()

        // Empty
        assertEquals(0, calculateBestStreak(emptyList()))

        // Single
        assertEquals(1, calculateBestStreak(listOf(today)))

        // Contiguous
        assertEquals(
            3, calculateBestStreak(
                listOf(
                    today,
                    today.minusDays(1),
                    today.minusDays(2)
                )
            )
        )

        // Broken streak, should return max
        assertEquals(
            3, calculateBestStreak(
                listOf(
                    today, // 1
                    today.minusDays(2), today.minusDays(3), today.minusDays(4) // 3
                )
            )
        )
    }
}

package io.github.benji377.timety.ui.screens.habit

import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

class HabitStatsScreenTest {

    private fun createHabitWithCompletions(dates: List<LocalDate>): HabitWithCompletions {
        val habit = HabitEntity(
            id = UUID.randomUUID().toString(),
            name = "Test Habit",
            frequency = HabitFrequency.DAILY,
            colorValue = 0,
            stackName = null,
            stackOrder = null,
            targetWeekdays = emptyList(),
            targetDaysPerWeek = null,
            notes = null
        )
        val completions = dates.map {
            HabitCompletionEntity(
                id = UUID.randomUUID().toString(),
                habitId = habit.id,
                completionDate = ZonedDateTime.of(it, LocalTime.NOON, ZoneId.systemDefault())
            )
        }
        return HabitWithCompletions(habit, completions)
    }

    @Test
    fun testCalculateCurrentStreak() {
        val today = LocalDate.now()
        
        // No completions
        assertEquals(0, calculateCurrentStreak(createHabitWithCompletions(emptyList())))

        // Completed today
        assertEquals(1, calculateCurrentStreak(createHabitWithCompletions(listOf(today))))

        // Completed yesterday and today
        assertEquals(2, calculateCurrentStreak(createHabitWithCompletions(listOf(today, today.minusDays(1)))))

        // Completed yesterday but not today (streak is maintained)
        assertEquals(1, calculateCurrentStreak(createHabitWithCompletions(listOf(today.minusDays(1)))))
        
        // Completed 2 days ago (streak broken)
        assertEquals(0, calculateCurrentStreak(createHabitWithCompletions(listOf(today.minusDays(2)))))
    }

    @Test
    fun testCalculateBestStreak() {
        val today = LocalDate.now()

        // Empty
        assertEquals(0, calculateBestStreak(createHabitWithCompletions(emptyList())))

        // Single
        assertEquals(1, calculateBestStreak(createHabitWithCompletions(listOf(today))))

        // Contiguous
        assertEquals(3, calculateBestStreak(createHabitWithCompletions(listOf(
            today,
            today.minusDays(1),
            today.minusDays(2)
        ))))

        // Broken streak, should return max
        assertEquals(3, calculateBestStreak(createHabitWithCompletions(listOf(
            today, // 1
            today.minusDays(2), today.minusDays(3), today.minusDays(4) // 3
        ))))
    }
}

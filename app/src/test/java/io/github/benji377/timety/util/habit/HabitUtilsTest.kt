package io.github.benji377.timety.util.habit

import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HabitUtilsTest {

    private fun habit(id: String = "h1") = HabitEntity(
        id = id,
        name = "Test Habit",
        frequency = HabitFrequency.DAILY,
        createdAt = Instant.EPOCH,
        colorValue = 0,
    )

    private fun completedOn(date: LocalDate) = HabitCompletionEntity(
        habitId = "h1",
        completionDate = date.atStartOfDay(ZoneId.systemDefault()).plusHours(12).toInstant(),
    )

    @Test
    fun testParseWeekdays() {
        assertEquals(emptySet<Int>(), HabitUtils.parseWeekdays(null))
        assertEquals(emptySet<Int>(), HabitUtils.parseWeekdays(""))
        assertEquals(emptySet<Int>(), HabitUtils.parseWeekdays("  "))
        assertEquals(setOf(1, 3, 5), HabitUtils.parseWeekdays("[1,3,5]"))
        assertEquals(setOf(1, 3), HabitUtils.parseWeekdays("[1, 3]"))
        // Non-numeric entries are skipped rather than crashing.
        assertEquals(setOf(2), HabitUtils.parseWeekdays("[2,x]"))
    }

    @Test
    fun testSerializeWeekdaysSortsAndRoundtrips() {
        assertEquals("[1,3,5]", HabitUtils.serializeWeekdays(setOf(5, 1, 3)))
        assertEquals("[]", HabitUtils.serializeWeekdays(emptySet()))
        val days = setOf(2, 4, 7)
        assertEquals(days, HabitUtils.parseWeekdays(HabitUtils.serializeWeekdays(days)))
    }

    @Test
    fun testIsHabitLocked() {
        // First habit of a stack is never locked.
        assertFalse(HabitUtils.isHabitLocked(0, isCurrentHabitDone = false, isPreviousHabitDone = false))
        // Locked only when neither the current nor the previous habit is done.
        assertTrue(HabitUtils.isHabitLocked(1, isCurrentHabitDone = false, isPreviousHabitDone = false))
        assertFalse(HabitUtils.isHabitLocked(1, isCurrentHabitDone = false, isPreviousHabitDone = true))
        assertFalse(HabitUtils.isHabitLocked(1, isCurrentHabitDone = true, isPreviousHabitDone = false))
    }

    @Test
    fun testIsCompletedOn() {
        val today = LocalDate.now()
        val hwc = HabitWithCompletions(habit(), listOf(completedOn(today)))
        assertTrue(HabitUtils.isCompletedOn(hwc, today))
        assertFalse(HabitUtils.isCompletedOn(hwc, today.minusDays(1)))
        assertFalse(HabitUtils.isCompletedOn(HabitWithCompletions(habit(), emptyList()), today))
    }

    @Test
    fun testStackCompletion() {
        val today = LocalDate.now()
        val done = HabitWithCompletions(habit("a"), listOf(completedOn(today)))
        val notDone = HabitWithCompletions(habit("b"), emptyList())

        assertFalse(HabitUtils.isStackFullyCompleted(emptyList(), today))
        assertFalse(HabitUtils.isStackFullyCompleted(listOf(done, notDone), today))
        assertTrue(HabitUtils.isStackFullyCompleted(listOf(done, done), today))
        assertEquals(1, HabitUtils.getStackCompletionCount(listOf(done, notDone), today))
    }

    @Test
    fun testReminderTimesRoundTrip() {
        assertEquals(emptyList<Int>(), HabitUtils.parseReminderTimes(null))
        assertEquals(emptyList<Int>(), HabitUtils.parseReminderTimes("[]"))
        assertEquals(listOf(480, 1200), HabitUtils.parseReminderTimes("[1200, 480, 480]"))
        assertEquals(listOf(480), HabitUtils.parseReminderTimes("[480,1440,-5]"))
        assertEquals(
            HabitUtils.MAX_REMINDER_TIMES,
            HabitUtils.parseReminderTimes("[1,2,3,4,5,6,7]").size,
        )
        assertEquals(null, HabitUtils.serializeReminderTimes(emptyList()))
        assertEquals("[480,1200]", HabitUtils.serializeReminderTimes(listOf(1200, 480)))
    }

    @Test
    fun testNeedsReminderToday() {
        val today = LocalDate.now()
        val open = HabitWithCompletions(habit(), emptyList())
        val done = HabitWithCompletions(habit(), listOf(completedOn(today)))
        assertTrue(HabitUtils.needsReminderToday(open))
        assertFalse(HabitUtils.needsReminderToday(done))
    }

    @Test
    fun testNeedsReminderTodayForWeeklyHabits() {
        val today = LocalDate.now()
        val otherDay = (1..7).first { it != today.dayOfWeek.value }
        val exactElsewhere = habit().copy(
            frequency = HabitFrequency.WEEKLY_EXACT,
            targetWeekdays = HabitUtils.serializeWeekdays(setOf(otherDay)),
        )
        assertFalse(HabitUtils.needsReminderToday(HabitWithCompletions(exactElsewhere, emptyList())))

        val exactToday = exactElsewhere.copy(
            targetWeekdays = HabitUtils.serializeWeekdays(setOf(today.dayOfWeek.value)),
        )
        assertTrue(HabitUtils.needsReminderToday(HabitWithCompletions(exactToday, emptyList())))

        // Target met on earlier days this week: the habit drops off the list and stops nagging.
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val flexible = habit().copy(frequency = HabitFrequency.WEEKLY_FLEXIBLE, targetDaysPerWeek = 1)
        if (monday != today) {
            val met = HabitWithCompletions(flexible, listOf(completedOn(monday)))
            assertFalse(HabitUtils.needsReminderToday(met))
        }
        assertTrue(HabitUtils.needsReminderToday(HabitWithCompletions(flexible, emptyList())))
    }
}

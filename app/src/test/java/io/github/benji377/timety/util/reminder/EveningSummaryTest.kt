package io.github.benji377.timety.util.reminder

import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.data.model.task.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class EveningSummaryTest {

    private val now = Instant.parse("2026-10-06T18:00:00Z")

    private fun task(title: String, due: Instant?, done: Boolean = false) = TaskEntity(
        id = title,
        title = title,
        dueDate = due,
        isCompleted = done,
        createdAt = Instant.EPOCH,
    )

    private fun habit(name: String, done: Boolean = false) = HabitWithCompletions(
        HabitEntity(
            id = name,
            name = name,
            frequency = HabitFrequency.DAILY,
            createdAt = Instant.EPOCH,
            colorValue = 0,
        ),
        if (done) listOf(HabitCompletionEntity(habitId = name, completionDate = Instant.now())) else emptyList(),
    )

    @Test
    fun allDoneWhenNothingIsOpen() {
        val summary = EveningSummaryBuilder.build(
            habits = listOf(habit("Read", done = true)),
            tasks = listOf(
                task("Done", now.minus(1, ChronoUnit.DAYS), done = true),
                task("Later", now.plus(1, ChronoUnit.DAYS)),
                task("Undated", null),
            ),
            recurringTasks = emptyList(),
            now = now,
        )
        assertTrue(summary.isAllDone)
    }

    @Test
    fun listsOverdueTasksMostOverdueFirstThenOpenHabits() {
        val summary = EveningSummaryBuilder.build(
            habits = listOf(habit("Read"), habit("Run", done = true)),
            tasks = listOf(
                task("Recent", now.minus(1, ChronoUnit.HOURS)),
                task("Old", now.minus(3, ChronoUnit.DAYS)),
            ),
            recurringTasks = emptyList(),
            now = now,
        )
        assertEquals(listOf("Old", "Recent"), summary.taskTitles)
        assertEquals(listOf("Read"), summary.habitNames)
        assertEquals(3, summary.total)
    }

    @Test
    fun previewKeepsTasksFirstAndCountsHiddenItems() {
        val summary = EveningSummary(
            taskTitles = listOf("t1", "t2", "t3"),
            habitNames = listOf("h1", "h2", "h3"),
        )
        val preview = summary.preview(5)
        assertEquals(listOf("t1", "t2", "t3", "h1", "h2"), preview.items.map { it.name })
        assertTrue(preview.items.take(3).all { it.isTask })
        assertFalse(preview.items.last().isTask)
        assertEquals(1, preview.hiddenCount)
        assertEquals(0, summary.preview(10).hiddenCount)
    }
}

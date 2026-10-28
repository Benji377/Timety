package io.github.benji377.timety.util.goal

import io.github.benji377.timety.data.model.goal.GoalEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class GoalUtilsTest {

    private val start = Instant.parse("2026-01-01T00:00:00Z")

    private fun goal(target: Int = 100, lengthDays: Long = 10, createdAt: Instant = start) = GoalEntity(
        id = "g",
        name = "Read",
        colorValue = 0,
        targetValue = target,
        unitLabel = "pages",
        targetDate = createdAt.plus(Duration.ofDays(lengthDays)),
        createdAt = createdAt,
    )

    @Test
    fun expectedProgress_scalesWithElapsedTime_andClamps() {
        val g = goal()
        assertEquals(0, GoalUtils.expectedProgress(g, start.minusSeconds(60)))
        assertEquals(30, GoalUtils.expectedProgress(g, start.plus(Duration.ofDays(3))))
        assertEquals(100, GoalUtils.expectedProgress(g, start.plus(Duration.ofDays(20))))
    }

    @Test
    fun expectedProgress_deadlineNotAfterStart_isFullTarget() {
        assertEquals(100, GoalUtils.expectedProgress(goal(lengthDays = 0), start))
    }

    @Test
    fun expectedFraction_isProgressOverTarget_andFullForEmptyTarget() {
        assertEquals(0.5f, GoalUtils.expectedFraction(goal(), start.plus(Duration.ofDays(5))), 0f)
        assertEquals(1f, GoalUtils.expectedFraction(goal(target = 0), start), 0f)
    }

    @Test
    fun daysLeft_countsToTheDeadlineDay_negativeWhenOverdue() {
        val today = LocalDate.of(2026, 3, 10)
        val createdAt = today.atStartOfDay(ZoneId.systemDefault()).plusHours(12).toInstant()
        val g = goal(lengthDays = 5, createdAt = createdAt)
        assertEquals(5, GoalUtils.daysLeft(g, today))
        assertEquals(0, GoalUtils.daysLeft(g, today.plusDays(5)))
        assertEquals(-2, GoalUtils.daysLeft(g, today.plusDays(7)))
    }
}

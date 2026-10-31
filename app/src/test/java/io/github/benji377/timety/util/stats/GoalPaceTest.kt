package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.goal.GoalEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class GoalPaceTest {
    private val zone = ZoneOffset.UTC
    private val created = LocalDate.of(2026, 1, 1)
    private val deadline = created.plusDays(100)

    private fun goal(completedOn: LocalDate? = null, target: Int = 100) = GoalEntity(
        id = "g",
        name = "g",
        colorValue = 0,
        targetValue = target,
        unitLabel = "km",
        targetDate = deadline.atStartOfDay(zone).toInstant(),
        createdAt = created.atStartOfDay(zone).toInstant(),
        completedAt = completedOn?.atStartOfDay(zone)?.toInstant(),
    )

    private fun pace(progress: Int, elapsedDays: Long, target: Int = 100) =
        GoalPace.of(goal(target = target), progress, created.plusDays(elapsedDays), zone)

    @Test
    fun onPace_withinTheTolerance() {
        assertEquals(GoalPaceStatus.ON_PACE, pace(50, 50).status)
        assertEquals(GoalPaceStatus.ON_PACE, pace(45, 50).status)
        assertEquals(GoalPaceStatus.ON_PACE, pace(55, 50).status)
    }

    @Test
    fun aheadAndBehind_outsideTheTolerance() {
        assertEquals(GoalPaceStatus.AHEAD, pace(56, 50).status)
        assertEquals(GoalPaceStatus.BEHIND, pace(44, 50).status)
    }

    @Test
    fun fractions_reportExpectedAndActualPosition() {
        val result = pace(25, 50)
        assertEquals(0.5f, result.expectedFraction, 0.001f)
        assertEquals(0.25f, result.progressFraction, 0.001f)
    }

    @Test
    fun zeroProgress_onDayOne_isOnPace_withNoProjection() {
        val result = pace(0, 0)
        assertEquals(GoalPaceStatus.ON_PACE, result.status)
        assertNull(result.projectedFinish)
    }

    @Test
    fun zeroProgress_later_isBehind_withNoProjection() {
        val result = pace(0, 50)
        assertEquals(GoalPaceStatus.BEHIND, result.status)
        assertNull(result.projectedFinish)
    }

    @Test
    fun projection_continuesThePaceSoFar() {
        // 25 in 50 days reaches 100 after 200 days.
        assertEquals(created.plusDays(200), pace(25, 50).projectedFinish)
    }

    @Test
    fun overdue_pastTheDeadlineWithTargetMissed() {
        val result = pace(50, 101)
        assertEquals(GoalPaceStatus.OVERDUE, result.status)
        assertEquals(1f, result.expectedFraction, 0.001f)
    }

    @Test
    fun deadlineDay_isNotOverdueYet() {
        assertEquals(GoalPaceStatus.BEHIND, pace(50, 100).status)
    }

    @Test
    fun targetReached_isNeverOverdue_andHasNoProjection() {
        val result = pace(120, 200)
        assertEquals(GoalPaceStatus.ON_PACE, result.status)
        assertEquals(1f, result.progressFraction, 0.001f)
        assertNull(result.projectedFinish)
    }

    @Test
    fun sameDayDeadline_doesNotDivideByZero() {
        val sameDay = goal().copy(targetDate = created.atStartOfDay(zone).toInstant())
        assertEquals(GoalPaceStatus.ON_PACE, GoalPace.of(sameDay, 0, created, zone).status)
    }

    @Test
    fun daysEarly_isPositiveEarly_negativeLate_nullWhileActive() {
        assertEquals(10L, GoalPace.daysEarly(goal(completedOn = deadline.minusDays(10)), zone))
        assertEquals(-3L, GoalPace.daysEarly(goal(completedOn = deadline.plusDays(3)), zone))
        assertNull(GoalPace.daysEarly(goal(), zone))
    }
}

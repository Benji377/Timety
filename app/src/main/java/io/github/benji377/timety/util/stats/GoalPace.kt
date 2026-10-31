package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.goal.GoalEntity
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.ceil

/** Where an active goal stands against a steady pace towards its deadline. */
enum class GoalPaceStatus {
    AHEAD,
    ON_PACE,
    BEHIND,
    OVERDUE,
}

/**
 * An active goal's pace. [expectedFraction] is where a steady pace would be today, [progressFraction]
 * where the goal is, both 0 to 1. [projectedFinish] continues the pace so far; it is null without
 * any progress, or once the target is reached.
 */
data class GoalPace(
    val status: GoalPaceStatus,
    val expectedFraction: Float,
    val progressFraction: Float,
    val projectedFinish: LocalDate?,
) {
    companion object {
        // Within this share of the target counts as on pace, so one entry doesn't flip the status.
        private const val ON_PACE_TOLERANCE = 0.05

        // Keeps a value exactly on the tolerance from falling outside it through rounding.
        private const val ROUNDING_MARGIN = 1e-9

        /** The pace of [goal] with [progress] logged so far, as of [today]. */
        fun of(goal: GoalEntity, progress: Int, today: LocalDate, zone: ZoneId): GoalPace {
            val createdOn = goal.createdAt.atZone(zone).toLocalDate()
            val deadline = goal.targetDate.atZone(zone).toLocalDate()
            val totalDays = ChronoUnit.DAYS.between(createdOn, deadline).coerceAtLeast(1)
            val elapsedDays = ChronoUnit.DAYS.between(createdOn, today).coerceAtLeast(0)
            val target = goal.targetValue.coerceAtLeast(1).toDouble()

            val expected = (elapsedDays.toDouble() / totalDays).coerceIn(0.0, 1.0)
            val actual = (progress / target).coerceIn(0.0, 1.0)
            val status = when {
                today.isAfter(deadline) && actual < 1.0 -> GoalPaceStatus.OVERDUE
                abs(actual - expected) <= ON_PACE_TOLERANCE + ROUNDING_MARGIN -> GoalPaceStatus.ON_PACE
                actual > expected -> GoalPaceStatus.AHEAD
                else -> GoalPaceStatus.BEHIND
            }
            val projected = if (progress > 0 && actual < 1.0) {
                val daysNeeded = ceil(elapsedDays.coerceAtLeast(1) * target / progress).toLong()
                createdOn.plusDays(daysNeeded)
            } else {
                null
            }
            return GoalPace(status, expected.toFloat(), actual.toFloat(), projected)
        }

        /** Days a finished goal beat its deadline by; negative when late, null while it is active. */
        fun daysEarly(goal: GoalEntity, zone: ZoneId): Long? = goal.completedAt?.let { completedAt ->
            ChronoUnit.DAYS.between(
                completedAt.atZone(zone).toLocalDate(),
                goal.targetDate.atZone(zone).toLocalDate(),
            )
        }
    }
}

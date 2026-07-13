package io.github.benji377.timety.util.goal

import io.github.benji377.timety.data.model.goal.GoalEntity
import io.github.benji377.timety.util.goal.GoalUtils.expectedProgress
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** Pacing math for goals: where progress *should* be by now on the createdAt → targetDate line. */
object GoalUtils {

    /**
     * The progress a perfectly-paced user would have logged by [now]: the target scaled by the
     * elapsed fraction of the goal's lifetime. Clamped so pre-start reads 0 and overdue reads the
     * full target.
     */
    fun expectedProgress(goal: GoalEntity, now: Instant = Instant.now()): Int {
        val total = goal.targetDate.toEpochMilli() - goal.createdAt.toEpochMilli()
        if (total <= 0) return goal.targetValue
        val elapsed = now.toEpochMilli() - goal.createdAt.toEpochMilli()
        val fraction = (elapsed.toDouble() / total).coerceIn(0.0, 1.0)
        return (goal.targetValue * fraction).roundToInt()
    }

    /** [expectedProgress] as a 0..1 fraction, for positioning the pace tick on a progress bar. */
    fun expectedFraction(goal: GoalEntity, now: Instant = Instant.now()): Float {
        if (goal.targetValue <= 0) return 1f
        return expectedProgress(goal, now).toFloat() / goal.targetValue
    }

    /** Whole days from [today] until the deadline; zero on the deadline day, negative once overdue. */
    fun daysLeft(goal: GoalEntity, today: LocalDate = LocalDate.now()): Long {
        val deadline = goal.targetDate.atZone(ZoneId.systemDefault()).toLocalDate()
        return ChronoUnit.DAYS.between(today, deadline)
    }
}

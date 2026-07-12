package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.focus.FocusTargetType
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.task.TaskSize
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Actual focus time observed for tasks declared as one [size], across [sampleCount] tasks. */
data class CalibrationBucket(
    val size: TaskSize,
    val avgMinutes: Int,
    val sampleCount: Int,
    val stdDevMinutes: Int,
)

/** A finding worth surfacing in the calibration card's caption. */
sealed interface CalibrationInsight {
    /** [largerSize]'s average focus time is below [smallerSize]'s, inverting the expected order. */
    data class OrderingViolation(val largerSize: TaskSize, val smallerSize: TaskSize) :
        CalibrationInsight

    /** Actual focus time for [size] varies too widely for its average to mean much. */
    data class HighSpread(val size: TaskSize) : CalibrationInsight
}

/**
 * Aggregates completed tasks against the focus sessions that targeted them, to see whether size
 * estimates (S/M/L/XL) track actual focus time. Pure and unit-testable.
 */
object EstimationCalibrator {

    // Below this, a bucket's spread isn't flagged even if proportionally large - too few
    // samples for variance to mean anything.
    private const val MIN_SAMPLES_FOR_SPREAD = 3

    // stdDev / mean beyond this ratio counts as "huge" spread.
    private const val SPREAD_RATIO_THRESHOLD = 0.75

    /**
     * One bucket per [TaskSize] that has at least one completed task with logged focus time,
     * in size order (S, M, L, XL). Sizes with no such tasks are omitted rather than shown as
     * zero, since zero would misread as "instant tasks" rather than "no data".
     */
    fun buckets(tasks: List<TaskEntity>, sessions: List<FocusSessionEntity>): List<CalibrationBucket> {
        val secondsByTaskId = sessions
            .filter { it.targetType == FocusTargetType.TASK && it.targetId != null }
            .groupBy { it.targetId!! }
            .mapValues { (_, taskSessions) -> taskSessions.sumOf { it.totalSecondsFocused } }

        val tasksById = tasks.associateBy { it.id }
        val minutesBySize = mutableMapOf<TaskSize, MutableList<Int>>()
        secondsByTaskId.forEach { (taskId, seconds) ->
            val task = tasksById[taskId] ?: return@forEach
            minutesBySize.getOrPut(task.size) { mutableListOf() }.add(seconds / 60)
        }

        return TaskSize.entries.mapNotNull { size ->
            val minutes = minutesBySize[size]
            if (minutes.isNullOrEmpty()) return@mapNotNull null
            val mean = minutes.average()
            val stdDev = if (minutes.size < 2) {
                0.0
            } else {
                sqrt(minutes.sumOf { (it - mean) * (it - mean) } / (minutes.size - 1))
            }
            CalibrationBucket(
                size = size,
                avgMinutes = mean.roundToInt(),
                sampleCount = minutes.size,
                stdDevMinutes = stdDev.roundToInt(),
            )
        }
    }

    /**
     * The single most relevant finding for [buckets], or null when the sizes track actual time
     * as expected. Ordering violations take priority over spread - a violation says the
     * estimates are actively misleading, while spread just says a bucket's average is noisy.
     */
    fun insight(buckets: List<CalibrationBucket>): CalibrationInsight? {
        orderingViolation(buckets)?.let { return it }
        val spread = buckets.firstOrNull {
            it.sampleCount >= MIN_SAMPLES_FOR_SPREAD &&
                it.avgMinutes > 0 &&
                it.stdDevMinutes.toDouble() / it.avgMinutes > SPREAD_RATIO_THRESHOLD
        }
        return spread?.let { CalibrationInsight.HighSpread(it.size) }
    }

    /**
     * Walks [buckets] in size order tracking the highest average seen so far; the first bucket
     * whose average dips below that running maximum violates S < M < L < XL, reported against
     * the specific smaller bucket that set the maximum (the clearest possible comparison).
     */
    private fun orderingViolation(buckets: List<CalibrationBucket>): CalibrationInsight.OrderingViolation? {
        val sorted = buckets.sortedBy { it.size.value }
        var runningMax: CalibrationBucket? = null
        for (bucket in sorted) {
            val max = runningMax
            if (max != null && bucket.avgMinutes < max.avgMinutes) {
                return CalibrationInsight.OrderingViolation(
                    largerSize = bucket.size,
                    smallerSize = max.size,
                )
            }
            if (max == null || bucket.avgMinutes > max.avgMinutes) runningMax = bucket
        }
        return null
    }
}

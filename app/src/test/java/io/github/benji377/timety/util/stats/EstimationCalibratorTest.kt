package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.focus.FocusTargetType
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.task.TaskSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class EstimationCalibratorTest {

    private fun task(id: String, size: TaskSize): TaskEntity =
        TaskEntity(id = id, title = id, size = size, isCompleted = true, createdAt = Instant.now())

    private fun session(taskId: String, minutes: Int): FocusSessionEntity =
        FocusSessionEntity(
            id = "session-$taskId-${minutes}m-${System.nanoTime()}",
            modeId = "mode",
            startTime = Instant.now(),
            totalSecondsFocused = minutes * 60,
            targetType = FocusTargetType.TASK,
            targetId = taskId,
        )

    // buckets()

    @Test
    fun noSessions_isEmpty() {
        val tasks = listOf(task("t1", TaskSize.SMALL))
        assertTrue(EstimationCalibrator.buckets(tasks, emptyList()).isEmpty())
    }

    @Test
    fun ignoresNonTaskTargets() {
        val tasks = listOf(task("t1", TaskSize.SMALL))
        val sessions = listOf(
            session("t1", 10).copy(targetType = FocusTargetType.TAG, targetId = "tag1"),
            session("t1", 10).copy(targetType = FocusTargetType.HABIT, targetId = "habit1"),
        )
        assertTrue(EstimationCalibrator.buckets(tasks, sessions).isEmpty())
    }

    @Test
    fun sumsMultipleSessionsPerTask() {
        val tasks = listOf(task("t1", TaskSize.MEDIUM))
        val sessions = listOf(session("t1", 10), session("t1", 15))
        val buckets = EstimationCalibrator.buckets(tasks, sessions)
        assertEquals(1, buckets.size)
        assertEquals(25, buckets[0].avgMinutes)
        assertEquals(1, buckets[0].sampleCount)
    }

    @Test
    fun averagesAcrossTasksOfTheSameSize() {
        val tasks = listOf(task("t1", TaskSize.LARGE), task("t2", TaskSize.LARGE))
        val sessions = listOf(session("t1", 20), session("t2", 40))
        val buckets = EstimationCalibrator.buckets(tasks, sessions)
        assertEquals(1, buckets.size)
        assertEquals(TaskSize.LARGE, buckets[0].size)
        assertEquals(30, buckets[0].avgMinutes)
        assertEquals(2, buckets[0].sampleCount)
    }

    @Test
    fun sizesWithNoData_areOmittedNotZero() {
        val tasks = listOf(task("t1", TaskSize.SMALL), task("t2", TaskSize.LARGE))
        val sessions = listOf(session("t1", 10))
        val buckets = EstimationCalibrator.buckets(tasks, sessions)
        assertEquals(listOf(TaskSize.SMALL), buckets.map { it.size })
    }

    @Test
    fun bucketsAreOrderedSmallToLarge() {
        val tasks = listOf(
            task("t1", TaskSize.VERY_LARGE),
            task("t2", TaskSize.SMALL),
            task("t3", TaskSize.MEDIUM),
        )
        val sessions = listOf(session("t1", 60), session("t2", 5), session("t3", 20))
        val buckets = EstimationCalibrator.buckets(tasks, sessions)
        assertEquals(
            listOf(TaskSize.SMALL, TaskSize.MEDIUM, TaskSize.VERY_LARGE),
            buckets.map { it.size }
        )
    }

    @Test
    fun taskWithNoMatchingEntity_isSkipped() {
        // Session targets a task id that isn't in the completed-tasks list (e.g. still open).
        val sessions = listOf(session("ghost", 30))
        assertTrue(EstimationCalibrator.buckets(emptyList(), sessions).isEmpty())
    }

    // insight() - ordering violations

    @Test
    fun noViolation_whenSizesTrackTime() {
        val buckets = listOf(
            CalibrationBucket(TaskSize.SMALL, avgMinutes = 10, sampleCount = 3, stdDevMinutes = 1),
            CalibrationBucket(TaskSize.MEDIUM, avgMinutes = 20, sampleCount = 3, stdDevMinutes = 1),
            CalibrationBucket(TaskSize.LARGE, avgMinutes = 40, sampleCount = 3, stdDevMinutes = 1),
        )
        assertNull(EstimationCalibrator.insight(buckets))
    }

    @Test
    fun violation_whenLargerBucketAveragesLess() {
        val buckets = listOf(
            CalibrationBucket(TaskSize.MEDIUM, avgMinutes = 40, sampleCount = 3, stdDevMinutes = 1),
            CalibrationBucket(TaskSize.LARGE, avgMinutes = 25, sampleCount = 3, stdDevMinutes = 1),
        )
        val insight = EstimationCalibrator.insight(buckets)
        assertTrue(insight is CalibrationInsight.OrderingViolation)
        val violation = insight as CalibrationInsight.OrderingViolation
        assertEquals(TaskSize.LARGE, violation.largerSize)
        assertEquals(TaskSize.MEDIUM, violation.smallerSize)
    }

    @Test
    fun violation_reportsAgainstHighestSmallerBucket() {
        // LARGE (25) dips below both SMALL (10) and MEDIUM (40) - MEDIUM is the clearer comparison.
        val buckets = listOf(
            CalibrationBucket(TaskSize.SMALL, avgMinutes = 10, sampleCount = 3, stdDevMinutes = 1),
            CalibrationBucket(TaskSize.MEDIUM, avgMinutes = 40, sampleCount = 3, stdDevMinutes = 1),
            CalibrationBucket(TaskSize.LARGE, avgMinutes = 25, sampleCount = 3, stdDevMinutes = 1),
        )
        val violation = EstimationCalibrator.insight(buckets) as CalibrationInsight.OrderingViolation
        assertEquals(TaskSize.MEDIUM, violation.smallerSize)
    }

    @Test
    fun singleBucket_noViolationPossible() {
        val buckets =
            listOf(CalibrationBucket(TaskSize.SMALL, avgMinutes = 10, sampleCount = 3, stdDevMinutes = 1))
        assertNull(EstimationCalibrator.insight(buckets))
    }

    // insight() - high spread

    @Test
    fun highSpread_flaggedWhenNoOrderingViolation() {
        val buckets = listOf(
            CalibrationBucket(TaskSize.MEDIUM, avgMinutes = 20, sampleCount = 4, stdDevMinutes = 18),
        )
        val insight = EstimationCalibrator.insight(buckets)
        assertTrue(insight is CalibrationInsight.HighSpread)
        assertEquals(TaskSize.MEDIUM, (insight as CalibrationInsight.HighSpread).size)
    }

    @Test
    fun highSpread_ignoredBelowSampleThreshold() {
        val buckets = listOf(
            CalibrationBucket(TaskSize.MEDIUM, avgMinutes = 20, sampleCount = 2, stdDevMinutes = 18),
        )
        assertNull(EstimationCalibrator.insight(buckets))
    }

    @Test
    fun lowSpread_notFlagged() {
        val buckets = listOf(
            CalibrationBucket(TaskSize.MEDIUM, avgMinutes = 40, sampleCount = 5, stdDevMinutes = 5),
        )
        assertNull(EstimationCalibrator.insight(buckets))
    }

    @Test
    fun orderingViolationTakesPriorityOverSpread() {
        val buckets = listOf(
            CalibrationBucket(TaskSize.MEDIUM, avgMinutes = 40, sampleCount = 3, stdDevMinutes = 1),
            CalibrationBucket(TaskSize.LARGE, avgMinutes = 25, sampleCount = 5, stdDevMinutes = 22),
        )
        assertTrue(EstimationCalibrator.insight(buckets) is CalibrationInsight.OrderingViolation)
    }
}

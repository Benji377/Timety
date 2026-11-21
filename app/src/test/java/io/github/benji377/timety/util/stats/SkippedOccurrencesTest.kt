package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.task.OccurrenceStatus
import io.github.benji377.timety.data.model.task.RecurrenceUnit
import io.github.benji377.timety.data.model.task.RecurringOccurrenceEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.RecurringTaskWithOccurrences
import io.github.benji377.timety.util.calendar.CalendarDayItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** A skipped round moves the schedule on but is never counted as work done. */
class SkippedOccurrencesTest {
    private val zone: ZoneId = ZoneOffset.UTC
    private val day = LocalDate.of(2026, 11, 14)
    private fun at(hour: Int) = day.atTime(hour, 0).toInstant(ZoneOffset.UTC)

    private val task = RecurringTaskEntity(
        id = "r1",
        title = "Bins",
        dueDate = at(9).plusSeconds(7 * 86_400L),
        unit = RecurrenceUnit.WEEK,
        createdAt = at(0).minusSeconds(30 * 86_400L),
    )
    private val done = RecurringOccurrenceEntity(
        id = 1, recurringTaskId = "r1", completedAt = at(10), previousDueDate = at(9),
    )
    private val skipped = RecurringOccurrenceEntity(
        id = 2, recurringTaskId = "r1", completedAt = at(12), previousDueDate = at(9),
        status = OccurrenceStatus.SKIPPED,
    )
    private val item = RecurringTaskWithOccurrences(task, listOf(done, skipped))

    @Test
    fun completed_leavesSkipsOut_whileTheLogKeepsThem() {
        assertEquals(listOf(done), item.completed)
        assertEquals(2, item.log.size)
    }

    @Test
    fun completionTimes_countOnlyTheCompletion() {
        assertEquals(listOf(done.completedAt), TaskTiming.completions(emptyList(), listOf(item)))
    }

    @Test
    fun theOnTimeRate_ignoresTheSkip() {
        val rate = TaskTiming.dueDateRate(emptyList(), listOf(item), day..day, zone)

        assertEquals(1, rate.total)
    }

    @Test
    fun aPeriodSummary_countsOneTaskCompleted() {
        val summary = PeriodSummary.of(day..day, emptyList(), listOf(item), emptyList(), emptyList(), zone)

        assertEquals(1, summary.tasksCompleted)
    }

    @Test
    fun theCalendarDay_showsOnlyTheCompletionAsDone() {
        val items = CalendarDayItems.of(day, emptyList(), listOf(item), emptyList(), emptyList(), zone)

        assertEquals(listOf(done), items.recurringDone.map { it.second })
    }

    @Test
    fun aTaskWithOnlySkips_hasNothingCompleted() {
        assertTrue(RecurringTaskWithOccurrences(task, listOf(skipped)).completed.isEmpty())
    }
}

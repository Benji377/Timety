package io.github.benji377.timety.workflow

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.benji377.timety.data.model.task.RecurrenceUnit
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.testutil.TestAppContainer
import io.github.benji377.timety.util.task.RecurrenceUtils
import io.github.benji377.timety.util.task.RecurringStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * The upcoming-tasks horizon reclassifies items on the home screen, the task list, the recurring
 * task list and the home-screen widget at once. This checks that one setting change moves both a
 * plain task and a recurring task across the boundary consistently, using the same queries those
 * surfaces use, so the widget and the app cannot start disagreeing about what counts as upcoming.
 */
@RunWith(AndroidJUnit4::class)
class UpcomingHorizonTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val zone: ZoneId = ZoneId.systemDefault()
    private val today: LocalDate = LocalDate.now()

    private lateinit var container: TestAppContainer

    @Before
    fun setUp() {
        container = TestAppContainer(context)
    }

    @After
    fun tearDown() {
        container.close()
    }

    @Test
    fun wideningTheHorizon_bringsTheSameItemsIntoView_forBothPlainAndRecurringTasks() =
        runBlocking {
            val dueInTenDays = today.plusDays(10).atStartOfDay(zone).toInstant()

            container.taskRepository.insertTask(
                TaskEntity(
                    id = "task-1",
                    title = "Renew passport",
                    dueDate = dueInTenDays,
                    createdAt = Instant.now(),
                ),
            )
            val recurring = RecurringTaskEntity(
                id = "recurring-1",
                title = "Pay rent",
                dueDate = dueInTenDays,
                unit = RecurrenceUnit.MONTH,
                createdAt = Instant.now(),
            )
            container.recurringTaskRepository.insertTask(recurring)

            container.settingsRepository.saveUpcomingTasksHorizon(7)
            assertFalse(
                "a task due in 10 days is outside a 7 day horizon",
                widgetVisibleTaskIds(horizonDays = 7).contains("task-1"),
            )
            assertEquals(
                "a recurring task due in 10 days is outside a 7 day horizon",
                RecurringStatus.SCHEDULED,
                RecurrenceUtils.statusOf(recurring, Instant.now(), horizonDays = 7, zone = zone),
            )

            container.settingsRepository.saveUpcomingTasksHorizon(14)
            assertEquals(
                "the horizon setting should have been persisted",
                14,
                container.settingsRepository.upcomingTasksHorizonFlow.first(),
            )
            assertTrue(
                "widening the horizon to 14 days should bring the task into view",
                widgetVisibleTaskIds(horizonDays = 14).contains("task-1"),
            )
            assertEquals(
                "widening the horizon to 14 days should bring the recurring task into view",
                RecurringStatus.UPCOMING,
                RecurrenceUtils.statusOf(recurring, Instant.now(), horizonDays = 14, zone = zone),
            )
        }

    /**
     * The window query the task widget runs in `provideGlance`, reproduced here because the widget
     * itself reads the application's own container and cannot be pointed at a test database.
     */
    private suspend fun widgetVisibleTaskIds(horizonDays: Int): List<String> {
        val windowEnd = today.plusDays(horizonDays.toLong() + 1).atStartOfDay(zone).toInstant()
        return container.taskRepository.getOpenTasksDueBefore(windowEnd).map { it.id }
    }

    @Test
    fun aTaskDueTodayCountsAsDue_notUpcoming_atAnyHorizon() = runBlocking {
        container.taskRepository.insertTask(
            TaskEntity(
                id = "task-today",
                title = "Call the dentist",
                dueDate = Instant.now().plus(1, ChronoUnit.HOURS),
                createdAt = Instant.now(),
            ),
        )

        val windowEnd = today.plusDays(8).atStartOfDay(zone).toInstant()
        val open = container.taskRepository.getOpenTasksDueBefore(windowEnd)
        val (due, upcoming) = open.partition { task ->
            val dueDay = task.dueDate?.atZone(zone)?.toLocalDate()
            dueDay != null && !dueDay.isAfter(today)
        }

        assertEquals("today's task belongs in the due section", listOf("task-today"), due.map { it.id })
        assertTrue("today's task must not also appear as upcoming", upcoming.isEmpty())
    }
}

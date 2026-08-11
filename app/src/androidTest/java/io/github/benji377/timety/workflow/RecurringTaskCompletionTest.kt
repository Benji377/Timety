package io.github.benji377.timety.workflow

import android.app.Application
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.benji377.timety.data.model.task.RecurrenceUnit
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.user.UserProfileEntity
import io.github.benji377.timety.testutil.TestAppContainer
import io.github.benji377.timety.testutil.TestViewModels
import io.github.benji377.timety.testutil.awaitTrue
import io.github.benji377.timety.testutil.get
import io.github.benji377.timety.ui.viewmodel.RecurringTaskViewModel
import io.github.benji377.timety.util.stats.ExperienceEngine
import io.github.benji377.timety.util.task.RecurrenceUtils
import io.github.benji377.timety.util.task.RecurringStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Completing a recurring task has to do four things at once: log an occurrence, roll the due date
 * forward, award XP, and leave the data the task widget reads in a state where the task is no
 * longer due today. The last of those is what broke once already, when the view model advanced the
 * due date without telling the widget.
 */
@RunWith(AndroidJUnit4::class)
class RecurringTaskCompletionTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val application = context.applicationContext as Application
    private val zone: ZoneId = ZoneId.systemDefault()

    private lateinit var container: TestAppContainer
    private lateinit var viewModels: TestViewModels

    @Before
    fun setUp() {
        container = TestAppContainer(context)
        viewModels = TestViewModels(application, container)
        runBlocking {
            container.userRepository.insertUserProfile(
                UserProfileEntity(name = "Tester", accountCreated = Instant.now(), totalXp = 0),
            )
        }
    }

    @After
    fun tearDown() {
        viewModels.clear()
        container.close()
    }

    @Test
    fun completingAnOccurrence_logsIt_advancesTheDueDate_awardsXp_andClearsItFromToday() =
        runBlocking {
            val dueToday = LocalDate.now().atStartOfDay(zone).plusHours(9).toInstant()
            val task = RecurringTaskEntity(
                id = "recurring-1",
                title = "Water the plants",
                dueDate = dueToday,
                unit = RecurrenceUnit.WEEK,
                interval = 1,
                createdAt = Instant.now(),
            )
            container.recurringTaskRepository.insertTask(task)

            assertEquals(
                "precondition: the task should start out due today",
                RecurringStatus.DUE_TODAY,
                RecurrenceUtils.statusOf(task, dueToday.minusSeconds(60), 7, zone),
            )

            viewModels.get<RecurringTaskViewModel>().completeOccurrence(task)

            val advanced = awaitTrue {
                runBlocking { container.recurringTaskRepository.getTaskById(task.id) }
                    ?.dueDate != dueToday
            }
            assertTrue("the due date should roll forward on completion", advanced)

            val stored = container.recurringTaskRepository.getTaskById(task.id)!!
            assertEquals(
                "the new due date should be the next weekly occurrence",
                RecurrenceUtils.nextDueDate(task, dueToday),
                stored.dueDate,
            )
            assertNotEquals(
                "after completing today's occurrence the task must not still read as due today",
                RecurringStatus.DUE_TODAY,
                RecurrenceUtils.statusOf(stored, Instant.now(), 7, zone),
            )

            val withOccurrences =
                container.recurringTaskRepository.allRecurringTasks.first().single()
            assertEquals(
                "completing should log exactly one occurrence",
                1,
                withOccurrences.occurrences.size,
            )

            assertTrue(
                "completing a recurring occurrence should award task XP",
                awaitTrue {
                    runBlocking { container.userRepository.userProfile.first() }?.totalXp ==
                        ExperienceEngine.XP_PER_TASK
                },
            )
        }
}

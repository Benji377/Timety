package io.github.benji377.timety.workflow

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.benji377.timety.data.model.task.OccurrenceStatus
import io.github.benji377.timety.data.model.task.RecurrenceUnit
import io.github.benji377.timety.data.model.task.RecurringOccurrenceEntity
import io.github.benji377.timety.data.model.task.RecurringSubtaskEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.services.CompletionActions
import io.github.benji377.timety.testutil.TestAppContainer
import io.github.benji377.timety.util.task.RecurrenceUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Skipping a round of a recurring task (#265): logged, moves the schedule on, and can be undone. */
@RunWith(AndroidJUnit4::class)
class RecurringSkipTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var container: TestAppContainer
    private val repository get() = container.recurringTaskRepository

    private val due = Instant.now().truncatedTo(ChronoUnit.MINUTES).minus(1, ChronoUnit.HOURS)
    private val task = RecurringTaskEntity(
        id = "recurring-s",
        title = "Bins",
        dueDate = due,
        unit = RecurrenceUnit.WEEK,
        createdAt = due,
    )

    @Before
    fun setUp() {
        container = TestAppContainer(context)
        runBlocking { repository.insertTask(task) }
    }

    @After
    fun tearDown() = container.close()

    private fun stored(): RecurringTaskEntity = runBlocking { repository.getTaskById(task.id) }!!

    private fun log(): List<RecurringOccurrenceEntity> =
        runBlocking { repository.observeTask(task.id).first() }!!.log

    private fun skip(snapshot: RecurringTaskEntity = stored()) = runBlocking {
        CompletionActions.completeRecurringOccurrence(context, repository, snapshot, OccurrenceStatus.SKIPPED)
    }

    @Test
    fun skipping_logsASkip_andMovesTheDueDateLikeACompletion() {
        val expectedNext = RecurrenceUtils.nextDueDate(task, Instant.now())

        assertNotNull(skip())

        val entry = log().single()
        assertEquals(OccurrenceStatus.SKIPPED, entry.status)
        assertEquals(due, entry.previousDueDate)
        assertEquals(expectedNext, stored().dueDate)
    }

    @Test
    fun aSecondTapWithTheSameSnapshot_isIgnored() {
        skip(task)
        assertNull(skip(task))
        assertEquals(1, log().size)
    }

    @Test
    fun skipping_leavesTheChecklistAlone_andRecordsNoSnapshot() {
        runBlocking {
            repository.insertSubtask(RecurringSubtaskEntity(id = "s", recurringTaskId = task.id, title = "Sort", isCompleted = true))
        }

        skip()

        assertNull(log().single().subtasksTotal)
        assertTrue(runBlocking { repository.getSubtasks(task.id) }.single().isCompleted)
    }

    @Test
    fun uncheckingTheSkip_putsTheDueDateBack() {
        skip()

        runBlocking { CompletionActions.uncheckRecurringOccurrence(context, repository, log().single()) }

        assertEquals(due, stored().dueDate)
        assertTrue(log().isEmpty())
    }

    @Test
    fun aCompletionAfterASkip_isTheOneThatUnchecksTheDueDate() {
        skip()
        val afterSkip = stored().dueDate
        runBlocking { CompletionActions.completeRecurringOccurrence(context, repository, stored()) }

        val latest = log().maxBy { it.id }
        runBlocking { CompletionActions.uncheckRecurringOccurrence(context, repository, latest) }

        assertEquals(afterSkip, stored().dueDate)
    }
}

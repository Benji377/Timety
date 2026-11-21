package io.github.benji377.timety.workflow

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.benji377.timety.data.model.task.RecurrenceUnit
import io.github.benji377.timety.data.model.task.RecurringOccurrenceEntity
import io.github.benji377.timety.data.model.task.RecurringSubtaskEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.services.CompletionActions
import io.github.benji377.timety.testutil.TestAppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * A recurring task's checklist: completing a round records how much was ticked, then unticks
 * everything for the next round; the undo data holds the ticks so they can be put back.
 */
@RunWith(AndroidJUnit4::class)
class RecurringChecklistTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var container: TestAppContainer
    private val repository get() = container.recurringTaskRepository

    private val due = Instant.now().truncatedTo(ChronoUnit.MINUTES).minus(1, ChronoUnit.HOURS)
    private val task = RecurringTaskEntity(
        id = "recurring-c",
        title = "Clean the flat",
        dueDate = due,
        unit = RecurrenceUnit.WEEK,
        createdAt = due,
    )
    private val subtasks = listOf("Kitchen", "Bathroom", "Floors").mapIndexed { index, title ->
        RecurringSubtaskEntity(id = "s$index", recurringTaskId = task.id, title = title, sortOrder = index)
    }

    @Before
    fun setUp() {
        container = TestAppContainer(context)
        runBlocking { repository.insertTask(task) }
    }

    @After
    fun tearDown() = container.close()

    private fun addChecklist(ticked: Int) = runBlocking {
        subtasks.forEachIndexed { index, subtask ->
            repository.insertSubtask(subtask.copy(isCompleted = index < ticked))
        }
    }

    private fun complete() = runBlocking {
        CompletionActions.completeRecurringOccurrence(context, repository, repository.getTaskById(task.id)!!)
    }

    private fun latestOccurrence(): RecurringOccurrenceEntity =
        runBlocking { repository.observeTask(task.id).first() }!!.log.maxBy { it.id }

    private fun checklist(): List<RecurringSubtaskEntity> =
        runBlocking { repository.observeTask(task.id).first() }!!.subtasks

    @Test
    fun completing_recordsTheTickedShare_andUnticksTheChecklist() {
        addChecklist(ticked = 2)

        complete()

        assertEquals(2, latestOccurrence().subtasksDone)
        assertEquals(3, latestOccurrence().subtasksTotal)
        assertTrue("the next round starts unticked", checklist().none { it.isCompleted })
    }

    @Test
    fun completingWithoutTicking_recordsNoneDone() {
        addChecklist(ticked = 0)

        complete()

        assertEquals(0, latestOccurrence().subtasksDone)
        assertEquals(3, latestOccurrence().subtasksTotal)
    }

    @Test
    fun withoutAChecklist_nothingIsRecorded() {
        complete()

        assertNull(latestOccurrence().subtasksDone)
        assertNull(latestOccurrence().subtasksTotal)
    }

    @Test
    fun theUndoData_holdsTheTicksFromBeforeTheReset() {
        addChecklist(ticked = 1)

        val undo = complete()!!
        runBlocking { repository.updateSubtasks(undo.subtasksBefore) }

        assertEquals(listOf(true, false, false), checklist().map { it.isCompleted })
    }

    @Test
    fun theChecklist_comesBackInItsOwnOrder() {
        runBlocking { subtasks.reversed().forEach { repository.insertSubtask(it) } }

        assertEquals(listOf("Kitchen", "Bathroom", "Floors"), checklist().map { it.title })
    }

    @Test
    fun deletingTheTask_deletesItsChecklist() {
        addChecklist(ticked = 0)

        runBlocking { repository.deleteTask(task) }

        assertTrue(runBlocking { repository.getSubtasks(task.id) }.isEmpty())
    }
}

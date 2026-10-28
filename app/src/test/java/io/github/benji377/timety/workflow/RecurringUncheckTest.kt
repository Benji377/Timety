package io.github.benji377.timety.workflow

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.benji377.timety.data.model.task.RecurrenceUnit
import io.github.benji377.timety.data.model.task.RecurringOccurrenceEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.TaskCategoryEntity
import io.github.benji377.timety.services.CompletionActions
import io.github.benji377.timety.testutil.TestAppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Completing and unchecking a recurring task: a repeated tap must not log twice, and unchecking may
 * only move the due date back when nothing has moved it since.
 */
@RunWith(AndroidJUnit4::class)
class RecurringUncheckTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var container: TestAppContainer
    private val repository get() = container.recurringTaskRepository

    private val due = Instant.now().truncatedTo(ChronoUnit.MINUTES).minus(1, ChronoUnit.HOURS)
    private val task = RecurringTaskEntity(
        id = "recurring-u",
        title = "Water the plants",
        category = "Home",
        dueDate = due,
        unit = RecurrenceUnit.DAY,
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

    private fun occurrences(): List<RecurringOccurrenceEntity> =
        runBlocking { repository.observeTask(task.id).first() }!!.occurrences.sortedBy { it.id }

    private fun complete(snapshot: RecurringTaskEntity) =
        runBlocking { CompletionActions.completeRecurringOccurrence(context, repository, snapshot) }

    private fun uncheck(occurrence: RecurringOccurrenceEntity) =
        runBlocking { CompletionActions.uncheckRecurringOccurrence(context, repository, occurrence) }

    @Test
    fun aSecondTapWithTheSameSnapshot_isIgnored() {
        assertNotNull(complete(task))
        assertNull("the due date already moved", complete(task))
        assertEquals(1, occurrences().size)
    }

    @Test
    fun uncheckingTheLatestCompletion_restoresTheDueDate() {
        complete(task)
        uncheck(occurrences().single())
        assertEquals(due, stored().dueDate)
        assertEquals(0, occurrences().size)
    }

    @Test
    fun uncheckingAnOlderCompletion_onlyRemovesTheEntry() {
        complete(task)
        complete(stored())
        val afterSecond = stored().dueDate
        uncheck(occurrences().first())
        assertEquals(afterSecond, stored().dueDate)
        assertEquals(1, occurrences().size)
    }

    @Test
    fun uncheckingAfterAnEdit_keepsTheEditedDueDate() {
        complete(task)
        val edited = stored().copy(dueDate = due.plus(5, ChronoUnit.DAYS))
        runBlocking { repository.updateTask(edited) }
        uncheck(occurrences().single())
        assertEquals(edited.dueDate, stored().dueDate)
        assertEquals(0, occurrences().size)
    }

    @Test
    fun uncheckingABackdatedEntry_leavesTheDueDate() {
        runBlocking {
            repository.insertOccurrence(
                RecurringOccurrenceEntity(recurringTaskId = task.id, completedAt = due.minus(2, ChronoUnit.DAYS))
            )
        }
        uncheck(occurrences().single())
        assertEquals(due, stored().dueDate)
        assertEquals(0, occurrences().size)
    }

    @Test
    fun uncheckingTwice_changesNothingTheSecondTime() {
        complete(task)
        val occurrence = occurrences().single()
        uncheck(occurrence)
        uncheck(occurrence)
        assertEquals(due, stored().dueDate)
    }

    @Test
    fun renamingOrDeletingACategory_updatesRecurringTasksToo() = runBlocking {
        val category = TaskCategoryEntity(id = "c1", name = "Home", colorValue = 0)
        container.taskRepository.createCategory(category)

        container.taskRepository.updateCategory("Home", category.copy(name = "House"))
        assertEquals("House", stored().category)

        container.taskRepository.deleteCategory(category.copy(name = "House"))
        assertEquals("", stored().category)
    }
}

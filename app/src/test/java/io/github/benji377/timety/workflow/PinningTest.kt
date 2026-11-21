package io.github.benji377.timety.workflow

import android.app.Application
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.benji377.timety.data.model.task.RecurrenceUnit
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.services.CompletionActions
import io.github.benji377.timety.testutil.TestAppContainer
import io.github.benji377.timety.testutil.TestViewModels
import io.github.benji377.timety.testutil.awaitTrue
import io.github.benji377.timety.testutil.get
import io.github.benji377.timety.ui.viewmodel.RecurringTaskViewModel
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
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

/** Completing a one-off task unpins it for good; a recurring task stays pinned across rounds. */
@RunWith(AndroidJUnit4::class)
class PinningTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val application = context.applicationContext as Application
    private val pinnedAt = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)

    private lateinit var container: TestAppContainer
    private lateinit var viewModels: TestViewModels

    @Before
    fun setUp() {
        container = TestAppContainer(context)
        viewModels = TestViewModels(application, container)
    }

    @After
    fun tearDown() {
        viewModels.clear()
        container.close()
    }

    private fun pinnedTask() = TaskEntity(id = "t1", title = "Call the bank", createdAt = Instant.now(), pinnedAt = pinnedAt)

    private fun storedTask() = runBlocking { container.taskRepository.getTaskById("t1") }!!

    @Test
    fun checkingAPinnedTask_unpinsIt_andUncheckingDoesNotPinItAgain() = runBlocking {
        container.taskRepository.insertTask(pinnedTask())
        val taskViewModel = viewModels.get<TaskViewModel>()

        taskViewModel.toggleTaskCompletion(storedTask())
        assertTrue(awaitTrue { storedTask().isCompleted })
        assertNull("a done task has nothing to stay on top for", storedTask().pinnedAt)

        taskViewModel.toggleTaskCompletion(storedTask())
        assertTrue(awaitTrue { !storedTask().isCompleted })
        assertNull("reopening the task leaves it unpinned", storedTask().pinnedAt)
    }

    @Test
    fun undoingTheCheck_bringsThePinBack() = runBlocking {
        val before = pinnedTask()
        container.taskRepository.insertTask(before)
        val taskViewModel = viewModels.get<TaskViewModel>()

        taskViewModel.toggleTaskCompletion(before)
        assertTrue(awaitTrue { storedTask().isCompleted })
        // The snackbar's Undo writes back the task as it was before the check.
        taskViewModel.updateTask(before)

        assertTrue(awaitTrue { !storedTask().isCompleted })
        assertEquals(pinnedAt, storedTask().pinnedAt)
    }

    @Test
    fun completingFromTheWidgetOrANotification_unpinsToo() = runBlocking {
        container.taskRepository.insertTask(pinnedTask())

        CompletionActions.completeTask(context, container.taskRepository, "t1")

        assertTrue(storedTask().isCompleted)
        assertNull(storedTask().pinnedAt)
    }

    @Test
    fun completingARoundOfAPinnedRecurringTask_keepsThePin() = runBlocking {
        val due = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val task = RecurringTaskEntity(
            id = "r1",
            title = "Water the plants",
            dueDate = due,
            unit = RecurrenceUnit.WEEK,
            createdAt = Instant.now(),
            pinnedAt = pinnedAt,
        )
        container.recurringTaskRepository.insertTask(task)

        viewModels.get<RecurringTaskViewModel>().completeOccurrence(task)

        assertTrue(awaitTrue { runBlocking { container.recurringTaskRepository.getTaskById("r1") }!!.dueDate != due })
        assertEquals(pinnedAt, container.recurringTaskRepository.getTaskById("r1")!!.pinnedAt)
    }

    @Test
    fun pinningAndUnpinning_onlyTouchesThePin() = runBlocking {
        container.taskRepository.insertTask(pinnedTask().copy(pinnedAt = null))
        val taskViewModel = viewModels.get<TaskViewModel>()

        taskViewModel.setPinned("t1", true)
        assertTrue(awaitTrue { storedTask().pinnedAt != null })
        assertEquals("Call the bank", storedTask().title)

        taskViewModel.setPinned("t1", false)
        assertTrue(awaitTrue { storedTask().pinnedAt == null })
    }
}

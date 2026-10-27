package io.github.benji377.timety.workflow

import android.app.Application
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.benji377.timety.data.model.focus.FocusModeEntity
import io.github.benji377.timety.data.model.focus.FocusModeType
import io.github.benji377.timety.data.model.focus.FocusTargetSelection
import io.github.benji377.timety.data.model.focus.FocusTargetType
import io.github.benji377.timety.data.model.focus.PhaseType
import io.github.benji377.timety.data.model.focus.SessionPhaseEntity
import io.github.benji377.timety.data.model.goal.GoalEntity
import io.github.benji377.timety.data.model.task.RecurrenceUnit
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.services.FocusTimerManager
import io.github.benji377.timety.testutil.TestAppContainer
import io.github.benji377.timety.testutil.TestViewModels
import io.github.benji377.timety.testutil.awaitTrue
import io.github.benji377.timety.testutil.get
import io.github.benji377.timety.testutil.settle
import io.github.benji377.timety.testutil.value
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.ui.viewmodel.ModeDeleteBlocker
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
 * Drives [FocusViewModel] through finished focus phases and session ends to check what
 * auto-complete does to each target type: at most one completion per session, never for goals,
 * nothing while the setting is off, and nothing at all for a discarded session.
 */
@RunWith(AndroidJUnit4::class)
class FocusAutoCompleteTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val application = context.applicationContext as Application

    private lateinit var container: TestAppContainer
    private lateinit var viewModels: TestViewModels
    private lateinit var focusViewModel: FocusViewModel

    private val now: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)
    private val mode = FocusModeEntity("mode-1", "Deep work", FocusModeType.CUSTOM)

    @Before
    fun setUp() {
        container = TestAppContainer(context)
        viewModels = TestViewModels(application, container)
        runBlocking {
            // Two phases, so finishing the first one waits for "continue" instead of ending the session.
            container.focusRepository.insertModeWithPhases(
                mode,
                listOf(
                    SessionPhaseEntity(modeId = mode.id, type = PhaseType.FOCUS, durationMinutes = PHASE_MINUTES, orderIndex = 0),
                    SessionPhaseEntity(modeId = mode.id, type = PhaseType.REST, durationMinutes = 5, orderIndex = 1),
                ),
            )
            container.settingsRepository.saveAutoCompleteFocus(true)
        }
        focusViewModel = viewModels.get()
        // The timer events are fire-and-forget: one emitted before the view model subscribes is lost.
        assertTrue(
            "the view model should subscribe to the timer events",
            awaitTrue {
                FocusTimerManager.phaseCompleteEvent.subscriptionCount.value > 0 &&
                    FocusTimerManager.stopEvent.subscriptionCount.value > 0
            },
        )
    }

    @After
    fun tearDown() {
        viewModels.clear()
        container.close()
    }

    @Test
    fun recurringTask_isCompletedOncePerDay_notPerPhaseOrSession() {
        val task = RecurringTaskEntity(
            id = "recurring-1",
            title = "Water the plants",
            dueDate = now.plus(1, ChronoUnit.DAYS),
            unit = RecurrenceUnit.WEEK,
            createdAt = now,
        )
        runBlocking { container.recurringTaskRepository.insertTask(task) }
        startSession(FocusTargetSelection.of(FocusTargetType.RECURRING_TASK, task.id, task.title))

        finishFocusPhase()
        finishFocusPhase()
        // Give a premature completion the time to land before asserting it didn't.
        settle()
        assertEquals("nothing is completed before the session is logged", 0, occurrenceCount(task.id))

        stopSession()
        awaitOccurrences(task.id, 1, "logging the session should complete the occurrence once")
        startSession(FocusTargetSelection.of(FocusTargetType.RECURRING_TASK, task.id, task.title))
        finishFocusPhase()
        stopSession()
        settle()
        assertEquals("a second session the same day must not complete it again", 1, occurrenceCount(task.id))
    }

    @Test
    fun goal_isNeverCompleted_butTheSessionIsAttributedToIt() {
        val goal = GoalEntity(
            id = "goal-1",
            name = "Ride 100 km",
            colorValue = 0,
            targetValue = 100,
            unitLabel = "km",
            targetDate = now.plus(30, ChronoUnit.DAYS),
            createdAt = now,
        )
        runBlocking { container.goalRepository.insertGoal(goal) }
        startSession(FocusTargetSelection.of(FocusTargetType.GOAL, goal.id, goal.name))

        finishFocusPhase()
        stopSession()

        val session = container.focusRepository.allSessions.let { sessions ->
            assertTrue("the session should be logged", awaitTrue { sessions.value().isNotEmpty() })
            sessions.value().single()
        }
        assertEquals(FocusTargetType.GOAL, session.targetType)
        assertEquals(goal.id, session.targetId)
        assertNull("goal sessions carry no tag", session.tagId)

        val stored = runBlocking { container.goalRepository.getGoalWithEntriesById(goal.id)!! }
        assertTrue("focus time must not add goal progress", stored.entries.isEmpty())
        assertNull("the goal must stay active", stored.goal.completedAt)
    }

    @Test
    fun task_isCompletedWithoutTheFocusScreen_andOnlyWhileTheSettingIsOn() {
        val task = TaskEntity(id = "task-1", title = "Write the report", createdAt = now)
        runBlocking {
            container.taskRepository.insertTask(task)
            container.settingsRepository.saveAutoCompleteFocus(false)
        }
        startSession(FocusTargetSelection.of(FocusTargetType.TASK, task.id, task.title))

        finishFocusPhase()
        stopSession()
        assertTrue("setting off: the task stays open", !isTaskCompleted(task.id))

        runBlocking { container.settingsRepository.saveAutoCompleteFocus(true) }
        startSession(FocusTargetSelection.of(FocusTargetType.TASK, task.id, task.title))
        finishFocusPhase()
        stopSession()
        assertTrue("setting on: the task is completed", awaitTrue { isTaskCompleted(task.id) })
        assertTrue(
            "both sessions are logged",
            awaitTrue { container.focusRepository.allSessions.value().size == 2 },
        )
    }

    @Test
    fun discardedSession_logsAndCompletesNothing() {
        val task = TaskEntity(id = "task-2", title = "Tidy the desk", createdAt = now)
        runBlocking { container.taskRepository.insertTask(task) }
        startSession(FocusTargetSelection.of(FocusTargetType.TASK, task.id, task.title))

        finishFocusPhase()
        settle()
        assertTrue(
            "a finished phase logs nothing yet",
            container.focusRepository.allSessions.value().isEmpty(),
        )

        FocusTimerManager.stopEvent.tryEmit(
            FocusTimerManager.StopInfo(elapsedFocusSeconds = 120, wasRestPhase = false, discard = true)
        )
        settle()
        assertTrue("discarding completes nothing", !isTaskCompleted(task.id))
        assertTrue("discarding logs no session", container.focusRepository.allSessions.value().isEmpty())
    }

    @Test
    fun sessionStoppedBeforeAFocusPhaseFinished_isLoggedButCompletesNothing() {
        val task = TaskEntity(id = "task-3", title = "Read a chapter", createdAt = now)
        runBlocking { container.taskRepository.insertTask(task) }
        startSession(FocusTargetSelection.of(FocusTargetType.TASK, task.id, task.title))

        FocusTimerManager.stopEvent.tryEmit(
            FocusTimerManager.StopInfo(elapsedFocusSeconds = 120, wasRestPhase = false, discard = false)
        )
        assertTrue(
            "the two minutes are logged",
            awaitTrue {
                container.focusRepository.allSessions.value().singleOrNull()?.totalSecondsFocused == 120
            },
        )
        settle()
        assertTrue("no focus phase finished: the task stays open", !isTaskCompleted(task.id))
    }

    @Test
    fun theRunningSessionsMode_cannotBeDeleted() {
        startSession(FocusTargetSelection.of(FocusTargetType.TAG, "default_tag", "None"))
        var blocker: ModeDeleteBlocker? = null
        focusViewModel.deleteMode(mode) { blocker = it }

        assertEquals(ModeDeleteBlocker.SESSION_RUNNING, blocker)
        settle()
        assertTrue(
            "the mode is kept",
            container.focusRepository.allModes.value().any { it.id == mode.id },
        )
    }

    private fun startSession(target: FocusTargetSelection) {
        focusViewModel.setSelectedTarget(target)
        focusViewModel.setActiveSessionMode(mode.id)
    }

    /**
     * Finishes a focus phase and waits until the view model has handled it. Raising "awaiting
     * continue" is the handler's last step, so it also means the phase's time has been banked.
     */
    private fun finishFocusPhase() {
        focusViewModel.setAwaitingContinue(false)
        FocusTimerManager.phaseCompleteEvent.tryEmit(false to PHASE_MINUTES * 60)
        assertTrue(
            "the finished phase should be handled",
            awaitTrue { focusViewModel.awaitingContinue.value },
        )
    }

    private fun stopSession() {
        FocusTimerManager.stopEvent.tryEmit(
            FocusTimerManager.StopInfo(elapsedFocusSeconds = 0, wasRestPhase = false, discard = false)
        )
        // The stop is handled asynchronously; let it reset the session before the next one starts.
        settle()
    }

    private fun occurrenceCount(recurringTaskId: String): Int =
        container.recurringTaskRepository.allRecurringTasks.value()
            .single { it.task.id == recurringTaskId }.occurrences.size

    private fun awaitOccurrences(recurringTaskId: String, expected: Int, step: String) {
        assertTrue(
            "$step: expected $expected occurrences but was ${occurrenceCount(recurringTaskId)}",
            awaitTrue { occurrenceCount(recurringTaskId) == expected },
        )
    }

    private fun isTaskCompleted(id: String): Boolean =
        runBlocking { container.taskRepository.getTaskById(id)!!.isCompleted }

    private companion object {
        const val PHASE_MINUTES = 25
    }
}

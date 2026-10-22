package io.github.benji377.timety.workflow

import android.app.Application
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.benji377.timety.data.model.focus.FocusModeEntity
import io.github.benji377.timety.data.model.focus.FocusModeType
import io.github.benji377.timety.data.model.focus.FocusTargetSelection
import io.github.benji377.timety.data.model.focus.FocusTargetType
import io.github.benji377.timety.data.model.goal.GoalEntity
import io.github.benji377.timety.data.model.task.RecurrenceUnit
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.user.UserProfileEntity
import io.github.benji377.timety.services.FocusTimerManager
import io.github.benji377.timety.testutil.TestAppContainer
import io.github.benji377.timety.testutil.TestViewModels
import io.github.benji377.timety.testutil.awaitTrue
import io.github.benji377.timety.testutil.get
import io.github.benji377.timety.testutil.value
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.util.stats.ExperienceEngine
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
 * Drives [FocusViewModel] through finished focus phases to check what auto-complete does to each
 * target type: at most one completion per session, never for goals, and nothing while the setting
 * is off.
 */
@RunWith(AndroidJUnit4::class)
class FocusAutoCompleteTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val application = context.applicationContext as Application

    private lateinit var container: TestAppContainer
    private lateinit var viewModels: TestViewModels
    private lateinit var focusViewModel: FocusViewModel

    private val now: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)
    private val mode = FocusModeEntity("mode-1", "Deep work", FocusModeType.POMODORO)

    @Before
    fun setUp() {
        container = TestAppContainer(context)
        viewModels = TestViewModels(application, container)
        runBlocking {
            container.userRepository.insertUserProfile(
                UserProfileEntity(name = "Tester", accountCreated = now, totalXp = 0),
            )
            container.focusRepository.insertModeWithPhases(mode, emptyList())
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
        awaitOccurrences(task.id, 1, "the first focus phase should complete the occurrence")
        finishFocusPhase()
        // Give a wrongly repeated completion the time to land before asserting it didn't.
        Thread.sleep(500)
        assertEquals("a second phase must not complete it again", 1, occurrenceCount(task.id))

        stopSession()
        startSession(FocusTargetSelection.of(FocusTargetType.RECURRING_TASK, task.id, task.title))
        finishFocusPhase()
        Thread.sleep(500)
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
        Thread.sleep(500)
        assertTrue("setting off: the task stays open", !isTaskCompleted(task.id))

        stopSession()
        runBlocking { container.settingsRepository.saveAutoCompleteFocus(true) }
        startSession(FocusTargetSelection.of(FocusTargetType.TASK, task.id, task.title))
        finishFocusPhase()
        assertTrue("setting on: the task is completed", awaitTrue { isTaskCompleted(task.id) })
        assertTrue(
            "completion XP is awarded alongside the focus minutes",
            awaitTrue {
                currentXp() == ExperienceEngine.XP_PER_TASK +
                    2 * PHASE_MINUTES * ExperienceEngine.XP_PER_FOCUS_MINS
            },
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
        Thread.sleep(500)
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

    private fun currentXp(): Int? = container.userRepository.userProfile.value()?.totalXp

    private companion object {
        const val PHASE_MINUTES = 25
    }
}

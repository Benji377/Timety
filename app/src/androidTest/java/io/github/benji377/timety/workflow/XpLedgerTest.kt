package io.github.benji377.timety.workflow

import android.app.Application
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.benji377.timety.data.model.focus.FocusModeEntity
import io.github.benji377.timety.data.model.focus.FocusModeType
import io.github.benji377.timety.data.model.goal.GoalEntity
import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.user.UserProfileEntity
import io.github.benji377.timety.testutil.TestAppContainer
import io.github.benji377.timety.testutil.TestViewModels
import io.github.benji377.timety.testutil.awaitTrue
import io.github.benji377.timety.testutil.get
import io.github.benji377.timety.ui.viewmodel.GoalViewModel
import io.github.benji377.timety.ui.viewmodel.HabitViewModel
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import io.github.benji377.timety.util.stats.ExperienceEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Walks one user through completing a task, a habit, a goal, and a focus session, checking the
 * shared XP total after every step and after every reversal.
 *
 * XP is the app's most cross-cutting piece of state: six view models write it and the profile and
 * statistics screens read it, so an award that stops matching its reversal only shows up as a
 * total that slowly drifts. Asserting the running total after each step localizes which feature
 * broke rather than just reporting that the number is wrong.
 */
@RunWith(AndroidJUnit4::class)
class XpLedgerTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val application = context.applicationContext as Application

    private lateinit var container: TestAppContainer
    private lateinit var viewModels: TestViewModels

    private val now: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)

    @Before
    fun setUp() {
        container = TestAppContainer(context)
        viewModels = TestViewModels(application, container)
        runBlocking {
            container.userRepository.insertUserProfile(
                UserProfileEntity(name = "Tester", accountCreated = now, totalXp = 0),
            )
        }
    }

    @After
    fun tearDown() {
        viewModels.clear()
        container.close()
    }

    @Test
    fun completingEachFeature_movesTheSharedXpTotalByItsOwnAmount() {
        val taskViewModel = viewModels.get<TaskViewModel>()
        val habitViewModel = viewModels.get<HabitViewModel>()
        val goalViewModel = viewModels.get<GoalViewModel>()

        // Rows are seeded through the repositories, not the view models. A view model mutator
        // suspends on its IO hop, so a second view model call issued straight after it can observe
        // the row as still missing and quietly do nothing - which shows up as a missing XP award
        // rather than as an error. The view models below are exercised for the behaviour under
        // test (the XP side effect of completing something), not to get rows into the database.
        val task = TaskEntity(id = "task-1", title = "Write the report", createdAt = now)
        val habit = HabitEntity(
            id = "habit-1",
            name = "Read",
            frequency = HabitFrequency.DAILY,
            createdAt = now,
            colorValue = 0,
        )
        val goal = GoalEntity(
            id = "goal-1",
            name = "Ride 100 km",
            colorValue = 0,
            targetValue = 100,
            unitLabel = "km",
            targetDate = now.plus(30, ChronoUnit.DAYS),
            createdAt = now,
        )
        runBlocking {
            container.taskRepository.insertTask(task)
            container.habitRepository.insertHabit(habit)
            container.goalRepository.insertGoal(goal)
        }
        awaitXp(0, "seeding rows should not award anything on its own")

        taskViewModel.toggleTaskCompletion(task)
        awaitXp(ExperienceEngine.XP_PER_TASK, "completing a task")

        taskViewModel.toggleTaskCompletion(currentTask(task.id))
        awaitXp(0, "un-completing a task should give the XP back")

        habitViewModel.toggleCompletionToday(habit.id)
        awaitXp(ExperienceEngine.XP_PER_HABIT, "completing a habit")

        habitViewModel.toggleCompletionToday(habit.id)
        awaitXp(0, "un-completing a habit should give the XP back")

        goalViewModel.addEntry(goal.id, value = 40, timestamp = now)
        awaitXp(0, "partial goal progress should not award the completion bonus")

        goalViewModel.addEntry(goal.id, value = 60, timestamp = now)
        awaitXp(ExperienceEngine.XP_PER_GOAL, "reaching a goal's target")

        val lastEntry = runBlocking {
            container.goalRepository.getGoalWithEntriesById(goal.id)!!.entries.maxBy { it.id }
        }
        goalViewModel.deleteEntry(lastEntry)
        awaitXp(0, "dropping back below the target should take the goal bonus back")
    }

    @Test
    fun loggingAFocusSession_awardsOneXpPerMinute_andDeletingItTakesThemBack() {
        val focusViewModel = viewModels.get<io.github.benji377.timety.ui.viewmodel.FocusViewModel>()

        val mode = FocusModeEntity("mode-1", "Deep work", FocusModeType.POMODORO)
        runBlocking { container.focusRepository.insertModeWithPhases(mode, emptyList()) }

        val start = now.minus(45, ChronoUnit.MINUTES)
        focusViewModel.logSessionForTask(
            mode = mode,
            startTime = start,
            endTime = now,
            taskId = "task-1",
            taskTitle = "Write the report",
        )

        val minutes = 45
        awaitXp(minutes * ExperienceEngine.XP_PER_FOCUS_MINS, "logging a 45 minute focus session")

        val session = runBlocking { container.focusRepository.allSessions.first().single() }
        focusViewModel.deleteSession(session)
        awaitXp(0, "deleting a logged session should take its XP back")
    }

    @Test
    fun reversingMoreXpThanWasEverEarned_clampsAtZeroRatherThanGoingNegative() {
        val habitViewModel = viewModels.get<HabitViewModel>()

        val habit = HabitEntity(
            id = "habit-1",
            name = "Read",
            frequency = HabitFrequency.DAILY,
            createdAt = now,
            colorValue = 0,
        )
        // Seeded through the repository rather than the view model: view model mutators are
        // fire-and-forget on the main dispatcher, so a direct insert from the test thread would
        // race the habit row into existence and trip the completion's foreign key.
        runBlocking {
            container.habitRepository.insertHabit(habit)
            // Mimics restoring a backup whose habits are already marked complete: the completions
            // exist without this install ever having awarded the XP for them.
            container.habitRepository.insertCompletion(
                HabitCompletionEntity(habitId = habit.id, completionDate = now),
            )
        }

        habitViewModel.toggleCompletionToday(habit.id)

        assertTrue(
            "un-completing a habit that never awarded XP must not push the total negative",
            awaitTrue { (currentXp() ?: -1) >= 0 },
        )
        assertEquals("total should sit at the floor, not below it", 0, currentXp())
    }

    private fun currentXp(): Int? =
        runBlocking { container.userRepository.userProfile.first() }?.totalXp

    private fun currentTask(id: String): TaskEntity =
        runBlocking { container.taskRepository.getTaskById(id)!! }

    private fun awaitXp(expected: Int, step: String) {
        val reached = awaitTrue { currentXp() == expected }
        assertTrue("$step: expected $expected XP but was ${currentXp()}", reached)
    }
}

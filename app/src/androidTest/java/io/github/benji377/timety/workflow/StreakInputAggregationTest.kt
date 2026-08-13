package io.github.benji377.timety.workflow

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.benji377.timety.data.model.focus.FocusModeEntity
import io.github.benji377.timety.data.model.focus.FocusModeType
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.task.RecurrenceUnit
import io.github.benji377.timety.data.model.task.RecurringOccurrenceEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.testutil.TestAppContainer
import io.github.benji377.timety.util.stats.StreakCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The profile screen's streak is fed by four independent features at once: completed tasks,
 * recurring occurrences, habit completions, and focus sessions. Activity in any one of them counts
 * for the day, so a feature that stops contributing its dates silently shortens the user's streak.
 *
 * The aggregation itself is written inline in `ProfileScreen`, so this test reproduces it rather
 * than calling it. That means it can drift from the screen; `CombinedStreakDisplayTest` reads the
 * rendered number and is the real guard. This one localizes which feature stopped contributing.
 */
@RunWith(AndroidJUnit4::class)
class StreakInputAggregationTest {

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
    fun oneDayOfActivityFromEachFeature_formsASingleUnbrokenStreak() = runBlocking {
        // Four consecutive days, each covered by a different feature. If any one stopped feeding
        // the streak the run would break in the middle and the length would drop below four.
        seedCompletedTask(daysAgo = 0)
        seedHabitCompletion(daysAgo = 1)
        seedRecurringOccurrence(daysAgo = 2)
        seedFocusSession(daysAgo = 3)

        val dates = collectActivityDates()

        assertEquals(
            "each feature should contribute exactly one distinct day",
            listOf(
                today.minusDays(3), today.minusDays(2), today.minusDays(1), today,
            ),
            dates,
        )
        assertEquals(
            "four consecutive days of mixed activity is a four day streak",
            4,
            StreakCalculator.currentStreak(dates).length,
        )
        assertEquals(4, StreakCalculator.calculateBestStreak(dates))
    }

    @Test
    fun dropSingleFeature_shortensTheStreak() = runBlocking {
        // Same as above but with nothing on the day the habit used to cover, which is what a
        // regression that stopped contributing habit dates would look like.
        seedCompletedTask(daysAgo = 0)
        seedRecurringOccurrence(daysAgo = 2)
        seedFocusSession(daysAgo = 3)

        val length = StreakCalculator.currentStreak(collectActivityDates()).length
        assertEquals(
            "a gap the never-miss-twice rule can bridge still ends the run at the second miss",
            3,
            length,
        )
    }

    /** The date aggregation `ProfileScreen` performs inline over its four view models. */
    private suspend fun collectActivityDates(): List<LocalDate> {
        val tasks = container.taskRepository.allTasks.first()
        val recurring = container.recurringTaskRepository.allRecurringTasks.first()
        val habits = container.habitRepository.allCompletions.first()
        val sessions = container.focusRepository.allSessions.first()

        val taskDates = tasks.mapNotNull { item ->
            if (item.task.isCompleted) item.task.completedAt?.atZone(zone)?.toLocalDate() else null
        } + recurring.flatMap { item ->
            item.occurrences.map { it.completedAt.atZone(zone).toLocalDate() }
        }
        val habitDates = habits.map { it.completionDate.atZone(zone).toLocalDate() }
        val focusDates = sessions.map { it.startTime.atZone(zone).toLocalDate() }

        return (taskDates + habitDates + focusDates).distinct().sorted()
    }

    private fun instantOn(daysAgo: Long): Instant =
        today.minusDays(daysAgo).atStartOfDay(zone).plusHours(12).toInstant()

    private suspend fun seedCompletedTask(daysAgo: Long) {
        container.taskRepository.insertTask(
            TaskEntity(
                id = "task-$daysAgo",
                title = "Task",
                isCompleted = true,
                completedAt = instantOn(daysAgo),
                createdAt = instantOn(daysAgo),
            ),
        )
    }

    private suspend fun seedHabitCompletion(daysAgo: Long) {
        val habit = HabitEntity(
            id = "habit-$daysAgo",
            name = "Read",
            frequency = HabitFrequency.DAILY,
            createdAt = instantOn(daysAgo),
            colorValue = 0,
        )
        container.habitRepository.insertHabit(habit)
        container.habitRepository.insertCompletion(
            HabitCompletionEntity(habitId = habit.id, completionDate = instantOn(daysAgo)),
        )
    }

    private suspend fun seedRecurringOccurrence(daysAgo: Long) {
        val task = RecurringTaskEntity(
            id = "recurring-$daysAgo",
            title = "Water the plants",
            dueDate = instantOn(daysAgo),
            unit = RecurrenceUnit.WEEK,
            createdAt = instantOn(daysAgo),
        )
        container.recurringTaskRepository.insertTask(task)
        container.recurringTaskRepository.insertOccurrence(
            RecurringOccurrenceEntity(recurringTaskId = task.id, completedAt = instantOn(daysAgo)),
        )
    }

    private suspend fun seedFocusSession(daysAgo: Long) {
        val mode = FocusModeEntity("mode-$daysAgo", "Deep work", FocusModeType.POMODORO)
        container.focusRepository.insertModeWithPhases(mode, emptyList())
        container.focusRepository.insertSession(
            FocusSessionEntity(
                id = "session-$daysAgo",
                modeId = mode.id,
                startTime = instantOn(daysAgo),
                endTime = instantOn(daysAgo),
                totalSecondsFocused = 1500,
                isCompleted = true,
            ),
        )
    }
}

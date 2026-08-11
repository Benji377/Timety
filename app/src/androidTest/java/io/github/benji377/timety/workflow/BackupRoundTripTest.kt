package io.github.benji377.timety.workflow

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.benji377.timety.data.model.focus.DistractionEntity
import io.github.benji377.timety.data.model.focus.DistractionType
import io.github.benji377.timety.data.model.focus.FocusModeEntity
import io.github.benji377.timety.data.model.focus.FocusModeType
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.focus.FocusTagEntity
import io.github.benji377.timety.data.model.focus.FocusTargetType
import io.github.benji377.timety.data.model.focus.PhaseType
import io.github.benji377.timety.data.model.focus.SessionPhaseEntity
import io.github.benji377.timety.data.model.goal.GoalEntity
import io.github.benji377.timety.data.model.goal.GoalEntryEntity
import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.habit.QuickHabitEntity
import io.github.benji377.timety.data.model.task.Priority
import io.github.benji377.timety.data.model.task.RecurrenceUnit
import io.github.benji377.timety.data.model.task.RecurringOccurrenceEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.SubtaskEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.task.TaskSize
import io.github.benji377.timety.data.model.user.DayRatingEntity
import io.github.benji377.timety.data.model.user.UserProfileEntity
import io.github.benji377.timety.testutil.TestAppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Exports a database holding one of every entity kind and restores it into a second, empty one.
 *
 * Assertions compare whole entities rather than a hand-listed set of fields, so a column that
 * backup forgets to serialize fails this test without anyone having to remember to assert on it.
 * That is the point: the two field drops this test was written for (`HabitEntity.sortOrder` and
 * `DistractionEntity.type`) were both invisible to a row-count check.
 */
@RunWith(AndroidJUnit4::class)
class BackupRoundTripTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var source: TestAppContainer
    private lateinit var target: TestAppContainer

    // Truncated to milliseconds: backup serializes instants as ISO-8601 strings, which do not
    // survive nanosecond precision, so equality on raw Instant.now() would fail for the wrong reason.
    private val now: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)

    @Before
    fun setUp() {
        source = TestAppContainer(context)
        target = TestAppContainer(context)
    }

    @After
    fun tearDown() {
        source.close()
        target.close()
    }

    @Test
    fun backupThenRestore_preservesEveryFieldAcrossAllTables() = runBlocking {
        val seeded = seedOneOfEverything(source)

        val exported = source.backupService.exportToShareUri().getOrThrow()
        assertTrue(
            "restore target should start empty",
            target.habitRepository.allHabits.first().isEmpty(),
        )

        target.backupService.importFromUri(exported).getOrThrow()

        assertEquals("habit", seeded.habit, target.habitRepository.allHabits.first().single())
        assertEquals(
            "habit completion",
            seeded.habitCompletion.copy(id = 0),
            target.habitRepository.allCompletions.first().single().copy(id = 0),
        )
        assertEquals(
            "quick habit",
            seeded.quickHabit,
            target.quickHabitRepository.allQuickHabits.first().single(),
        )

        val restoredTask = target.taskRepository.allTasks.first().single()
        assertEquals("task", seeded.task, restoredTask.task)
        assertEquals("subtask", seeded.subtask, restoredTask.subtasks.single())

        val restoredRecurring = target.recurringTaskRepository.allRecurringTasks.first().single()
        assertEquals("recurring task", seeded.recurringTask, restoredRecurring.task)
        assertEquals(
            "recurring occurrence",
            seeded.occurrence.copy(id = 0),
            restoredRecurring.occurrences.single().copy(id = 0),
        )

        val restoredGoal = target.goalRepository.allGoalsWithEntries.first().single()
        assertEquals("goal", seeded.goal, restoredGoal.goal)
        assertEquals(
            "goal entry",
            seeded.goalEntry.copy(id = 0),
            restoredGoal.entries.single().copy(id = 0),
        )

        assertEquals(
            "day rating",
            seeded.dayRating,
            target.dayRatingRepository.allRatings.first().single(),
        )
        assertEquals("user profile", seeded.profile, target.userRepository.userProfile.first())

        assertEquals("focus mode", seeded.mode, target.focusRepository.allModes.first().single())
        assertEquals(
            "session phase",
            seeded.phase.copy(id = 0),
            target.focusRepository.getPhasesForMode(seeded.mode.id).first().single().copy(id = 0),
        )
        assertEquals("focus tag", seeded.tag, target.focusRepository.allTags.first().single())
        assertEquals(
            "focus session",
            seeded.session,
            target.focusRepository.allSessions.first().single(),
        )
        assertEquals(
            "distraction",
            seeded.distraction.copy(id = 0),
            target.focusRepository.allDistractions.first().single().copy(id = 0),
        )
    }

    /**
     * A payload from an older app version has no `recurringTasks`, `goals`, or `quickHabits` keys
     * at all. Restoring it must still succeed and bring the tables it does carry.
     */
    @Test
    fun restoreLegacyPayloadMissingOptionalKeys_doesNotCrash() = runBlocking {
        seedOneOfEverything(source)
        val full = JSONObject(source.backupService.exportToShareUri().getOrThrow().readText())

        listOf("recurringTasks", "goals", "quickHabits", "dayRatings").forEach(full::remove)

        target.backupService.importFromJson(full).getOrThrow()

        assertTrue(
            "tables absent from the payload should end up empty, not crash the import",
            target.recurringTaskRepository.allRecurringTasks.first().isEmpty() &&
                target.goalRepository.allGoalsWithEntries.first().isEmpty() &&
                target.quickHabitRepository.allQuickHabits.first().isEmpty(),
        )
        assertEquals(
            "tables present in the payload should still restore",
            1,
            target.habitRepository.allHabits.first().size,
        )
    }

    private fun android.net.Uri.readText(): String =
        context.contentResolver.openInputStream(this)!!.use { it.reader().readText() }

    private suspend fun seedOneOfEverything(container: TestAppContainer): Seeded {
        val habit = HabitEntity(
            id = "habit-1",
            name = "Read",
            frequency = HabitFrequency.WEEKLY_EXACT,
            targetDaysPerWeek = 3,
            targetWeekdays = "[1,3,5]",
            targetTimeMinutes = 21 * 60,
            createdAt = now,
            colorValue = 0xFF00FF00.toInt(),
            notes = "20 pages",
            iconCodePoint = 0xE800,
            stackName = "Evening",
            stackOrder = 3,
            // The reorder feature's column, dropped by an earlier version of the exporter.
            sortOrder = 7,
        )
        val habitCompletion = HabitCompletionEntity(habitId = habit.id, completionDate = now)
        container.habitRepository.insertHabit(habit)
        container.habitRepository.insertCompletion(habitCompletion)

        val quickHabit = QuickHabitEntity(
            id = "quick-1",
            name = "Drink water",
            intervalMinutes = 90,
            startMinuteOfDay = 8 * 60,
            endMinuteOfDay = 20 * 60,
            targetWeekdays = "[1,2,3,4,5]",
            isEnabled = false,
            createdAt = now,
        )
        container.quickHabitRepository.insert(quickHabit)

        val task = TaskEntity(
            id = "task-1",
            title = "Write the report",
            description = "Two pages",
            dueDate = now.plus(2, ChronoUnit.DAYS),
            location = "Office",
            priority = Priority.VERY_HIGH,
            category = "Work",
            size = TaskSize.LARGE,
            isCompleted = true,
            completedAt = now,
            createdAt = now,
            reminders = listOf(now.plus(1, ChronoUnit.DAYS)),
        )
        val subtask = SubtaskEntity("sub-1", task.id, "Draft it", isCompleted = true)
        container.taskRepository.insertTask(task)
        container.taskRepository.insertSubtask(subtask)

        val recurringTask = RecurringTaskEntity(
            id = "recurring-1",
            title = "Pay rent",
            description = "Standing order",
            category = "Home",
            dueDate = now.plus(5, ChronoUnit.DAYS),
            unit = RecurrenceUnit.MONTH,
            interval = 2,
            monthlyDay = 31,
            reminderOffsetsMinutes = listOf(0, 60),
            createdAt = now,
        )
        val occurrence = RecurringOccurrenceEntity(
            recurringTaskId = recurringTask.id,
            completedAt = now,
        )
        container.recurringTaskRepository.insertTask(recurringTask)
        container.recurringTaskRepository.insertOccurrence(occurrence)

        val goal = GoalEntity(
            id = "goal-1",
            name = "Ride 300 km",
            description = "By September",
            colorValue = 0xFF0000FF.toInt(),
            iconCodePoint = 0xE801,
            targetValue = 300,
            unitLabel = "km",
            targetDate = now.plus(30, ChronoUnit.DAYS),
            createdAt = now,
        )
        val goalEntry = GoalEntryEntity(goalId = goal.id, value = 42, timestamp = now)
        container.goalRepository.insertGoal(goal)
        container.goalRepository.insertEntry(goalEntry)

        val dayRating = DayRatingEntity(dayKey = "2026-08-11", rating = 3, createdAt = now)
        container.dayRatingRepository.upsert(dayRating)

        val profile = UserProfileEntity(
            name = "Benji",
            accountCreated = now,
            unlockedAchievements = listOf("first_task"),
            totalXp = 1234,
        )
        container.userRepository.insertUserProfile(profile)

        val mode = FocusModeEntity("mode-1", "Deep work", FocusModeType.POMODORO)
        val phase = SessionPhaseEntity(
            modeId = mode.id,
            type = PhaseType.FOCUS,
            durationMinutes = 25,
            orderIndex = 0,
        )
        container.focusRepository.insertModeWithPhases(mode, listOf(phase))

        val tag = FocusTagEntity("tag-1", "Study", 0xFFFF0000.toInt())
        container.focusRepository.insertTag(tag)

        val session = FocusSessionEntity(
            id = "session-1",
            modeId = mode.id,
            startTime = now.minus(1, ChronoUnit.HOURS),
            endTime = now,
            totalSecondsFocused = 1500,
            isCompleted = true,
            tagId = tag.id,
            targetType = FocusTargetType.TASK,
            targetId = task.id,
            targetLabel = task.title,
        )
        container.focusRepository.insertSession(session)

        val distraction = DistractionEntity(
            sessionId = session.id,
            time = now,
            // Deliberately not the default: an earlier exporter dropped this column, so every
            // restored distraction came back as DISTRACTED.
            type = DistractionType.HYDRATED,
            note = "refilled bottle",
        )
        container.focusRepository.insertDistraction(distraction)

        return Seeded(
            habit, habitCompletion, quickHabit, task, subtask, recurringTask, occurrence,
            goal, goalEntry, dayRating, profile, mode, phase, tag, session, distraction,
        )
    }

    private data class Seeded(
        val habit: HabitEntity,
        val habitCompletion: HabitCompletionEntity,
        val quickHabit: QuickHabitEntity,
        val task: TaskEntity,
        val subtask: SubtaskEntity,
        val recurringTask: RecurringTaskEntity,
        val occurrence: RecurringOccurrenceEntity,
        val goal: GoalEntity,
        val goalEntry: GoalEntryEntity,
        val dayRating: DayRatingEntity,
        val profile: UserProfileEntity,
        val mode: FocusModeEntity,
        val phase: SessionPhaseEntity,
        val tag: FocusTagEntity,
        val session: FocusSessionEntity,
        val distraction: DistractionEntity,
    )
}

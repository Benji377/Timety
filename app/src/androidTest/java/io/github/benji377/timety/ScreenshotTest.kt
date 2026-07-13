package io.github.benji377.timety

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import io.github.benji377.timety.data.model.focus.FocusModeEntity
import io.github.benji377.timety.data.model.focus.FocusModeType
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.focus.FocusTagEntity
import io.github.benji377.timety.data.model.focus.FocusTargetType
import io.github.benji377.timety.data.model.focus.PhaseType
import io.github.benji377.timety.data.model.focus.SessionPhaseEntity
import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.task.Priority
import io.github.benji377.timety.data.model.task.SubtaskEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.task.TaskSize
import io.github.benji377.timety.data.model.user.UserProfileEntity
import io.github.benji377.timety.data.repository.ThemeMode
import io.github.benji377.timety.di.AppContainer
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.util.stats.ExperienceEngine
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    // Granted before the activity launches; otherwise MainScreen's permission prompt covers the
    // freshly launched activity (the test APK is reinstalled each run, revoking prior grants).
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS)

    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(permissionRule).around(composeTestRule)

    private val targetContext get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun str(resId: Int): String = targetContext.getString(resId)

    @Test
    fun generateScreenshots() {
        val container = (targetContext.applicationContext as TimetyApplication).container
        runBlocking {
            resetPersistentData(container)
            seedMockData(container)
        }

        // Wait until the seeded data has flowed into the Home screen.
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("Reply to client email", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()

        // 1. Home
        takeScreenshot("01_home_screen")

        // Statistics (bar chart icon in the Home app bar)
        composeTestRule.onNodeWithContentDescription(str(R.string.commonTooltipStats))
            .performClick()
        composeTestRule.waitForIdle()
        takeScreenshot("07_stats_screen")
        pressBack()

        // Calendar (calendar icon in the Home app bar)
        composeTestRule.onNodeWithContentDescription(str(R.string.commonTooltipCalendar))
            .performClick()
        composeTestRule.waitForIdle()
        takeScreenshot("08_calendar_screen")
        pressBack()

        // 2. Focus tab
        composeTestRule.onNodeWithText(str(R.string.navigationFocus)).performClick()
        composeTestRule.waitForIdle()
        takeScreenshot("02_focus_screen")

        // Focus modes (manage-modes icon in the Focus app bar)
        composeTestRule.onNodeWithContentDescription(str(R.string.focusModesTitle)).performClick()
        composeTestRule.waitForIdle()
        takeScreenshot("06_focus_modes_screen")
        pressBack()

        // 3. Tasks tab
        composeTestRule.onNodeWithText(str(R.string.navigationTasks)).performClick()
        composeTestRule.waitForIdle()
        takeScreenshot("03_tasks_screen")

        // Task detail (the overdue proposal task)
        composeTestRule.onNodeWithText("Finish project proposal").performClick()
        composeTestRule.waitForIdle()
        takeScreenshot("09_task_detail_screen")
        pressBack()

        // 4. Habits tab
        composeTestRule.onNodeWithText(str(R.string.navigationHabits)).performClick()
        composeTestRule.waitForIdle()
        takeScreenshot("04_habits_screen")

        // Habit detail (first habit of the morning stack)
        composeTestRule.onNodeWithText("MORNING ROUTINE").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Drink Water").performClick()
        composeTestRule.waitForIdle()
        takeScreenshot("10_habit_detail_screen")
        pressBack()

        // 5. Profile tab
        composeTestRule.onNodeWithText(str(R.string.navigationProfile)).performClick()
        composeTestRule.waitForIdle()
        takeScreenshot("05_profile_screen")
    }

    private fun pressBack() {
        composeTestRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()
    }

    private suspend fun resetPersistentData(container: AppContainer) {
        container.taskRepository.clearAll()
        container.habitRepository.clearAll()
        container.focusRepository.clearAll()
    }

    
    private suspend fun seedMockData(container: AppContainer) {
        val zone = ZoneId.systemDefault()
        val dayStart = LocalDate.now().atStartOfDay(zone)
        fun at(days: Long, hours: Long, minutes: Long = 0): Instant =
            dayStart.plusDays(days).plusHours(hours).plusMinutes(minutes).toInstant()

        container.settingsRepository.saveThemePref(ThemeMode.LIGHT)
        container.settingsRepository.saveDailyGoalMins(120)

        // --- Habits ---
        val habitColor = HabitColor.toArgb()
        val habits = container.habitRepository
        habits.insertHabit(
            HabitEntity(
                id = "habit_water",
                name = "Drink Water",
                frequency = HabitFrequency.DAILY,
                stackName = "Morning Routine",
                stackOrder = 1,
                createdAt = Instant.now(),
                colorValue = habitColor,
            )
        )
        habits.insertHabit(
            HabitEntity(
                id = "habit_meditate",
                name = "Meditation",
                frequency = HabitFrequency.DAILY,
                stackName = "Morning Routine",
                stackOrder = 2,
                createdAt = Instant.now(),
                colorValue = habitColor,
            )
        )
        habits.insertCompletion(
            HabitCompletionEntity(habitId = "habit_meditate", completionDate = at(0, 7, 15))
        )
        habits.insertHabit(
            HabitEntity(
                id = "habit_read",
                name = "Read 20 Pages",
                frequency = HabitFrequency.WEEKLY_EXACT,
                targetWeekdays = "[${LocalDate.now().dayOfWeek.value}]",
                notes = "Keep the momentum going before lunch.",
                createdAt = Instant.now(),
                colorValue = habitColor,
            )
        )
        habits.insertHabit(
            HabitEntity(
                id = "habit_workout",
                name = "Workout",
                frequency = HabitFrequency.WEEKLY_FLEXIBLE,
                targetDaysPerWeek = 3,
                createdAt = Instant.now(),
                colorValue = habitColor,
            )
        )
        for (daysAgo in 1L..3L) {
            habits.insertCompletion(
                HabitCompletionEntity(
                    habitId = "habit_workout",
                    completionDate = at(-daysAgo, 18)
                )
            )
        }

        // --- Tasks ---
        val tasks = container.taskRepository
        tasks.insertTask(
            TaskEntity(
                id = "task_proposal",
                title = "Finish project proposal",
                description = "Polish the scope, milestones, and budget notes.",
                dueDate = at(0, -3),
                location = "Home office",
                priority = Priority.HIGH,
                size = TaskSize.LARGE,
                category = "Work",
                createdAt = at(-2, 0),
            )
        )
        tasks.insertSubtask(
            SubtaskEntity(id = "task_proposal_1", taskId = "task_proposal", title = "Review outline")
        )
        tasks.insertSubtask(
            SubtaskEntity(
                id = "task_proposal_2",
                taskId = "task_proposal",
                title = "Check budget table"
            )
        )
        tasks.insertTask(
            TaskEntity(
                id = "task_client",
                title = "Reply to client email",
                description = "Answer the questions from yesterday's review.",
                dueDate = at(0, 23),
                location = "Inbox",
                priority = Priority.VERY_HIGH,
                size = TaskSize.SMALL,
                category = "Communication",
                createdAt = at(-1, 0),
                reminders = listOf(at(0, 9, 30)),
            )
        )
        tasks.insertTask(
            TaskEntity(
                id = "task_retro",
                title = "Prepare sprint retro",
                description = "Collect highlights and blockers for Friday.",
                dueDate = at(2, 0),
                location = "Meeting room",
                category = "Work",
                createdAt = at(0, 0),
            )
        )
        tasks.insertTask(
            TaskEntity(
                id = "task_backup",
                title = "Back up phone photos",
                description = "Archive the last trip and mark it complete.",
                dueDate = at(-1, 0),
                location = "Laptop",
                priority = Priority.LOW,
                size = TaskSize.SMALL,
                category = "Personal",
                isCompleted = true,
                completedAt = at(0, -20),
                createdAt = at(-4, 0),
            )
        )

        // --- Focus ---
        // The system modes are normally seeded lazily by FocusViewModel; the sessions below
        // reference them via a RESTRICT foreign key, so seed them here first.
        val focus = container.focusRepository
        focus.insertModeWithPhases(
            FocusModeEntity(
                id = FocusModeEntity.SYSTEM_FLEXIBLE_ID,
                name = "Flexible",
                type = FocusModeType.FLEXIBLE,
                isSystem = true
            ),
            listOf(
                SessionPhaseEntity(
                    modeId = FocusModeEntity.SYSTEM_FLEXIBLE_ID,
                    type = PhaseType.FOCUS,
                    durationMinutes = -1,
                    orderIndex = 0
                )
            ),
        )
        focus.insertModeWithPhases(
            FocusModeEntity(
                id = FocusModeEntity.SYSTEM_POMODORO_ID,
                name = "Pomodoro Classic",
                type = FocusModeType.POMODORO,
                isSystem = true
            ),
            buildList {
                repeat(4) { cycle ->
                    add(
                        SessionPhaseEntity(
                            modeId = FocusModeEntity.SYSTEM_POMODORO_ID,
                            type = PhaseType.FOCUS,
                            durationMinutes = 25,
                            orderIndex = cycle * 2
                        )
                    )
                    add(
                        SessionPhaseEntity(
                            modeId = FocusModeEntity.SYSTEM_POMODORO_ID,
                            type = PhaseType.REST,
                            durationMinutes = if (cycle == 3) 15 else 5,
                            orderIndex = cycle * 2 + 1
                        )
                    )
                }
            },
        )

        val deepWorkTagId = "tag_deep_work"
        focus.insertTag(
            FocusTagEntity(id = deepWorkTagId, name = "Deep Work", colorValue = 0xFF3F51B5.toInt())
        )

        fun pastSession(id: String, modeId: String, start: Instant, end: Instant) =
            FocusSessionEntity(
                id = id,
                modeId = modeId,
                startTime = start,
                endTime = end,
                totalSecondsFocused = (end.epochSecond - start.epochSecond).toInt(),
                isCompleted = true,
                tagId = deepWorkTagId,
                targetType = FocusTargetType.TAG,
                targetId = deepWorkTagId,
                targetLabel = "Deep Work",
            )
        focus.insertSession(
            pastSession("session_1", FocusModeEntity.SYSTEM_FLEXIBLE_ID, at(0, 8, 15), at(0, 9, 0))
        )
        focus.insertSession(
            pastSession("session_2", FocusModeEntity.SYSTEM_FLEXIBLE_ID, at(0, 13, 30), at(0, 14, 10))
        )
        focus.insertSession(
            pastSession("session_3", FocusModeEntity.SYSTEM_POMODORO_ID, at(-1, 16), at(-1, 16, 30))
        )

        // --- User profile ---
        // XP as if the seeded activity had been done in-app.
        val completedTasks = 1
        val habitCompletions = 4
        val focusMinutes = 45 + 40 + 30
        val seededXp = completedTasks * ExperienceEngine.XP_PER_TASK +
            habitCompletions * ExperienceEngine.XP_PER_HABIT +
            focusMinutes * ExperienceEngine.XP_PER_FOCUS_MINS
        container.userRepository.insertUserProfile(
            UserProfileEntity(
                name = "Bobert",
                accountCreated = Instant.now(),
                totalXp = seededXp
            )
        )
    }

    private fun takeScreenshot(name: String) {
        val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        // AGP's additionalTestOutputDir is pulled to build/outputs/ before the app (and with it
        // any app-private directory such as getExternalFilesDir) is uninstalled and wiped.
        val outputDir = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val dir = outputDir?.let { File(it) }
            ?: targetContext.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        val file = File(dir, "$name.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        println("Screenshot saved to: ${file.absolutePath}")
    }
}

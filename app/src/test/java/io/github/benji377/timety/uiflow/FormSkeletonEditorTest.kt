package io.github.benji377.timety.uiflow

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.form.FormSkeleton
import io.github.benji377.timety.data.model.form.HabitInput
import io.github.benji377.timety.data.model.form.TaskInput
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.task.Priority
import io.github.benji377.timety.data.model.task.RecurrenceUnit
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.SubtaskEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.task.TaskSize
import io.github.benji377.timety.data.model.task.TaskWindowUnit
import io.github.benji377.timety.testutil.TestAppContainer
import io.github.benji377.timety.testutil.TestViewModels
import io.github.benji377.timety.testutil.get
import io.github.benji377.timety.ui.components.task.TaskListTile
import io.github.benji377.timety.ui.screens.habit.HabitDetailScreen
import io.github.benji377.timety.ui.screens.settings.FormsSettings
import io.github.benji377.timety.ui.screens.task.RecurringTaskDetailScreen
import io.github.benji377.timety.ui.screens.task.TaskDetailScreen
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.LocalSnackbarHostState
import io.github.benji377.timety.ui.theme.TimetyTheme
import io.github.benji377.timety.ui.utils.AppUtils
import io.github.benji377.timety.ui.utils.LocalTaskSkeleton
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant

/**
 * Settings > Forms in the real editors: switched-off inputs are hidden while creating or editing,
 * unless the item already had a value when editing began; view mode is unchanged; a section whose
 * inputs are all hidden goes, header included.
 */
@RunWith(AndroidJUnit4::class)
// Tall enough that every form row is composed, so a missing node really means hidden.
@Config(qualifiers = "w412dp-h3000dp-xxhdpi")
class FormSkeletonEditorTest {
    @get:Rule
    val compose = createComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var container: TestAppContainer
    private lateinit var viewModels: TestViewModels
    private val settings get() = container.settingsRepository

    @Before
    fun setUp() {
        container = TestAppContainer(context)
        viewModels = TestViewModels(context.applicationContext as Application, container)
    }

    @After
    fun tearDown() {
        viewModels.clear()
        container.close()
    }

    private fun str(resId: Int) = context.getString(resId)
    private fun header(resId: Int) = str(resId).uppercase()

    private fun exists(text: String) =
        compose.onAllNodes(hasText(text), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun assertShown(vararg texts: String) {
        texts.forEach { text -> compose.waitUntil(TIMEOUT) { exists(text) } }
    }

    private fun assertHidden(vararg texts: String) {
        compose.waitForIdle()
        texts.forEach { assertFalse("\"$it\" should be hidden", exists(it)) }
    }

    /** Sets which inputs are on and waits until the screens' view model has them. */
    private fun skeleton(tasks: Set<TaskInput>? = null, habits: Set<HabitInput>? = null) = runBlocking {
        tasks?.let { on ->
            TaskInput.entries.forEach { settings.setTaskInputOn(it, it in on) }
        }
        habits?.let { on ->
            HabitInput.entries.forEach { settings.setHabitInputOn(it, it in on) }
        }
        val vm = viewModels.get<SettingsViewModel>()
        compose.waitUntil(TIMEOUT) {
            (tasks == null || vm.taskSkeleton.value.on == tasks) &&
                (habits == null || vm.habitSkeleton.value.on == habits)
        }
    }

    private fun allTasksOnExcept(vararg off: TaskInput) = TaskInput.entries.toSet() - off.toSet()
    private fun allHabitsOnExcept(vararg off: HabitInput) = HabitInput.entries.toSet() - off.toSet()

    private fun showTask(id: String?) = compose.setContent {
        AppFrame {
            TaskDetailScreen(
                taskId = id,
                onNavigateBack = {},
                taskViewModel = viewModels.get(),
                tagViewModel = viewModels.get(),
                settingsViewModel = viewModels.get(),
            )
        }
    }

    private fun showRecurring(id: String?) = compose.setContent {
        AppFrame {
            RecurringTaskDetailScreen(
                recurringTaskId = id,
                onNavigateBack = {},
                viewModel = viewModels.get(),
                tagViewModel = viewModels.get(),
                settingsViewModel = viewModels.get(),
            )
        }
    }

    private fun showHabit(id: String?) = compose.setContent {
        AppFrame {
            HabitDetailScreen(
                habitId = id,
                onNavigateBack = {},
                habitViewModel = viewModels.get(),
                tagViewModel = viewModels.get(),
                settingsViewModel = viewModels.get(),
            )
        }
    }

    /** The theme and snackbar host the app's main screen provides around every editor. */
    @Composable
    private fun AppFrame(content: @Composable () -> Unit) {
        TimetyTheme(darkTheme = false) {
            CompositionLocalProvider(LocalSnackbarHostState provides remember { SnackbarHostState() }, content = content)
        }
    }

    private fun startEditing(title: String) {
        assertShown(title)
        compose.onNodeWithContentDescription(str(R.string.commonLabelEdit)).performClick()
        compose.waitForIdle()
    }

    private fun save() {
        compose.onNodeWithContentDescription(str(R.string.commonLabelSave)).performClick()
        compose.waitForIdle()
    }

    private fun task(
        title: String = "Errand",
        description: String = "",
        location: String = "",
        priority: Priority = Priority.MEDIUM,
        due: Instant? = null,
        reminders: List<Instant> = emptyList(),
    ) = TaskEntity(
        id = "task",
        title = title,
        description = description,
        location = location,
        priority = priority,
        dueDate = due,
        reminders = reminders,
        createdAt = Instant.now(),
    ).also { runBlocking { container.taskRepository.insertTask(it) } }

    private fun habit(
        colorValue: Int = HabitColor.toArgb(),
        iconCodePoint: Int? = null,
        stackName: String? = null,
        timesPerDay: Int = 1,
    ) = HabitEntity(
        id = "habit",
        name = "Stretch",
        frequency = HabitFrequency.DAILY,
        createdAt = Instant.now(),
        colorValue = colorValue,
        iconCodePoint = iconCodePoint,
        stackName = stackName,
        stackOrder = stackName?.let { 0 },
        timesPerDay = timesPerDay,
    ).also { runBlocking { container.habitRepository.insertHabit(it) } }

    // Tasks.

    @Test
    fun newTask_withThePreset_hidesThePowerInputs() {
        skeleton(tasks = FormSkeleton.newInstallPreset())
        showTask(null)
        assertShown(
            str(R.string.taskDetailsLabelDescription),
            header(R.string.taskDetailsSectionScheduling),
            str(R.string.reminderFieldAdd),
            header(R.string.taskDetailsSectionChecklist),
        )
        assertHidden(header(R.string.taskDetailsSectionPriorityAndEffort), header(R.string.taskDetailsSectionLocation))
    }

    @Test
    fun newTask_afterAnUpdate_showsEveryInput() {
        // Nothing stored: an update from before Settings > Forms.
        showTask(null)
        assertShown(
            str(R.string.taskDetailsLabelDescription),
            header(R.string.taskDetailsSectionPriorityAndEffort),
            header(R.string.taskDetailsSectionScheduling),
            header(R.string.taskDetailsSectionLocation),
            header(R.string.taskDetailsSectionChecklist),
        )
    }

    @Test
    fun editingATask_keepsOffInputsThatHaveAValue() {
        skeleton(tasks = allTasksOnExcept(TaskInput.LOCATION, TaskInput.PRIORITY_EFFORT, TaskInput.DESCRIPTION))
        task(location = "Office", priority = Priority.HIGH, description = "")
        showTask("task")
        startEditing("Errand")
        assertShown(header(R.string.taskDetailsSectionLocation), header(R.string.taskDetailsSectionPriorityAndEffort))
        assertHidden(str(R.string.taskDetailsLabelDescription))
    }

    @Test
    fun editingATask_withDefaultPriority_hidesPriority() {
        skeleton(tasks = allTasksOnExcept(TaskInput.PRIORITY_EFFORT))
        task()
        showTask("task")
        startEditing("Errand")
        assertHidden(header(R.string.taskDetailsSectionPriorityAndEffort))
    }

    @Test
    fun clearingAnOffInput_keepsItUntilSaved_thenItsGone() {
        skeleton(tasks = allTasksOnExcept(TaskInput.DESCRIPTION))
        task(description = "Bring the receipt")
        showTask("task")
        startEditing("Errand")

        compose.onNode(hasSetTextAction() and hasText("Bring the receipt")).performTextClearance()
        // Still there while editing, so the field never vanishes under the user's fingers.
        assertShown(str(R.string.taskDetailsLabelDescription))

        save()
        startEditing("Errand")
        assertHidden(str(R.string.taskDetailsLabelDescription))
    }

    @Test
    fun viewMode_showsOffInputsThatHaveAValue() {
        skeleton(tasks = allTasksOnExcept(TaskInput.LOCATION, TaskInput.DESCRIPTION))
        task(location = "Office", description = "Bring the receipt")
        showTask("task")
        assertShown("Office", "Bring the receipt")
    }

    @Test
    fun viewMode_hidesOffInputsAtTheirDefault() {
        // Default priority and effort aren't a choice the user made, so they don't show either.
        skeleton(tasks = allTasksOnExcept(TaskInput.PRIORITY_EFFORT))
        task()
        showTask("task")
        assertShown("Errand")
        assertHidden(header(R.string.taskDetailsSectionPriorityAndEffort))
    }

    @Test
    fun taskTile_hidesPriorityAndEffortWhileOff_unlessTheTaskHasThem() {
        var shown by mutableStateOf(task(title = "Plain"))
        compose.setContent {
            AppFrame {
                CompositionLocalProvider(LocalTaskSkeleton provides FormSkeleton(allTasksOnExcept(TaskInput.PRIORITY_EFFORT))) {
                    TaskListTile(task = shown, onToggleCompleted = {}, onTap = {})
                }
            }
        }
        assertShown("Plain")
        assertHidden(AppUtils.getSizeEmoji(TaskSize.MEDIUM))

        shown = shown.copy(title = "Urgent", priority = Priority.HIGH)
        assertShown("Urgent", AppUtils.getSizeEmoji(TaskSize.MEDIUM))
    }

    @Test
    fun dueDateOff_hidesSchedulingAndReminders() {
        skeleton(tasks = allTasksOnExcept(TaskInput.DUE))
        showTask(null)
        assertHidden(header(R.string.taskDetailsSectionScheduling), str(R.string.reminderFieldAdd))
    }

    @Test
    fun remindersOff_hidesOnlyTheReminders() {
        skeleton(tasks = allTasksOnExcept(TaskInput.REMINDERS))
        showTask(null)
        assertShown(header(R.string.taskDetailsSectionScheduling), str(R.string.taskDetailsLabelDueDateSet))
        assertHidden(str(R.string.reminderFieldAdd))
    }

    @Test
    fun dueDateOff_onATaskWithADueDate_showsTheDateButNotEmptyReminders() {
        // Reminders count from the due date, so with its switch off they're off too.
        skeleton(tasks = allTasksOnExcept(TaskInput.DUE))
        task(due = Instant.now().plus(Duration.ofDays(2)))
        showTask("task")
        startEditing("Errand")
        assertShown(header(R.string.taskDetailsSectionScheduling))
        assertHidden(str(R.string.reminderFieldAdd))
    }

    @Test
    fun remindersOff_onATaskWithReminders_keepsThem() {
        skeleton(tasks = allTasksOnExcept(TaskInput.REMINDERS))
        val due = Instant.now().plus(Duration.ofDays(2))
        task(due = due, reminders = listOf(due.minus(Duration.ofHours(1))))
        showTask("task")
        startEditing("Errand")
        assertShown(str(R.string.reminderFieldLabel))
    }

    @Test
    fun checklistOff_keepsTheChecklistWhileEditingATaskThatHadSubtasks() {
        skeleton(tasks = allTasksOnExcept(TaskInput.CHECKLIST))
        task()
        runBlocking {
            container.taskRepository.insertSubtask(SubtaskEntity(id = "sub", taskId = "task", title = "Buy stamps"))
        }
        showTask("task")
        startEditing("Errand")
        assertShown(header(R.string.taskDetailsSectionChecklist), "Buy stamps")
    }

    // Recurring tasks.

    private fun recurring(windowUnit: TaskWindowUnit? = null) = RecurringTaskEntity(
        id = "recurring",
        title = "Water plants",
        dueDate = Instant.now().plus(Duration.ofDays(1)),
        unit = RecurrenceUnit.WEEK,
        windowAmount = windowUnit?.let { 2 },
        windowUnit = windowUnit,
        createdAt = Instant.now(),
    ).also { runBlocking { container.recurringTaskRepository.insertTask(it) } }

    @Test
    fun newRecurringTask_withThePreset_hidesTheWindowButKeepsTheRule() {
        skeleton(tasks = FormSkeleton.newInstallPreset())
        showRecurring(null)
        assertShown(header(R.string.taskDetailsSectionScheduling))
        assertHidden(header(R.string.recurringTaskWindowLabel), header(R.string.taskDetailsSectionPriorityAndEffort))
    }

    @Test
    fun recurringTask_withACustomWindow_keepsItWhileEditing() {
        skeleton(tasks = allTasksOnExcept(TaskInput.WINDOW))
        recurring(windowUnit = TaskWindowUnit.DAY)
        showRecurring("recurring")
        startEditing("Water plants")
        assertShown(header(R.string.recurringTaskWindowLabel))
    }

    @Test
    fun recurringTask_viewMode_hidesTheDefaultWindowWhileOff() {
        skeleton(tasks = allTasksOnExcept(TaskInput.WINDOW))
        recurring()
        showRecurring("recurring")
        assertShown("Water plants")
        assertHidden(header(R.string.recurringTaskWindowLabel))
    }

    @Test
    fun recurringTask_dueDateOff_stillShowsTheDueDate() {
        // The due date belongs to the repeat rule here; only the reminders follow the switch.
        skeleton(tasks = allTasksOnExcept(TaskInput.DUE))
        showRecurring(null)
        assertShown(header(R.string.taskDetailsSectionScheduling))
        assertHidden(str(R.string.reminderFieldAdd))
    }

    // Habits.

    @Test
    fun newHabit_withThePreset_hidesStackAndTimesPerDay() {
        skeleton(habits = FormSkeleton.newInstallPreset())
        showHabit(null)
        assertShown(header(R.string.detailSectionAppearance), str(R.string.detailLabelIcon), str(R.string.habitDetailLabelColor))
        assertHidden(str(R.string.habitDetailLabelStack), "Once a day")
    }

    @Test
    fun newHabit_iconAndColorOff_dropsTheAppearanceSection() {
        skeleton(habits = allHabitsOnExcept(HabitInput.ICON, HabitInput.COLOR))
        showHabit(null)
        assertHidden(header(R.string.detailSectionAppearance))
    }

    @Test
    fun habitWithACustomColor_keepsOnlyTheColorWhileEditing() {
        skeleton(habits = allHabitsOnExcept(HabitInput.ICON, HabitInput.COLOR))
        habit(colorValue = 0xFFE53935.toInt())
        showHabit("habit")
        startEditing("Stretch")
        assertShown(header(R.string.detailSectionAppearance), str(R.string.habitDetailLabelColor))
        assertHidden(str(R.string.detailLabelIcon))
    }

    @Test
    fun removingAnIcon_whileIconIsOff_hidesTheInputAfterSaving() {
        skeleton(habits = allHabitsOnExcept(HabitInput.ICON))
        habit(iconCodePoint = 3)
        showHabit("habit")
        startEditing("Stretch")

        compose.onNode(hasText(str(R.string.detailLabelIcon)) and hasClickAction()).performClick()
        compose.onNodeWithContentDescription(str(R.string.detailIconNone)).performClick()
        save()

        startEditing("Stretch")
        assertShown(str(R.string.habitDetailLabelColor))
        assertHidden(str(R.string.detailLabelIcon))
        assertTrue(runBlocking { container.habitRepository.allHabits.first().single().iconCodePoint == null })
    }

    @Test
    fun habitInAStack_keepsTheStackWhileEditing() {
        skeleton(habits = allHabitsOnExcept(HabitInput.STACK))
        habit(stackName = "Morning")
        showHabit("habit")
        startEditing("Stretch")
        assertShown(str(R.string.habitDetailLabelStack))
    }

    @Test
    fun habitWithSeveralCheckIns_keepsTimesPerDayWhileEditing() {
        skeleton(habits = allHabitsOnExcept(HabitInput.TIMES_PER_DAY))
        habit(timesPerDay = 3)
        showHabit("habit")
        startEditing("Stretch")
        assertShown("3 times a day")
    }

    // The Forms page.

    @Test
    fun formsPage_dueDateOff_greysOutTaskReminders() {
        skeleton(tasks = allTasksOnExcept(TaskInput.DUE))
        compose.setContent {
            AppFrame {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    FormsSettings(settingsViewModel = viewModels.get())
                }
            }
        }
        assertShown(str(R.string.settingsFormsRemindersNeedDue))
        compose.onNode(switchRow(R.string.settingsFormsReminders)).assertIsOff().assertIsNotEnabled()
        // The stored reminder switch is untouched: switching the due date back on restores it.
        assertTrue(runBlocking { settings.taskSkeletonFlow.first().isOn(TaskInput.REMINDERS) })
    }

    @Test
    fun formsPage_switchPersists() {
        compose.setContent {
            AppFrame {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    FormsSettings(settingsViewModel = viewModels.get())
                }
            }
        }
        compose.onNode(switchRow(R.string.taskDetailsSectionLocation)).assertIsOn().performClick()
        compose.waitForIdle()
        compose.onNode(switchRow(R.string.taskDetailsSectionLocation)).assertIsOff()
        assertFalse(runBlocking { settings.taskSkeletonFlow.first().isOn(TaskInput.LOCATION) })
    }

    /** A Forms page row: the whole row is the switch. */
    private fun switchRow(titleRes: Int) =
        hasText(str(titleRes)) and SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState)

    private companion object {
        const val TIMEOUT = 5_000L
    }
}

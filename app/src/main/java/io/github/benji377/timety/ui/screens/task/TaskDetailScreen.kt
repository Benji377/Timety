package io.github.benji377.timety.ui.screens.task

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.task.Priority
import io.github.benji377.timety.data.model.task.SubtaskEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.task.TaskSize
import io.github.benji377.timety.ui.components.common.BackNavigationIcon
import io.github.benji377.timety.ui.components.common.ConfirmationDialog
import io.github.benji377.timety.ui.components.common.DetailTopBarActions
import io.github.benji377.timety.ui.components.common.NeoDateTimePickerDialog
import io.github.benji377.timety.ui.components.common.NeoOutlinedTextField
import io.github.benji377.timety.ui.components.common.NeoTopBar
import io.github.benji377.timety.ui.components.task.CategoryPicker
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.utils.rememberFormSource
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import io.github.benji377.timety.util.task.CustomReminderIssue
import io.github.benji377.timety.util.task.ReminderUtils
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

/**
 * Shows a single task for viewing, creating, or editing: title, category, description, priority,
 * effort size, due date and reminders, location, and a subtask checklist. Pass a null [taskId] to
 * create a new task; an existing one opens in read-only mode with an edit toggle.
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
@Composable
fun TaskDetailScreen(
    taskId: String? = null,
    onNavigateBack: () -> Unit,
    taskViewModel: TaskViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val existingTaskWithSubtasks by remember(taskId) {
        taskId?.let { taskViewModel.observeTask(it) } ?: flowOf(null)
    }.collectAsState(initial = null)
    val allCategories by taskViewModel.allCategories.collectAsState()
    val isNewTask = taskId == null
    val existingTask = existingTaskWithSubtasks?.task

    var isEditing by rememberSaveable(taskId) { mutableStateOf(isNewTask) }
    // Fixed for the screen's lifetime so a repeated Save overwrites instead of duplicating.
    val newItemId = rememberSaveable { UUID.randomUUID().toString() }

    // Form state.
    val formSource = rememberFormSource(existingTaskWithSubtasks, isEditing)
    val formTask = formSource?.task
    var title by rememberSaveable(formTask) { mutableStateOf(existingTask?.title ?: "") }
    var description by rememberSaveable(formTask) { mutableStateOf(existingTask?.description ?: "") }
    var location by rememberSaveable(formTask) { mutableStateOf(existingTask?.location ?: "") }
    var place by rememberSaveable(formTask) { mutableStateOf(existingTask?.place) }
    var priority by rememberSaveable(formTask) {
        mutableStateOf(
            existingTask?.priority ?: Priority.MEDIUM
        )
    }
    var size by rememberSaveable(formTask) { mutableStateOf(existingTask?.size ?: TaskSize.MEDIUM) }
    var dueDate by rememberSaveable(formTask) { mutableStateOf(existingTask?.dueDate) }
    var category by rememberSaveable(formTask) { mutableStateOf(existingTask?.category ?: "") }
    var reminders by rememberSaveable(formTask) {
        mutableStateOf(
            existingTask?.reminders ?: emptyList()
        )
    }
    var subtasks by rememberSaveable(formSource) {
        mutableStateOf(
            existingTaskWithSubtasks?.subtasks ?: emptyList()
        )
    }

    var isAddingNewCategory by remember { mutableStateOf(false) }
    var newCategoryText by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val titleRequiredMsg = stringResource(R.string.taskDetailsSnackbarTitleRequired)
    val reminderTooLateMsg = stringResource(R.string.taskDetailsLabelReminderTooEarly)
    val reminderInPastMsg = stringResource(R.string.reminderCustomInPast)
    val remindersShiftedMsg = stringResource(R.string.reminderShifted)

    // Date-then-time picker flow; a non-null target keeps the dialog open.
    var pickerTarget by remember { mutableStateOf<PickerTarget?>(null) }

    fun addReminder(reminder: Instant) {
        if (reminder !in reminders) reminders = (reminders + reminder).sorted()
    }

    // Preset reminders follow the due date; see ReminderUtils.shiftWithDueDate.
    fun changeDueDate(newDue: Instant) {
        val shift = ReminderUtils.shiftWithDueDate(reminders, dueDate, newDue, Instant.now())
        dueDate = newDue
        reminders = shift.reminders
        if (shift.changed) scope.launch { snackbarHostState.showSnackbar(remindersShiftedMsg) }
    }

    // Presets lose their reference point without a due date.
    fun clearDueDate() {
        dueDate?.let { reminders = ReminderUtils.withoutPresets(reminders, it) }
        dueDate = null
    }

    val appBarTitle = when {
        isNewTask -> stringResource(R.string.taskDetailsTitleNew)
        isEditing -> stringResource(R.string.taskDetailsTitleEdit)
        else -> stringResource(R.string.taskDetailsTitleDetails)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            NeoTopBar(
                title = appBarTitle,
                navigationIcon = {
                    BackNavigationIcon(onClick = onNavigateBack)
                },
                actions = {
                    DetailTopBarActions(
                        isViewing = !isEditing && !isNewTask,
                        onDelete = { showDeleteConfirm = true },
                        onEdit = { isEditing = true },
                        onSave = {
                            val trimmedTitle = title.trim()
                            if (trimmedTitle.isEmpty()) {
                                scope.launch { snackbarHostState.showSnackbar(titleRequiredMsg) }
                                return@DetailTopBarActions
                            }
                            val taskToSave = TaskEntity(
                                id = taskId ?: newItemId,
                                title = trimmedTitle,
                                description = description.trim(),
                                location = location.trim(),
                                place = place,
                                priority = priority,
                                size = size,
                                dueDate = dueDate,
                                category = category,
                                reminders = reminders,
                                isCompleted = existingTask?.isCompleted ?: false,
                                completedAt = existingTask?.completedAt,
                                createdAt = existingTask?.createdAt ?: Instant.now(),
                            )
                            if (isNewTask) {
                                val subtasksToSave =
                                    subtasks.map { it.copy(taskId = taskToSave.id) }
                                taskViewModel.addTask(taskToSave, subtasksToSave)
                                onNavigateBack()
                            } else {
                                taskViewModel.updateTask(taskToSave)
                                isEditing = false
                            }
                        },
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = AppTheme.spaceLarge, vertical = AppTheme.spaceSmall)
        ) {
            // The basics: title, category, description.
            item {
                SectionHeader(
                    stringResource(R.string.taskDetailsSectionInfo),
                    Icons.Outlined.Info
                )
            }
            item {
                NeoOutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    enabled = isEditing,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.taskDetailsLabelTitle) + " *") },
                    leadingIcon = { Icon(Icons.Filled.Title, null) },
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(AppTheme.spaceLarge))

                CategoryPicker(
                    category = category,
                    onCategoryChange = { category = it },
                    isEditing = isEditing,
                    isAddingNewCategory = isAddingNewCategory,
                    onIsAddingNewCategoryChange = { isAddingNewCategory = it },
                    newCategoryText = newCategoryText,
                    onNewCategoryTextChange = { newCategoryText = it },
                    existingCategories = allCategories,
                    onCreateCategory = { taskViewModel.createCategory(it) }
                )

                Spacer(Modifier.height(AppTheme.spaceLarge))
                NeoOutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    enabled = isEditing,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.taskDetailsLabelDescription)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null) },
                    minLines = 3,
                    maxLines = 6
                )
            }

            // Priority and effort size.
            item {
                SectionHeader(
                    stringResource(R.string.taskDetailsSectionPriorityAndEffort),
                    Icons.Filled.BarChart
                )
            }
            item {
                PriorityAndEffortFields(
                    priority = priority,
                    size = size,
                    isEditing = isEditing,
                    onPriorityChange = { priority = it },
                    onSizeChange = { size = it },
                )
            }

            // Scheduling: due date and reminders.
            item {
                SectionHeader(
                    stringResource(R.string.taskDetailsSectionScheduling),
                    Icons.Filled.CalendarToday
                )
            }
            item {
                DueDateField(
                    dueDate = dueDate,
                    isEditing = isEditing,
                    onPick = { pickerTarget = PickerTarget.DUE_DATE },
                    onClear = ::clearDueDate,
                )
                Spacer(Modifier.height(AppTheme.spaceMedium))
                TaskReminderField(
                    reminders = reminders,
                    dueDate = dueDate,
                    isEditing = isEditing,
                    onRemindersChange = { reminders = it },
                    onAddCustom = { pickerTarget = PickerTarget.CUSTOM_REMINDER },
                )
            }

            // Location.
            item {
                SectionHeader(
                    stringResource(R.string.taskDetailsSectionLocation),
                    Icons.Outlined.LocationOn
                )
            }
            item {
                LocationField(
                    location = location,
                    place = place,
                    isEditing = isEditing,
                    onTextChange = {
                        location = it
                        // Typed text no longer describes the picked place, so drop its details.
                        place = null
                    },
                    onPlacePicked = {
                        location = it.name
                        place = it
                    },
                )
            }

            // Checklist / subtasks.
            item {
                SectionHeader(
                    stringResource(R.string.taskDetailsSectionChecklist),
                    Icons.Filled.Checklist
                )
            }
            items(subtasks, key = { it.id }) { subtask ->
                SubtaskRow(
                    subtask = subtask,
                    isEditing = isEditing,
                    onCheckedChange = { checked ->
                        val updated = subtask.copy(isCompleted = checked)
                        subtasks = subtasks.map { if (it.id == subtask.id) updated else it }
                        if (taskId != null) taskViewModel.updateSubtask(updated)
                    },
                    onRemove = {
                        subtasks = subtasks.filter { it.id != subtask.id }
                        if (taskId != null) taskViewModel.deleteSubtask(subtask)
                    },
                )
            }
            if (isEditing) {
                item {
                    NewSubtaskField(onAdd = { title ->
                        val newSubtask = SubtaskEntity(id = UUID.randomUUID().toString(), taskId = taskId ?: "", title = title)
                        subtasks = subtasks + newSubtask
                        if (taskId != null) taskViewModel.addSubtask(newSubtask)
                    })
                }
            }
            item { Spacer(Modifier.height(AppTheme.space3XLarge)) }
        }
    }

    // Delete confirmation dialog.
    ConfirmationDialog(
        visible = showDeleteConfirm,
        title = stringResource(R.string.taskDeleteTitle),
        content = stringResource(R.string.taskDeleteContent),
        confirmLabel = stringResource(R.string.commonLabelDelete),
        confirmColor = ErrorColor,
        onConfirm = {
            existingTask?.let { taskViewModel.deleteTask(it) }
            showDeleteConfirm = false
            onNavigateBack()
        },
        onDismiss = { showDeleteConfirm = false }
    )

    // Date and time picker dialog.
    pickerTarget?.let { target ->
        // Due dates can't be before today; a custom reminder must fall between today and the due date.
        val zone = ZoneId.systemDefault()
        val todayUtcMillis =
            LocalDate.now(zone).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val dueUtcMillis = dueDate?.atZone(zone)?.toLocalDate()
            ?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val selectableDates = remember(target, dueUtcMillis, todayUtcMillis) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = when (target) {
                    PickerTarget.DUE_DATE -> utcTimeMillis >= todayUtcMillis
                    PickerTarget.CUSTOM_REMINDER ->
                        utcTimeMillis >= todayUtcMillis &&
                                (dueUtcMillis == null || utcTimeMillis <= dueUtcMillis)
                }
            }
        }
        NeoDateTimePickerDialog(
            initialDate = (if (target == PickerTarget.DUE_DATE) dueDate else null) ?: Instant.now(),
            initialHour = 12,
            initialMinute = 0,
            timeTitle = stringResource(R.string.taskDetailsLabelDueDateSet),
            selectableDates = selectableDates,
            onConfirm = { date, hour, minute ->
                val instant = date.atTime(hour, minute).atZone(zone).toInstant()
                when (target) {
                    PickerTarget.DUE_DATE -> changeDueDate(instant)
                    PickerTarget.CUSTOM_REMINDER -> when (
                        ReminderUtils.customIssue(instant, dueDate, Instant.now())
                    ) {
                        null -> addReminder(instant)
                        CustomReminderIssue.IN_PAST ->
                            scope.launch { snackbarHostState.showSnackbar(reminderInPastMsg) }
                        CustomReminderIssue.AFTER_DUE ->
                            scope.launch { snackbarHostState.showSnackbar(reminderTooLateMsg) }
                    }
                }
                pickerTarget = null
            },
            onDismiss = { pickerTarget = null },
        )
    }
}

private enum class PickerTarget { DUE_DATE, CUSTOM_REMINDER }

package io.github.benji377.timety.ui.screens.task

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.focus.focusRequester
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
import io.github.benji377.timety.ui.components.common.DraggableReorderColumn
import io.github.benji377.timety.ui.components.common.FormSectionHeader
import io.github.benji377.timety.ui.components.common.NeoDatePickerDialog
import io.github.benji377.timety.ui.components.common.NeoDateTimePickerDialog
import io.github.benji377.timety.ui.components.common.NeoOutlinedTextField
import io.github.benji377.timety.ui.components.common.NeoTimePickerDialog
import io.github.benji377.timety.ui.components.common.NeoTopBar
import io.github.benji377.timety.ui.components.common.TagPicker
import io.github.benji377.timety.ui.components.common.rememberAutoFocus
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.utils.rememberFormSource
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.TagViewModel
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import io.github.benji377.timety.util.task.CustomReminderIssue
import io.github.benji377.timety.util.task.Due
import io.github.benji377.timety.util.task.ReminderUtils
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

/**
 * Shows a single task for viewing, creating, or editing: title, tags, description, priority,
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
    taskViewModel: TaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    tagViewModel: TagViewModel = activityScopedViewModel(),
) {
    val existingTaskWithSubtasks by remember(taskId) {
        taskId?.let { taskViewModel.observeTask(it) } ?: flowOf(null)
    }.collectAsState(initial = null)
    val taskTags by tagViewModel.taskTags.collectAsState()
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
    var hasDueTime by rememberSaveable(formTask) { mutableStateOf(existingTask?.hasDueTime ?: true) }
    val due = dueDate?.let { Due(it, hasDueTime) }
    // Null until the user changes them, so a save doesn't overwrite tags still loading.
    var pickedTagIds by rememberSaveable(formTask) { mutableStateOf<Set<String>?>(null) }
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

    var showDeleteConfirm by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val titleRequiredMsg = stringResource(R.string.taskDetailsSnackbarTitleRequired)
    val reminderTooLateMsg = stringResource(R.string.taskDetailsLabelReminderTooEarly)
    val reminderInPastMsg = stringResource(R.string.reminderCustomInPast)
    val remindersShiftedMsg = stringResource(R.string.reminderShifted)

    // A non-null target keeps its picker dialog open.
    var pickerTarget by remember { mutableStateOf<PickerTarget?>(null) }

    fun addReminder(reminder: Instant) {
        if (reminder !in reminders) reminders = (reminders + reminder).sorted()
    }

    // Preset reminders follow the due date; see ReminderUtils.shiftWithDueDate.
    fun changeDue(newDue: Due) {
        val shift = ReminderUtils.shiftWithDueDate(reminders, due, newDue, Instant.now())
        dueDate = newDue.at
        hasDueTime = newDue.hasTime
        reminders = shift.reminders
        if (shift.changed) scope.launch { snackbarHostState.showSnackbar(remindersShiftedMsg) }
    }

    // Presets lose their reference point without a due date.
    fun clearDueDate() {
        due?.let { reminders = ReminderUtils.withoutPresets(reminders, it) }
        dueDate = null
        hasDueTime = true
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
                                hasDueTime = hasDueTime,
                                reminders = reminders,
                                isCompleted = existingTask?.isCompleted ?: false,
                                completedAt = existingTask?.completedAt,
                                createdAt = existingTask?.createdAt ?: Instant.now(),
                            )
                            if (isNewTask) {
                                val subtasksToSave =
                                    subtasks.map { it.copy(taskId = taskToSave.id) }
                                taskViewModel.addTask(taskToSave, subtasksToSave, pickedTagIds.orEmpty())
                                onNavigateBack()
                            } else {
                                taskViewModel.updateTask(taskToSave, pickedTagIds)
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
            // The basics: title, tags, description.
            item {
                FormSectionHeader(
                    stringResource(R.string.taskDetailsSectionInfo),
                    Icons.Outlined.Info,
                    TaskColor,
                )
            }
            item {
                NeoOutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    enabled = isEditing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(rememberAutoFocus(isNewTask)),
                    label = { Text(stringResource(R.string.taskDetailsLabelTitle) + " *") },
                    leadingIcon = { Icon(Icons.Filled.Title, null) },
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                TagPicker(
                    selectedIds = pickedTagIds
                        ?: taskTags[taskId].orEmpty().mapTo(LinkedHashSet()) { it.id },
                    onSelectionChange = { pickedTagIds = it },
                    isEditing = isEditing,
                    modifier = Modifier.padding(top = AppTheme.spaceLarge),
                )
                // View mode hides empty optional fields.
                if (isEditing || description.isNotBlank()) {
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
            }

            // Priority and effort size.
            item {
                FormSectionHeader(
                    stringResource(R.string.taskDetailsSectionPriorityAndEffort),
                    Icons.Filled.BarChart,
                    TaskColor,
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

            // Scheduling: due date and reminders. Without a due date there is nothing to show
            // outside edit mode, since reminders need one.
            val showScheduling = isEditing || due != null
            if (showScheduling) item {
                FormSectionHeader(
                    stringResource(R.string.taskDetailsSectionScheduling),
                    Icons.Filled.CalendarToday,
                    TaskColor,
                )
            }
            if (showScheduling) item {
                DueDateTimeFields(
                    due = due,
                    isEditing = isEditing,
                    // "Set due date" invites a tap on an empty field; a filled one is just the due date.
                    dateLabel = stringResource(
                        if (due == null) R.string.taskDetailsLabelDueDateSet else R.string.taskDetailsLabelDueDate
                    ),
                    onPickDate = { pickerTarget = PickerTarget.DUE_DATE },
                    onPickTime = { pickerTarget = PickerTarget.DUE_TIME },
                    onClearTime = { due?.let { changeDue(Due.of(it.date(), null)) } },
                    onClearDate = ::clearDueDate,
                )
                // The reminder field hides itself in view mode when there are none.
                if (isEditing || reminders.isNotEmpty()) Spacer(Modifier.height(AppTheme.spaceLarge))
                TaskReminderField(
                    reminders = reminders,
                    due = due,
                    isEditing = isEditing,
                    onRemindersChange = { reminders = it },
                    onAddCustom = { pickerTarget = PickerTarget.CUSTOM_REMINDER },
                )
            }

            // Location.
            if (isEditing || location.isNotBlank()) item {
                FormSectionHeader(
                    stringResource(R.string.taskDetailsSectionLocation),
                    Icons.Outlined.LocationOn,
                    TaskColor,
                )
            }
            if (isEditing || location.isNotBlank()) item {
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

            // Checklist / subtasks; an empty checklist has nothing to show outside edit mode.
            if (isEditing || subtasks.isNotEmpty()) item {
                FormSectionHeader(
                    stringResource(R.string.taskDetailsSectionChecklist),
                    Icons.Filled.Checklist,
                    TaskColor,
                )
            }
            item {
                // Hold a row to drag it; a lone subtask has nothing to reorder.
                val canReorder = subtasks.size > 1
                DraggableReorderColumn(
                    items = subtasks,
                    key = { it.id },
                    isReorderEnabled = canReorder,
                    dragAfterLongPress = true,
                    onOrderChanged = { ordered ->
                        val renumbered = ordered.mapIndexed { index, subtask -> subtask.copy(sortOrder = index) }
                        if (renumbered != subtasks) {
                            subtasks = renumbered
                            if (taskId != null) taskViewModel.updateSubtasks(renumbered)
                        }
                    },
                ) { subtask, isDragged ->
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
                        showDragHandle = canReorder,
                        isDragged = isDragged,
                    )
                }
            }
            if (isEditing) {
                item {
                    NewSubtaskField(onAdd = { title ->
                        val newSubtask = SubtaskEntity(
                            id = UUID.randomUUID().toString(),
                            taskId = taskId ?: "",
                            title = title,
                            sortOrder = (subtasks.maxOfOrNull { it.sortOrder } ?: -1) + 1,
                        )
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

    // Date, time, and custom reminder pickers.
    pickerTarget?.let { target ->
        // Due dates can't be before today; a custom reminder must fall between today and the due date.
        val zone = ZoneId.systemDefault()
        val todayUtcMillis =
            LocalDate.now(zone).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val dueUtcMillis = due?.date(zone)?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val selectableDates = remember(target, dueUtcMillis, todayUtcMillis) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis >= todayUtcMillis &&
                            (target == PickerTarget.DUE_DATE || dueUtcMillis == null || utcTimeMillis <= dueUtcMillis)
            }
        }
        val dueTime = due?.takeIf { it.hasTime }?.at?.atZone(zone)
        when (target) {
            PickerTarget.DUE_DATE -> NeoDatePickerDialog(
                initialDate = dueDate ?: Instant.now(),
                selectableDates = selectableDates,
                onConfirm = { date ->
                    changeDue(Due.of(date, dueTime?.let { it.hour * 60 + it.minute }, zone))
                    pickerTarget = null
                },
                onDismiss = { pickerTarget = null },
            )

            PickerTarget.DUE_TIME -> NeoTimePickerDialog(
                initialHour = dueTime?.hour ?: DEFAULT_PICKER_HOUR,
                initialMinute = dueTime?.minute ?: 0,
                title = { Text(stringResource(R.string.taskDetailsLabelDueTime)) },
                onConfirm = { hour, minute ->
                    due?.let { changeDue(Due.of(it.date(zone), hour * 60 + minute, zone)) }
                    pickerTarget = null
                },
                onDismiss = { pickerTarget = null },
            )

            PickerTarget.CUSTOM_REMINDER -> NeoDateTimePickerDialog(
                initialDate = Instant.now(),
                initialHour = DEFAULT_PICKER_HOUR,
                initialMinute = 0,
                timeTitle = stringResource(R.string.taskDetailsReminderOptionCustom),
                selectableDates = selectableDates,
                onConfirm = { date, hour, minute ->
                    val instant = date.atTime(hour, minute).atZone(zone).toInstant()
                    when (ReminderUtils.customIssue(instant, due, Instant.now(), zone)) {
                        null -> addReminder(instant)
                        CustomReminderIssue.IN_PAST ->
                            scope.launch { snackbarHostState.showSnackbar(reminderInPastMsg) }
                        CustomReminderIssue.AFTER_DUE ->
                            scope.launch { snackbarHostState.showSnackbar(reminderTooLateMsg) }
                    }
                    pickerTarget = null
                },
                onDismiss = { pickerTarget = null },
            )
        }
    }
}

private const val DEFAULT_PICKER_HOUR = 12

private enum class PickerTarget { DUE_DATE, DUE_TIME, CUSTOM_REMINDER }

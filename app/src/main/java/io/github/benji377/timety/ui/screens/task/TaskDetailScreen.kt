package io.github.benji377.timety.ui.screens.task

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.LabelImportant
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.task.Priority
import io.github.benji377.timety.data.model.task.ReminderOption
import io.github.benji377.timety.data.model.task.SubtaskEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.task.TaskSize
import io.github.benji377.timety.ui.components.common.ConfirmationDialog
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.InfoColor
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.utils.AppUtils
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID
import io.github.benji377.timety.ui.components.common.TimetyButton as Button
import io.github.benji377.timety.ui.components.common.TimetyOutlinedTextField as OutlinedTextField


@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
@Composable
fun TaskDetailScreen(
    taskId: String? = null,
    onNavigateBack: () -> Unit,
    taskViewModel: TaskViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val allTasks by taskViewModel.allTasks.collectAsState()
    val dateFmt = io.github.benji377.timety.ui.utils.LocalDateFormatSettings.current
    val isNewTask = taskId == null
    val existingTaskWithSubtasks = taskId?.let { id -> allTasks.find { it.task.id == id } }
    val existingTask = existingTaskWithSubtasks?.task

    var isEditing by remember(taskId) { mutableStateOf(isNewTask) }

    // --- Form state (mirrors Flutter's controllers/state variables) ---
    var title by remember(existingTask) { mutableStateOf(existingTask?.title ?: "") }
    var description by remember(existingTask) { mutableStateOf(existingTask?.description ?: "") }
    var location by remember(existingTask) { mutableStateOf(existingTask?.location ?: "") }
    var priority by remember(existingTask) {
        mutableStateOf(
            existingTask?.priority ?: Priority.MEDIUM
        )
    }
    var size by remember(existingTask) { mutableStateOf(existingTask?.size ?: TaskSize.MEDIUM) }
    var dueDate by remember(existingTask) { mutableStateOf(existingTask?.dueDate) }
    var category by remember(existingTask) { mutableStateOf(existingTask?.category ?: "") }
    var reminders by remember(existingTask) {
        mutableStateOf(
            existingTask?.reminders ?: emptyList()
        )
    }
    var subtasks by remember(existingTaskWithSubtasks) {
        mutableStateOf(
            existingTaskWithSubtasks?.subtasks ?: emptyList()
        )
    }

    var showLocationPicker by remember { mutableStateOf(false) }
    var isAddingNewCategory by remember { mutableStateOf(false) }
    var newCategoryText by remember { mutableStateOf("") }
    var selectedReminderOption by remember { mutableStateOf(ReminderOption.MINUTES_30_BEFORE) }
    var newSubtaskTitle by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val titleRequiredMsg = stringResource(R.string.taskDetailsSnackbarTitleRequired)
    val reminderNoDueMsg = stringResource(R.string.taskDetailsSnackbarReminderNoDue)
    val reminderTooEarlyMsg = stringResource(R.string.taskDetailsLabelReminderTooEarly)

    // --- Date + time picker flow (mirrors AppDatePickers.pickDateTime: date, then time) ---
    var pickerTarget by remember { mutableStateOf<PickerTarget?>(null) }
    var pickerStep by remember { mutableIntStateOf(0) } // 0 = none, 1 = date, 2 = time
    var pickedLocalDate by remember { mutableStateOf<LocalDate?>(null) }

    fun closePicker() {
        pickerTarget = null
        pickerStep = 0
        pickedLocalDate = null
    }

    fun addComputedReminder(reminderTime: Instant?) {
        if (reminderTime == null) return
        if (!reminders.contains(reminderTime)) {
            reminders = (reminders + reminderTime).sorted()
        }
    }

    fun onAddReminderClicked() {
        when (selectedReminderOption) {
            ReminderOption.CUSTOM -> {
                pickerTarget = PickerTarget.CUSTOM_REMINDER
                pickerStep = 1
            }

            else -> {
                val due = dueDate
                if (due == null) {
                    scope.launch { snackbarHostState.showSnackbar(reminderNoDueMsg) }
                    return
                }
                val reminderTime = when (selectedReminderOption) {
                    ReminderOption.ON_TIME -> due
                    ReminderOption.MINUTES_30_BEFORE -> due.minus(30, ChronoUnit.MINUTES)
                    ReminderOption.HOUR_1_BEFORE -> due.minus(1, ChronoUnit.HOURS)
                    ReminderOption.DAY_1_BEFORE -> due.minus(1, ChronoUnit.DAYS)
                    ReminderOption.CUSTOM -> null
                }
                addComputedReminder(reminderTime)
            }
        }
    }

    val appBarTitle = when {
        isNewTask -> stringResource(R.string.taskDetailsTitleNew)
        isEditing -> stringResource(R.string.taskDetailsTitleEdit)
        else -> stringResource(R.string.taskDetailsTitleDetails)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = { Text(appBarTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (!isEditing && !isNewTask) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Filled.DeleteOutline,
                                contentDescription = stringResource(R.string.commonLabelDelete),
                                tint = ErrorColor
                            )
                        }
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                        }
                    } else {
                        IconButton(onClick = {
                            val trimmedTitle = title.trim()
                            if (trimmedTitle.isEmpty()) {
                                scope.launch { snackbarHostState.showSnackbar(titleRequiredMsg) }
                                return@IconButton
                            }
                            val taskToSave = TaskEntity(
                                id = taskId ?: UUID.randomUUID().toString(),
                                title = trimmedTitle,
                                description = description.trim(),
                                location = location.trim(),
                                priority = priority,
                                size = size,
                                dueDate = dueDate,
                                category = category,
                                reminders = reminders,
                                isCompleted = existingTask?.isCompleted ?: false,
                                completedAt = existingTask?.completedAt,
                                createdAt = existingTask?.createdAt ?: Instant.now()
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
                        }) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = stringResource(R.string.commonLabelSave)
                            )
                        }
                    }
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
            // --- SECTION: THE BASICS ---
            item {
                SectionHeader(
                    stringResource(R.string.taskDetailsSectionInfo),
                    Icons.Outlined.Info
                )
            }
            item {
                OutlinedTextField(
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
                    existingCategories = taskViewModel.getAllCategories()
                )

                Spacer(Modifier.height(AppTheme.spaceLarge))
                OutlinedTextField(
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

            // --- SECTION: PRIORITY & SIZE ---
            item {
                SectionHeader(
                    stringResource(R.string.taskDetailsSectionPriorityAndEffort),
                    Icons.Filled.BarChart
                )
            }
            item {
                Text(
                    stringResource(R.string.taskDetailsLabelPriority),
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(AppTheme.spaceSmall))
                AccordionSelector(
                    values = Priority.entries,
                    selectedValue = priority,
                    isEditing = isEditing,
                    onSelected = { priority = it },
                    iconBuilder = { p, isSelected ->
                        Box(modifier = Modifier.alpha(if (isEditing && isSelected) 1f else 0.5f)) {
                            AppUtils.PriorityIcon(priority = p)
                        }
                    },
                    labelBuilder = { p -> p.name.replace("_", "") }
                )

                Spacer(Modifier.height(AppTheme.spaceLarge))
                Text(stringResource(R.string.taskDetailsLabelEffort), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(AppTheme.spaceSmall))
                AccordionSelector(
                    values = TaskSize.entries,
                    selectedValue = size,
                    isEditing = isEditing,
                    onSelected = { size = it },
                    iconBuilder = { s, isSelected ->
                        Text(
                            text = AppUtils.getSizeEmoji(s),
                            fontSize = 16.sp,
                            color = if (!isEditing || !isSelected) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified
                        )
                    },
                    labelBuilder = { s -> s.name.replace("_", "") }
                )
            }

            // --- SECTION: TIME ---
            item {
                SectionHeader(
                    stringResource(R.string.taskDetailsSectionScheduling),
                    Icons.Filled.CalendarToday
                )
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isEditing) {
                            pickerTarget = PickerTarget.DUE_DATE
                            pickerStep = 1
                        }
                ) {
                    OutlinedTextField(
                        value = dueDate?.let {
                            stringResource(
                                R.string.taskDetailsLabelDueDate,
                                formatDate(it, dateFmt.dateFormatCode),
                                formatTime(it, dateFmt.use24HourFormat)
                            )
                        } ?: "",
                        onValueChange = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.taskDetailsLabelDueDateSet)) },
                        leadingIcon = { Icon(Icons.Filled.Event, null) },
                        trailingIcon = { if (isEditing) Icon(Icons.Filled.Edit, null) },
                        colors = disabledFieldColors(isEditing)
                    )
                }

                if (isEditing || reminders.isNotEmpty()) {
                    Spacer(Modifier.height(AppTheme.spaceMedium))
                    if (isEditing) {
                        ReminderInput(
                            selected = selectedReminderOption,
                            onSelectedChange = { selectedReminderOption = it },
                            onAdd = { onAddReminderClicked() }
                        )
                    }
                    Spacer(Modifier.height(AppTheme.spaceSmall))
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall)
                    ) {
                        reminders.forEach { reminder ->
                            InputChip(
                                selected = false,
                                onClick = { if (isEditing) reminders = reminders - reminder },
                                label = {
                                    Text(
                                        "${
                                            formatDate(
                                                reminder,
                                                dateFmt.dateFormatCode
                                            )
                                        } - ${formatTime(reminder, dateFmt.use24HourFormat)}",
                                        fontSize = AppTheme.fsBodySmall
                                    )
                                },
                                trailingIcon = if (isEditing) {
                                    {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = stringResource(R.string.commonLabelRemove),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }

            // --- SECTION: LOCATION ---
            item {
                SectionHeader(
                    stringResource(R.string.taskDetailsSectionLocation),
                    Icons.Outlined.LocationOn
                )
            }
            item {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    enabled = isEditing,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.taskDetailsLabelLocation)) },
                    leadingIcon = { Icon(Icons.Outlined.Map, null) },
                    trailingIcon = {
                        if (isEditing) {
                            IconButton(onClick = { showLocationPicker = true }) {
                                Icon(Icons.Filled.Search, contentDescription = null)
                            }
                        }
                    }
                )

                if (showLocationPicker) {
                    androidx.compose.ui.window.Dialog(
                        onDismissRequest = { showLocationPicker = false },
                        properties = androidx.compose.ui.window.DialogProperties(
                            usePlatformDefaultWidth = false
                        )
                    ) {
                        io.github.benji377.timety.ui.screens.location.LocationPickerScreen(
                            onLocationSelected = { selectedLocation ->
                                location = selectedLocation
                                showLocationPicker = false
                            },
                            onBack = { showLocationPicker = false }
                        )
                    }
                }
            }

            // --- SUBTASKS ---
            item {
                SectionHeader(
                    stringResource(R.string.taskDetailsSectionChecklist),
                    Icons.Filled.Checklist
                )
            }
            items(subtasks, key = { it.id }) { subtask ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = subtask.isCompleted,
                        onCheckedChange = { checked ->
                            val updated = subtask.copy(isCompleted = checked)
                            subtasks = subtasks.map { if (it.id == subtask.id) updated else it }
                            if (taskId != null) {
                                taskViewModel.updateSubtask(updated)
                            }
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = SuccessColor,
                            uncheckedColor = TaskColor,
                            checkmarkColor = Color.White
                        )
                    )
                    Text(
                        text = subtask.title,
                        modifier = Modifier.weight(1f),
                        textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else null,
                        color = if (subtask.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    if (isEditing) {
                        IconButton(onClick = {
                            subtasks = subtasks.filter { it.id != subtask.id }
                            if (taskId != null) {
                                taskViewModel.deleteSubtask(subtask)
                            }
                        }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = null,
                                tint = ErrorColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            item {
                if (isEditing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppTheme.spaceSmall)
                    ) {
                        Icon(
                            Icons.Filled.SubdirectoryArrowRight,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                        Spacer(Modifier.width(AppTheme.spaceMedium))
                        OutlinedTextField(
                            value = newSubtaskTitle,
                            onValueChange = { newSubtaskTitle = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(R.string.taskDetailsLabelSubtaskHint)) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                val trimmed = newSubtaskTitle.trim()
                                if (trimmed.isNotEmpty()) {
                                    val newSubtask = SubtaskEntity(
                                        id = UUID.randomUUID().toString(),
                                        taskId = taskId ?: "",
                                        title = trimmed
                                    )
                                    subtasks = subtasks + newSubtask
                                    if (taskId != null) taskViewModel.addSubtask(newSubtask)
                                    newSubtaskTitle = ""
                                }
                            }),
                            trailingIcon = {
                                IconButton(onClick = {
                                    val trimmed = newSubtaskTitle.trim()
                                    if (trimmed.isNotEmpty()) {
                                        val newSubtask = SubtaskEntity(
                                            id = UUID.randomUUID().toString(),
                                            taskId = taskId ?: "",
                                            title = trimmed
                                        )
                                        subtasks = subtasks + newSubtask
                                        if (taskId != null) taskViewModel.addSubtask(newSubtask)
                                        newSubtaskTitle = ""
                                    }
                                }) {
                                    Icon(
                                        Icons.Filled.AddCircle,
                                        contentDescription = stringResource(R.string.commonLabelAdd),
                                        tint = InfoColor
                                    )
                                }
                            }
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(AppTheme.space3XLarge)) }
        }
    }

    // --- Delete confirmation ---
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

    // --- Date + time picker dialogs (mirrors AppDatePickers.pickDateTime) ---
    if (pickerStep == 1) {
        // Mirror AppDatePickers.pickDateTime constraints: due dates can't be before today; a custom
        // reminder can't be after the task's due date.
        val zone = ZoneId.systemDefault()
        val todayUtcMillis =
            LocalDate.now(zone).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val dueUtcMillis = dueDate?.atZone(zone)?.toLocalDate()
            ?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val selectableDates = remember(pickerTarget, dueUtcMillis, todayUtcMillis) {
            object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = when (pickerTarget) {
                    PickerTarget.DUE_DATE -> utcTimeMillis >= todayUtcMillis
                    PickerTarget.CUSTOM_REMINDER -> dueUtcMillis == null || utcTimeMillis <= dueUtcMillis
                    else -> true
                }
            }
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (if (pickerTarget == PickerTarget.DUE_DATE) dueDate else null)
                ?.toEpochMilli() ?: System.currentTimeMillis(),
            selectableDates = selectableDates
        )
        DatePickerDialog(
            onDismissRequest = { closePicker() },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        pickedLocalDate =
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        pickerStep = 2
                    } else {
                        closePicker()
                    }
                }) { Text(stringResource(R.string.commonLabelConfirm)) }
            },
            dismissButton = {
                TextButton(onClick = { closePicker() }) { Text(stringResource(R.string.commonLabelCancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (pickerStep == 2 && pickedLocalDate != null) {
        val timePickerState = rememberTimePickerState(
            initialHour = 12,
            initialMinute = 0,
            is24Hour = dateFmt.use24HourFormat
        )
        AlertDialog(
            onDismissRequest = { closePicker() },
            title = { Text(stringResource(R.string.taskDetailsLabelDueDateSet)) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val date = pickedLocalDate!!
                    val instant = date.atTime(timePickerState.hour, timePickerState.minute)
                        .atZone(ZoneId.systemDefault()).toInstant()
                    when (pickerTarget) {
                        PickerTarget.DUE_DATE -> dueDate = instant
                        PickerTarget.CUSTOM_REMINDER -> {
                            val due = dueDate
                            if (due != null && instant.isAfter(due)) {
                                scope.launch { snackbarHostState.showSnackbar(reminderTooEarlyMsg) }
                            } else {
                                addComputedReminder(instant)
                            }
                        }

                        null -> {}
                    }
                    closePicker()
                }) { Text(stringResource(R.string.commonLabelConfirm)) }
            },
            dismissButton = {
                TextButton(onClick = { closePicker() }) { Text(stringResource(R.string.commonLabelCancel)) }
            }
        )
    }
}

private enum class PickerTarget { DUE_DATE, CUSTOM_REMINDER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun disabledFieldColors(isEditing: Boolean) = OutlinedTextFieldDefaults.colors(
    // While editing these click-through fields should look like regular enabled inputs;
    // in view mode they blend into the screen background like other disabled fields.
    disabledContainerColor = if (isEditing) MaterialTheme.colorScheme.surface else Color.Transparent,
    disabledTextColor = if (isEditing) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
    disabledBorderColor = if (isEditing) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(
        alpha = 0.6f
    ),
    disabledLeadingIconColor = if (isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    disabledTrailingIconColor = if (isEditing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant,
    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
)


private fun formatDate(instant: Instant, dateFormatCode: String): String =
    io.github.benji377.timety.util.datetime.AppDateFormatUtils.formatDate(instant, dateFormatCode)

private fun formatTime(instant: Instant, use24Hour: Boolean): String =
    io.github.benji377.timety.util.datetime.AppDateFormatUtils.formatTime(instant, use24Hour)

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = AppTheme.spaceXLarge, bottom = AppTheme.spaceMedium)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(AppTheme.iconSizeSmall)
        )
        Spacer(Modifier.width(AppTheme.spaceSmall))
        Text(
            text = title.uppercase(),
            fontSize = AppTheme.fsBodySmall,
            fontWeight = AppTheme.fwBold,
            letterSpacing = AppTheme.lsWide,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )
        Spacer(Modifier.width(AppTheme.spaceSmall))
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}


@Composable
private fun <T> AccordionSelector(
    values: List<T>,
    selectedValue: T,
    isEditing: Boolean,
    iconBuilder: @Composable (T, Boolean) -> Unit,
    labelBuilder: (T) -> String,
    onSelected: (T) -> Unit,
    activeBgColor: Color = TaskColor.copy(alpha = 0.15f),
    activeTextColor: Color = TaskColor,
) {
    val borderColor =
        if (isEditing) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(
            alpha = 0.3f
        )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (isEditing) MaterialTheme.colorScheme.surface else Color.Transparent)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
    ) {
        values.forEachIndexed { index, value ->
            val isSelected = value == selectedValue
            val weight by animateFloatAsState(
                targetValue = if (isSelected) 3f else 1f,
                label = "accordionSegment"
            )
            val isLast = index == values.lastIndex
            Row(
                modifier = Modifier
                    .weight(weight)
                    .fillMaxHeight()
                    .let { m ->
                        if (!isLast) {
                            m.drawBehind {
                                drawLine(
                                    color = borderColor,
                                    start = Offset(size.width, 0f),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        } else m
                    }
                    .background(
                        if (isSelected) {
                            if (isEditing) activeBgColor else MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.2f
                            )
                        } else Color.Transparent
                    )
                    .clickable(enabled = isEditing) { onSelected(value) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                iconBuilder(value, isSelected)
                AnimatedVisibility(visible = isSelected) {
                    Text(
                        text = labelBuilder(value),
                        modifier = Modifier.padding(start = 8.dp),
                        fontWeight = FontWeight.Bold,
                        color = if (isEditing) activeTextColor else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.38f
                        ),
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderInput(
    selected: ReminderOption,
    onSelectedChange: (ReminderOption) -> Unit,
    onAdd: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = reminderOptionLabel(selected),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.taskDetailsLabelReminderSet)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ReminderOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(reminderOptionLabel(option)) },
                        onClick = {
                            onSelectedChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
        Spacer(Modifier.width(AppTheme.spaceSmall))
        Button(onClick = onAdd) { Text(stringResource(R.string.commonLabelAdd)) }
    }
}

@Composable
private fun reminderOptionLabel(option: ReminderOption): String = when (option) {
    ReminderOption.ON_TIME -> stringResource(R.string.taskDetailsReminderOptionOnce)
    ReminderOption.MINUTES_30_BEFORE -> stringResource(R.string.taskDetailsReminderOptionHalfHour)
    ReminderOption.HOUR_1_BEFORE -> stringResource(R.string.taskDetailsReminderOptionHour)
    ReminderOption.DAY_1_BEFORE -> stringResource(R.string.taskDetailsReminderOptionDay)
    ReminderOption.CUSTOM -> stringResource(R.string.taskDetailsReminderOptionCustom)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPicker(
    category: String,
    onCategoryChange: (String) -> Unit,
    isEditing: Boolean,
    isAddingNewCategory: Boolean,
    onIsAddingNewCategoryChange: (Boolean) -> Unit,
    newCategoryText: String,
    onNewCategoryTextChange: (String) -> Unit,
    existingCategories: List<String>,
) {
    if (!isEditing) {
        OutlinedTextField(
            value = category.ifEmpty { stringResource(R.string.taskDetailsLabelCategoryEmpty) },
            onValueChange = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.taskDetailsLabelCategory)) },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, null) },
            colors = disabledFieldColors(isEditing = false)
        )
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (!isAddingNewCategory) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text(if (category.isEmpty()) stringResource(R.string.taskDetailsLabelCategorySelect) else category) },
                    label = { Text(stringResource(R.string.taskDetailsLabelCategory)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.taskDetailsLabelCategoryEmpty)) },
                        onClick = {
                            onCategoryChange("")
                            expanded = false
                        }
                    )
                    existingCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                onCategoryChange(cat)
                                expanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = null,
                                    tint = InfoColor,
                                    modifier = Modifier.size(AppTheme.iconSizeSmall)
                                )
                                Spacer(Modifier.width(AppTheme.spaceSmall))
                                Text(
                                    stringResource(R.string.taskDetailsLabelCategoryAddNew),
                                    color = InfoColor
                                )
                            }
                        },
                        onClick = {
                            onIsAddingNewCategoryChange(true)
                            expanded = false
                        }
                    )
                }
            }
        } else {
            OutlinedTextField(
                value = newCategoryText,
                onValueChange = onNewCategoryTextChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.taskDetailsLabelCategoryNewName)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.LabelImportant, null) },
                trailingIcon = {
                    Row {
                        IconButton(onClick = {
                            val trimmed = newCategoryText.trim()
                            if (trimmed.isNotEmpty()) {
                                onCategoryChange(trimmed)
                                onIsAddingNewCategoryChange(false)
                                onNewCategoryTextChange("")
                            }
                        }) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = stringResource(R.string.commonLabelConfirm),
                                tint = SuccessColor
                            )
                        }
                        IconButton(onClick = { onIsAddingNewCategoryChange(false) }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.commonLabelCancel),
                                tint = ErrorColor
                            )
                        }
                    }
                }
            )
        }
    }
}

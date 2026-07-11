package io.github.benji377.timety.ui.screens.task

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.task.MonthlyMode
import io.github.benji377.timety.data.model.task.RecurrenceUnit
import io.github.benji377.timety.data.model.task.RecurringOccurrenceEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.ReminderOption
import io.github.benji377.timety.ui.components.common.ConfirmationDialog
import io.github.benji377.timety.ui.components.common.TimetyDateTimePickerDialog
import io.github.benji377.timety.ui.components.common.TimetyTopBar
import io.github.benji377.timety.ui.components.task.CategoryPicker
import io.github.benji377.timety.ui.components.task.readOnlyFieldColors
import io.github.benji377.timety.ui.components.task.recurrenceOrdinalName
import io.github.benji377.timety.ui.components.task.recurrenceUnitName
import io.github.benji377.timety.ui.components.task.rememberRecurringCompleter
import io.github.benji377.timety.ui.components.task.weekdayShortName
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.RecurringTaskViewModel
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.habit.HabitUtils
import io.github.benji377.timety.util.task.RecurrenceUtils
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import io.github.benji377.timety.ui.components.common.TimetyButton as Button
import io.github.benji377.timety.ui.components.common.TimetyOutlinedTextField as OutlinedTextField

/** Which monthly anchoring the user picked; mapped to [MonthlyMode] plus ordinal on save. */
private enum class MonthlyChoice { DAY_OF_MONTH, NTH_WEEKDAY, LAST_WEEKDAY }

private enum class RecurringPickerTarget { DUE_DATE, PAST_OCCURRENCE }

/**
 * Creates, views, or edits a recurring task: title, description, next due date, the recurrence
 * rule (unit, interval, weekdays or monthly anchoring), and reminder offsets. In view mode it
 * additionally shows a habit-style completion history with manual backfill.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecurringTaskDetailScreen(
    recurringTaskId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: RecurringTaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    taskViewModel: TaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val allRecurring by viewModel.allRecurringTasks.collectAsState()
    val allCategories by taskViewModel.allCategories.collectAsState()
    val existing = recurringTaskId?.let { id -> allRecurring.find { it.task.id == id } }
    val existingTask = existing?.task
    val isNew = recurringTaskId == null
    val zone = ZoneId.systemDefault()
    val dateFmt = LocalDateFormatSettings.current

    var isEditing by remember(recurringTaskId) { mutableStateOf(isNew) }

    // Form state.
    var title by remember(existingTask) { mutableStateOf(existingTask?.title ?: "") }
    var description by remember(existingTask) { mutableStateOf(existingTask?.description ?: "") }
    var category by remember(existingTask) { mutableStateOf(existingTask?.category ?: "") }
    var isAddingNewCategory by remember { mutableStateOf(false) }
    var newCategoryText by remember { mutableStateOf("") }
    var dueDate by remember(existingTask) { mutableStateOf(existingTask?.dueDate) }
    var unit by remember(existingTask) { mutableStateOf(existingTask?.unit ?: RecurrenceUnit.WEEK) }
    var interval by remember(existingTask) { mutableIntStateOf(existingTask?.interval ?: 1) }
    var selectedWeekdays by remember(existingTask) {
        mutableStateOf(HabitUtils.parseWeekdays(existingTask?.daysOfWeek))
    }
    var monthlyChoice by remember(existingTask) {
        mutableStateOf(
            when {
                existingTask?.monthlyMode != MonthlyMode.NTH_WEEKDAY -> MonthlyChoice.DAY_OF_MONTH
                existingTask.monthlyOrdinal == RecurringTaskEntity.LAST_ORDINAL -> MonthlyChoice.LAST_WEEKDAY
                else -> MonthlyChoice.NTH_WEEKDAY
            }
        )
    }
    var reminderOffsets by remember(existingTask) {
        mutableStateOf(existingTask?.reminderOffsetsMinutes ?: emptyList())
    }
    var selectedReminderOption by remember { mutableStateOf(ReminderOption.MINUTES_30_BEFORE) }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    // Date-then-time picker flow; a non-null target keeps the dialog open.
    var pickerTarget by remember { mutableStateOf<RecurringPickerTarget?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val titleRequiredMsg = stringResource(R.string.taskDetailsSnackbarTitleRequired)
    val dueRequiredMsg = stringResource(R.string.recurringTaskSnackbarDueRequired)

    val dueLocalDate = dueDate?.atZone(zone)?.toLocalDate()
    // The nth-weekday options only exist for a concrete due date; a 5th occurrence of a weekday
    // is offered as "last" instead.
    val nthOrdinal = dueLocalDate?.let { RecurrenceUtils.ordinalInMonth(it) }?.takeIf { it <= 4 }
    val lastAvailable = dueLocalDate?.let { RecurrenceUtils.isLastWeekdayOfMonth(it) } ?: false

    fun save() {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            scope.launch { snackbarHostState.showSnackbar(titleRequiredMsg) }
            return
        }
        val due = dueDate
        val dueLocal = due?.atZone(zone)?.toLocalDate()
        if (due == null || dueLocal == null) {
            scope.launch { snackbarHostState.showSnackbar(dueRequiredMsg) }
            return
        }
        val effectiveChoice = when {
            monthlyChoice == MonthlyChoice.LAST_WEEKDAY && lastAvailable -> MonthlyChoice.LAST_WEEKDAY
            monthlyChoice == MonthlyChoice.NTH_WEEKDAY && nthOrdinal != null -> MonthlyChoice.NTH_WEEKDAY
            else -> MonthlyChoice.DAY_OF_MONTH
        }
        val taskToSave = RecurringTaskEntity(
            id = recurringTaskId ?: UUID.randomUUID().toString(),
            title = trimmedTitle,
            description = description.trim(),
            category = category,
            dueDate = due,
            unit = unit,
            interval = interval.coerceAtLeast(1),
            daysOfWeek = if (unit == RecurrenceUnit.WEEK) {
                HabitUtils.serializeWeekdays(
                    selectedWeekdays.ifEmpty { setOf(dueLocal.dayOfWeek.value) }
                )
            } else null,
            monthlyMode = if (unit == RecurrenceUnit.MONTH && effectiveChoice != MonthlyChoice.DAY_OF_MONTH) {
                MonthlyMode.NTH_WEEKDAY
            } else MonthlyMode.DAY_OF_MONTH,
            monthlyDay = if (unit == RecurrenceUnit.MONTH && effectiveChoice == MonthlyChoice.DAY_OF_MONTH) {
                dueLocal.dayOfMonth
            } else null,
            monthlyOrdinal = when {
                unit != RecurrenceUnit.MONTH -> null
                effectiveChoice == MonthlyChoice.NTH_WEEKDAY -> nthOrdinal
                effectiveChoice == MonthlyChoice.LAST_WEEKDAY -> RecurringTaskEntity.LAST_ORDINAL
                else -> null
            },
            monthlyWeekday = if (unit == RecurrenceUnit.MONTH && effectiveChoice != MonthlyChoice.DAY_OF_MONTH) {
                dueLocal.dayOfWeek.value
            } else null,
            reminderOffsetsMinutes = reminderOffsets,
            createdAt = existingTask?.createdAt ?: Instant.now(),
        )
        if (isNew) {
            viewModel.addTask(taskToSave)
            onNavigateBack()
        } else {
            viewModel.updateTask(taskToSave)
            isEditing = false
        }
    }

    val appBarTitle = when {
        isNew -> stringResource(R.string.recurringTaskTitleNew)
        isEditing -> stringResource(R.string.recurringTaskTitleEdit)
        else -> stringResource(R.string.recurringTaskTitleView)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TimetyTopBar(
                title = appBarTitle,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (!isEditing && !isNew) {
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
                        IconButton(onClick = { save() }) {
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
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(horizontal = AppTheme.spaceLarge, vertical = AppTheme.spaceSmall)
        ) {
            // Title and description.
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
                    existingCategories = allCategories,
                    onCreateCategory = { taskViewModel.createCategory(it) }
                )
                Spacer(Modifier.height(AppTheme.spaceLarge))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    enabled = isEditing,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.taskDetailsLabelDescription)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null) },
                    minLines = 2,
                    maxLines = 6
                )
                Spacer(Modifier.height(AppTheme.spaceLarge))
            }

            // Next due date (also the anchor for the recurrence rule and time of day).
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isEditing) {
                            pickerTarget = RecurringPickerTarget.DUE_DATE
                        }
                ) {
                    OutlinedTextField(
                        value = dueDate?.let {
                            "${AppDateFormatUtils.formatDate(it, dateFmt.dateFormatCode)} " +
                                AppDateFormatUtils.formatTime(it, dateFmt.use24HourFormat)
                        } ?: "",
                        onValueChange = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.recurringTaskLabelNextDue) + " *") },
                        leadingIcon = { Icon(Icons.Filled.Event, null) },
                        trailingIcon = { if (isEditing) Icon(Icons.Filled.Edit, null) },
                        colors = readOnlyFieldColors(isEditing)
                    )
                }
                Spacer(Modifier.height(AppTheme.spaceLarge))
            }

            // The recurrence rule.
            item {
                Text(stringResource(R.string.recurrenceLabel), fontWeight = AppTheme.fwBold)
                Spacer(Modifier.height(AppTheme.spaceSmall))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    RecurrenceUnit.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = unit == option,
                            onClick = { if (isEditing) unit = option },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = RecurrenceUnit.entries.size
                            ),
                            enabled = isEditing,
                        ) { Text(recurrenceUnitName(option)) }
                    }
                }
                Spacer(Modifier.height(AppTheme.spaceMedium))
                OutlinedTextField(
                    value = interval.toString(),
                    onValueChange = { new ->
                        if (new.length <= 2 && new.all(Char::isDigit)) {
                            interval = new.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        }
                    },
                    enabled = isEditing,
                    singleLine = true,
                    label = { Text(stringResource(R.string.recurrenceEveryLabel)) },
                    leadingIcon = { Icon(Icons.Filled.Repeat, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(160.dp)
                )

                if (unit == RecurrenceUnit.WEEK) {
                    Spacer(Modifier.height(AppTheme.spaceMedium))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall),
                        verticalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall),
                    ) {
                        (1..7).forEach { day ->
                            val isSelected = selectedWeekdays.contains(day)
                            FilterChip(
                                selected = isSelected,
                                enabled = isEditing,
                                onClick = {
                                    selectedWeekdays =
                                        if (isSelected) selectedWeekdays - day
                                        else selectedWeekdays + day
                                },
                                label = { Text(weekdayShortName(day)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TaskColor.copy(alpha = 0.3f),
                                ),
                            )
                        }
                    }
                }

                if (unit == RecurrenceUnit.MONTH && dueLocalDate != null) {
                    Spacer(Modifier.height(AppTheme.spaceMedium))
                    MonthlyChoiceOption(
                        label = stringResource(
                            R.string.recurringTaskMonthlyOnDay,
                            dueLocalDate.dayOfMonth
                        ),
                        selected = monthlyChoice == MonthlyChoice.DAY_OF_MONTH ||
                            (monthlyChoice == MonthlyChoice.NTH_WEEKDAY && nthOrdinal == null) ||
                            (monthlyChoice == MonthlyChoice.LAST_WEEKDAY && !lastAvailable),
                        enabled = isEditing,
                        onSelect = { monthlyChoice = MonthlyChoice.DAY_OF_MONTH },
                    )
                    if (nthOrdinal != null) {
                        MonthlyChoiceOption(
                            label = stringResource(
                                R.string.recurringTaskMonthlyOnNth,
                                recurrenceOrdinalName(nthOrdinal),
                                weekdayShortName(dueLocalDate.dayOfWeek.value)
                            ),
                            selected = monthlyChoice == MonthlyChoice.NTH_WEEKDAY,
                            enabled = isEditing,
                            onSelect = { monthlyChoice = MonthlyChoice.NTH_WEEKDAY },
                        )
                    }
                    if (lastAvailable) {
                        MonthlyChoiceOption(
                            label = stringResource(
                                R.string.recurringTaskMonthlyOnNth,
                                recurrenceOrdinalName(RecurringTaskEntity.LAST_ORDINAL),
                                weekdayShortName(dueLocalDate.dayOfWeek.value)
                            ),
                            selected = monthlyChoice == MonthlyChoice.LAST_WEEKDAY,
                            enabled = isEditing,
                            onSelect = { monthlyChoice = MonthlyChoice.LAST_WEEKDAY },
                        )
                    }
                }
                Spacer(Modifier.height(AppTheme.spaceLarge))
            }

            // Reminder offsets, re-applied to every occurrence.
            item {
                if (isEditing || reminderOffsets.isNotEmpty()) {
                    Text(
                        stringResource(R.string.taskDetailsLabelReminderSet),
                        fontWeight = AppTheme.fwBold
                    )
                    Spacer(Modifier.height(AppTheme.spaceSmall))
                }
                if (isEditing) {
                    ReminderOffsetInput(
                        selected = selectedReminderOption,
                        onSelectedChange = { selectedReminderOption = it },
                        onAdd = {
                            val minutes = reminderOptionOffsetMinutes(selectedReminderOption)
                            if (minutes != null && minutes !in reminderOffsets) {
                                reminderOffsets = (reminderOffsets + minutes).sorted()
                            }
                        },
                    )
                    Spacer(Modifier.height(AppTheme.spaceSmall))
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall)) {
                    reminderOffsets.forEach { minutes ->
                        InputChip(
                            selected = false,
                            onClick = {
                                if (isEditing) reminderOffsets = reminderOffsets - minutes
                            },
                            label = {
                                Text(offsetLabel(minutes), fontSize = AppTheme.fsBodySmall)
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
                Spacer(Modifier.height(AppTheme.spaceLarge))
            }

            // Completion history: view mode only, like a habit's past-occurrence log.
            if (!isNew && !isEditing && existing != null) {
                item {
                    val completeTask = rememberRecurringCompleter(viewModel, snackbarHostState)
                    Button(
                        onClick = { completeTask(existing.task) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.width(AppTheme.spaceSmall))
                        Text(stringResource(R.string.recurringTaskCompleteNow))
                    }
                    Spacer(Modifier.height(AppTheme.spaceLarge))
                    HorizontalDivider()
                    Spacer(Modifier.height(AppTheme.spaceLarge))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.recurringTaskHistoryTitle),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = AppTheme.fwBold,
                        )
                        TextButton(onClick = {
                            pickerTarget = RecurringPickerTarget.PAST_OCCURRENCE
                        }) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(AppTheme.iconSizeSmall)
                            )
                            Spacer(Modifier.width(AppTheme.spaceXSmall))
                            Text(stringResource(R.string.recurringTaskHistoryAddPast))
                        }
                    }
                    Spacer(Modifier.height(AppTheme.spaceSmall))
                }
                val occurrences = existing.occurrences.sortedByDescending { it.completedAt }
                if (occurrences.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.recurringTaskHistoryEmpty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(occurrences, key = { it.id }) { occurrence ->
                        OccurrenceRow(
                            occurrence = occurrence,
                            onDelete = { viewModel.deleteOccurrence(occurrence) },
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(AppTheme.space3XLarge)) }
        }
    }

    ConfirmationDialog(
        visible = showDeleteConfirm,
        title = stringResource(R.string.recurringTaskDeleteTitle),
        content = stringResource(R.string.recurringTaskDeleteContent),
        confirmLabel = stringResource(R.string.commonLabelDelete),
        confirmColor = ErrorColor,
        onConfirm = {
            existingTask?.let { viewModel.deleteTask(it) }
            showDeleteConfirm = false
            onNavigateBack()
        },
        onDismiss = { showDeleteConfirm = false }
    )

    // Date and time picker dialog (date first, then time).
    pickerTarget?.let { target ->
        // Due dates can't be in the past; backfilled occurrences can't be in the future.
        val todayUtcMillis =
            LocalDate.now(zone).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val selectableDates = remember(target, todayUtcMillis) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    when (target) {
                        RecurringPickerTarget.DUE_DATE -> utcTimeMillis >= todayUtcMillis
                        RecurringPickerTarget.PAST_OCCURRENCE -> utcTimeMillis <= todayUtcMillis
                    }
            }
        }
        val initialTime = dueDate?.atZone(zone)
        TimetyDateTimePickerDialog(
            initialDateMillis = (if (target == RecurringPickerTarget.DUE_DATE) dueDate else null)
                ?.toEpochMilli() ?: System.currentTimeMillis(),
            initialHour = initialTime?.hour ?: 12,
            initialMinute = initialTime?.minute ?: 0,
            timeTitle = stringResource(
                if (target == RecurringPickerTarget.DUE_DATE) R.string.recurringTaskLabelNextDue
                else R.string.recurringTaskHistoryAddPast
            ),
            selectableDates = selectableDates,
            onConfirm = { date, hour, minute ->
                val instant = date.atTime(hour, minute).atZone(zone).toInstant()
                when (target) {
                    RecurringPickerTarget.DUE_DATE -> dueDate = instant
                    RecurringPickerTarget.PAST_OCCURRENCE ->
                        recurringTaskId?.let { viewModel.addPastOccurrence(it, instant) }
                }
                pickerTarget = null
            },
            onDismiss = { pickerTarget = null },
        )
    }
}

@Composable
private fun MonthlyChoiceOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect, enabled = enabled)
        Text(label)
    }
}

@Composable
private fun OccurrenceRow(
    occurrence: RecurringOccurrenceEntity,
    onDelete: () -> Unit,
) {
    val dateFmt = LocalDateFormatSettings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = SuccessColor,
            modifier = Modifier.size(AppTheme.iconSizeSmall),
        )
        Spacer(Modifier.width(AppTheme.spaceMedium))
        Text(
            text = "${
                AppDateFormatUtils.formatDate(occurrence.completedAt, dateFmt.dateFormatCode)
            } - ${
                AppDateFormatUtils.formatTime(occurrence.completedAt, dateFmt.use24HourFormat)
            }",
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = stringResource(R.string.commonLabelRemove),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderOffsetInput(
    selected: ReminderOption,
    onSelectedChange: (ReminderOption) -> Unit,
    onAdd: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = reminderOffsetOptionLabel(selected),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.taskDetailsLabelReminderSet)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                // CUSTOM picks an absolute time on plain tasks; offsets are always relative here.
                ReminderOption.entries.filter { it != ReminderOption.CUSTOM }.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(reminderOffsetOptionLabel(option)) },
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

/** The relative offset in minutes an option stands for; null for [ReminderOption.CUSTOM]. */
private fun reminderOptionOffsetMinutes(option: ReminderOption): Int? = when (option) {
    ReminderOption.ON_TIME -> 0
    ReminderOption.MINUTES_30_BEFORE -> 30
    ReminderOption.HOUR_1_BEFORE -> 60
    ReminderOption.DAY_1_BEFORE -> 24 * 60
    ReminderOption.CUSTOM -> null
}

@Composable
private fun reminderOffsetOptionLabel(option: ReminderOption): String = when (option) {
    ReminderOption.ON_TIME -> stringResource(R.string.taskDetailsReminderOptionOnce)
    ReminderOption.MINUTES_30_BEFORE -> stringResource(R.string.taskDetailsReminderOptionHalfHour)
    ReminderOption.HOUR_1_BEFORE -> stringResource(R.string.taskDetailsReminderOptionHour)
    ReminderOption.DAY_1_BEFORE -> stringResource(R.string.taskDetailsReminderOptionDay)
    ReminderOption.CUSTOM -> stringResource(R.string.taskDetailsReminderOptionCustom)
}

/** The chip label for a stored offset; known values reuse the option labels. */
@Composable
private fun offsetLabel(minutes: Int): String = when (minutes) {
    0 -> stringResource(R.string.taskDetailsReminderOptionOnce)
    30 -> stringResource(R.string.taskDetailsReminderOptionHalfHour)
    60 -> stringResource(R.string.taskDetailsReminderOptionHour)
    24 * 60 -> stringResource(R.string.taskDetailsReminderOptionDay)
    else -> stringResource(R.string.recurringTaskReminderMinutesBefore, minutes)
}

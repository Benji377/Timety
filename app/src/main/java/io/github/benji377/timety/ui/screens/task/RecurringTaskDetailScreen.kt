package io.github.benji377.timety.ui.screens.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.task.MonthlyMode
import io.github.benji377.timety.data.model.task.Priority
import io.github.benji377.timety.data.model.task.RecurrenceUnit
import io.github.benji377.timety.data.model.task.OccurrenceStatus
import io.github.benji377.timety.data.model.task.RecurringOccurrenceEntity
import io.github.benji377.timety.data.model.task.RecurringSubtaskEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.TaskSize
import io.github.benji377.timety.data.model.task.TaskWindowUnit
import io.github.benji377.timety.ui.components.common.BackNavigationIcon
import io.github.benji377.timety.ui.components.common.ConfirmationDialog
import io.github.benji377.timety.ui.components.common.DraggableReorderColumn
import io.github.benji377.timety.ui.components.common.DetailTopBarActions
import io.github.benji377.timety.ui.components.common.FieldLabel
import io.github.benji377.timety.ui.components.common.FormSectionHeader
import io.github.benji377.timety.ui.components.common.NeoButton
import io.github.benji377.timety.ui.components.common.NeoDatePickerDialog
import io.github.benji377.timety.ui.components.common.NeoDateTimePickerDialog
import io.github.benji377.timety.ui.components.common.NeoIconButton
import io.github.benji377.timety.ui.components.common.NeoOutlinedTextField
import io.github.benji377.timety.ui.components.common.NeoSegmentedSelector
import io.github.benji377.timety.ui.components.common.NeoTextButton
import io.github.benji377.timety.ui.components.common.NeoTimePickerDialog
import io.github.benji377.timety.ui.components.common.NeoTopBar
import io.github.benji377.timety.ui.components.common.TagPicker
import io.github.benji377.timety.ui.components.common.WeekdayToggleRow
import io.github.benji377.timety.ui.components.common.rememberAutoFocus
import io.github.benji377.timety.ui.components.task.ReminderEntry
import io.github.benji377.timety.ui.components.task.ReminderField
import io.github.benji377.timety.ui.components.task.recurrenceOrdinalName
import io.github.benji377.timety.ui.components.task.recurrenceUnitName
import io.github.benji377.timety.ui.components.task.rememberRecurringCompleter
import io.github.benji377.timety.ui.components.task.reminderPresetLabel
import io.github.benji377.timety.ui.components.task.weekdayShortName
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.ui.theme.forText
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.ui.utils.quantityString
import io.github.benji377.timety.ui.utils.rememberFormSource
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.RecurringTaskViewModel
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import io.github.benji377.timety.ui.viewmodel.TagViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.habit.HabitUtils
import io.github.benji377.timety.util.task.Due
import io.github.benji377.timety.util.task.RecurrenceUtils
import io.github.benji377.timety.util.task.ReminderPreset
import io.github.benji377.timety.util.task.ReminderUtils
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.UUID

/** Which monthly anchoring the user picked; mapped to [MonthlyMode] plus ordinal on save. */
private enum class MonthlyChoice { DAY_OF_MONTH, NTH_WEEKDAY, LAST_WEEKDAY }

private enum class RecurringPickerTarget { DUE_DATE, DUE_TIME, PAST_OCCURRENCE }

/**
 * Creates, views, or edits a recurring task: title, description, priority and effort, next due
 * date, the recurrence rule (unit, interval, weekdays or monthly anchoring), reminder offsets, the
 * task window, location, and a checklist that unticks itself each round. In view mode it
 * additionally shows a habit-style completion history with manual backfill.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTaskDetailScreen(
    recurringTaskId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: RecurringTaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    tagViewModel: TagViewModel = activityScopedViewModel(),
    settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val existingState = remember(recurringTaskId) {
        recurringTaskId?.let { viewModel.observeTask(it) } ?: flowOf(null)
    }.collectAsState(initial = null)
    val existing = existingState.value
    val recurringTags by tagViewModel.recurringTaskTags.collectAsState()
    val defaultWindowDays by settingsViewModel.upcomingTasksHorizon.collectAsState()
    val existingTask = existing?.task
    val isNew = recurringTaskId == null
    val zone = ZoneId.systemDefault()
    val dateFmt = LocalDateFormatSettings.current

    var isEditing by rememberSaveable(recurringTaskId) { mutableStateOf(isNew) }
    // Fixed for the screen's lifetime so a repeated Save overwrites instead of duplicating.
    val newItemId = rememberSaveable { UUID.randomUUID().toString() }

    // Form state.
    val formTask = rememberFormSource(existingTask, isEditing)
    var title by rememberSaveable(formTask) { mutableStateOf(existingTask?.title ?: "") }
    var description by rememberSaveable(formTask) { mutableStateOf(existingTask?.description ?: "") }
    var priority by rememberSaveable(formTask) { mutableStateOf(existingTask?.priority ?: Priority.MEDIUM) }
    var size by rememberSaveable(formTask) { mutableStateOf(existingTask?.size ?: TaskSize.MEDIUM) }
    var location by rememberSaveable(formTask) { mutableStateOf(existingTask?.location ?: "") }
    var place by rememberSaveable(formTask) { mutableStateOf(existingTask?.place) }
    // Saved as the user changes it, like a one-off task's checklist; a new task's list is saved
    // with the task. Keyed on the whole loaded task, so a completion's untick shows right away.
    val formSource = rememberFormSource(existing, isEditing)
    var subtasks by rememberSaveable(formSource) { mutableStateOf(existing?.subtasks ?: emptyList()) }
    // Null until the user changes them, so a save doesn't overwrite tags still loading.
    var pickedTagIds by rememberSaveable(formTask) { mutableStateOf<Set<String>?>(null) }
    var dueDate by rememberSaveable(formTask) { mutableStateOf(existingTask?.dueDate) }
    // The picked time, kept apart from dueDate: a time inside a DST gap resolves to a later instant.
    var dueTimeMinutes by rememberSaveable(formTask) { mutableStateOf(existingTask?.timeOfDayMinutes) }
    var hasDueTime by rememberSaveable(formTask) { mutableStateOf(existingTask?.hasDueTime ?: false) }
    val due = dueDate?.let { Due(it, hasDueTime) }
    var unit by rememberSaveable(formTask) { mutableStateOf(existingTask?.unit ?: RecurrenceUnit.WEEK) }
    // Starts at 1, the cadence most people want; still required, so clearing it blocks saving.
    var intervalText by rememberSaveable(formTask) {
        mutableStateOf((existingTask?.interval ?: 1).toString())
    }
    var intervalError by rememberSaveable(formTask) { mutableStateOf(false) }
    var dueError by rememberSaveable(formTask) { mutableStateOf(false) }
    var selectedWeekdays by rememberSaveable(formTask) {
        mutableStateOf(HabitUtils.parseWeekdays(existingTask?.daysOfWeek))
    }
    var monthlyChoice by rememberSaveable(formTask) {
        mutableStateOf(
            when {
                existingTask?.monthlyMode != MonthlyMode.NTH_WEEKDAY -> MonthlyChoice.DAY_OF_MONTH
                existingTask.monthlyOrdinal == RecurringTaskEntity.LAST_ORDINAL -> MonthlyChoice.LAST_WEEKDAY
                else -> MonthlyChoice.NTH_WEEKDAY
            }
        )
    }
    var reminderOffsets by rememberSaveable(formTask) {
        mutableStateOf(existingTask?.reminderOffsetsMinutes ?: emptyList())
    }

    // Null = the global upcoming task window.
    var windowUnit by rememberSaveable(formTask) { mutableStateOf(existingTask?.windowUnit) }
    var windowAmountText by rememberSaveable(formTask) {
        mutableStateOf(existingTask?.windowAmount?.toString() ?: "")
    }
    var windowError by rememberSaveable(formTask) { mutableStateOf(false) }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    // A non-null target keeps its picker dialog open.
    var pickerTarget by remember { mutableStateOf<RecurringPickerTarget?>(null) }

    fun changeDue(date: LocalDate, timeMinutes: Int?) {
        dueError = false
        dueDate = Due.of(date, timeMinutes, zone).at
        dueTimeMinutes = timeMinutes
        hasDueTime = timeMinutes != null
        // A date-only due offers whole-day presets only.
        if (timeMinutes == null) {
            reminderOffsets = reminderOffsets.filter { minutes ->
                ReminderPreset.entries.none { it.minutes == minutes && !it.isWholeDays }
            }
            // Hours need a due time, so round them up to whole days.
            if (windowUnit == TaskWindowUnit.HOUR) {
                windowUnit = TaskWindowUnit.DAY
                windowAmountText = windowAmountText.toIntOrNull()
                    ?.let { ((it + HOURS_PER_DAY - 1) / HOURS_PER_DAY).coerceIn(1, TaskWindowUnit.DAY.maxAmount) }
                    ?.toString() ?: ""
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val titleRequiredMsg = stringResource(R.string.taskDetailsSnackbarTitleRequired)

    val dueLocalDate = dueDate?.atZone(zone)?.toLocalDate()
    // The nth-weekday options only exist for a concrete due date; a 5th occurrence of a weekday
    // is offered as "last" instead.
    val nthOrdinal = dueLocalDate?.let { RecurrenceUtils.ordinalInMonth(it) }?.takeIf { it <= 4 }
    val lastAvailable = dueLocalDate?.let { RecurrenceUtils.isLastWeekdayOfMonth(it) } ?: false

    fun save() {
        // Flagged up front so the field shows its error even when another check stops the save.
        val interval = intervalText.toIntOrNull()?.takeIf { it >= 1 }
        intervalError = interval == null
        val windowAmount = windowUnit?.let { unit -> windowAmountText.toIntOrNull()?.takeIf { it in unit.amounts } }
        windowError = windowUnit != null && windowAmount == null
        dueError = dueDate == null
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            scope.launch { snackbarHostState.showSnackbar(titleRequiredMsg) }
            return
        }
        val due = dueDate
        val dueLocal = due?.atZone(zone)?.toLocalDate()
        if (due == null || dueLocal == null) return
        if (interval == null || windowError) return
        val effectiveChoice = when {
            monthlyChoice == MonthlyChoice.LAST_WEEKDAY && lastAvailable -> MonthlyChoice.LAST_WEEKDAY
            monthlyChoice == MonthlyChoice.NTH_WEEKDAY && nthOrdinal != null -> MonthlyChoice.NTH_WEEKDAY
            else -> MonthlyChoice.DAY_OF_MONTH
        }
        val taskToSave = RecurringTaskEntity(
            id = recurringTaskId ?: newItemId,
            title = trimmedTitle,
            description = description.trim(),
            priority = priority,
            size = size,
            location = location.trim(),
            place = place,
            dueDate = due,
            hasDueTime = hasDueTime,
            timeOfDayMinutes = if (hasDueTime) {
                dueTimeMinutes ?: due.atZone(zone).let { it.hour * 60 + it.minute }
            } else null,
            unit = unit,
            interval = interval,
            daysOfWeek = if (unit == RecurrenceUnit.WEEK) {
                HabitUtils.serializeWeekdays(
                    selectedWeekdays.ifEmpty { setOf(dueLocal.dayOfWeek.value) }
                )
            } else null,
            monthlyMode = if (unit == RecurrenceUnit.MONTH && effectiveChoice != MonthlyChoice.DAY_OF_MONTH) {
                MonthlyMode.NTH_WEEKDAY
            } else MonthlyMode.DAY_OF_MONTH,
            monthlyDay = if (
                unit == RecurrenceUnit.YEAR ||
                (unit == RecurrenceUnit.MONTH && effectiveChoice == MonthlyChoice.DAY_OF_MONTH)
            ) {
                // A due date clamped to a short month (Feb 28 for "the 31st" or a yearly Feb 29)
                // must not become the rule.
                existingTask?.monthlyDay?.takeIf { existingTask.dueDate == due } ?: dueLocal.dayOfMonth
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
            windowAmount = windowAmount,
            windowUnit = windowUnit,
            createdAt = existingTask?.createdAt ?: Instant.now(),
        )
        if (isNew) {
            viewModel.addTask(taskToSave, subtasks.map { it.copy(recurringTaskId = taskToSave.id) }, pickedTagIds.orEmpty())
            onNavigateBack()
        } else {
            viewModel.updateTask(taskToSave, pickedTagIds)
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
            NeoTopBar(
                title = appBarTitle,
                navigationIcon = {
                    BackNavigationIcon(onClick = onNavigateBack)
                },
                actions = {
                    DetailTopBarActions(
                        isViewing = !isEditing && !isNew,
                        onDelete = { showDeleteConfirm = true },
                        onEdit = { isEditing = true },
                        onSave = { save() },
                    )
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
            // The basics: title, tags, description.
            item {
                FormSectionHeader(stringResource(R.string.taskDetailsSectionInfo), Icons.Outlined.Info, TaskColor)
            }
            item {
                NeoOutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    enabled = isEditing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(rememberAutoFocus(isNew)),
                    label = { Text(stringResource(R.string.taskDetailsLabelTitle) + " *") },
                    leadingIcon = { Icon(Icons.Filled.Title, null) },
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                TagPicker(
                    selectedIds = pickedTagIds
                        ?: recurringTags[recurringTaskId].orEmpty().mapTo(LinkedHashSet()) { it.id },
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

            // Priority and effort size, as on one-off tasks.
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

            // Next due date (also the anchor for the rule and time of day), rule and reminders.
            item {
                FormSectionHeader(
                    stringResource(R.string.taskDetailsSectionScheduling),
                    Icons.Filled.CalendarToday,
                    TaskColor,
                )
            }
            item {
                DueDateTimeFields(
                    due = due,
                    isEditing = isEditing,
                    dateLabel = stringResource(R.string.recurringTaskLabelNextDue) + " *",
                    onPickDate = { pickerTarget = RecurringPickerTarget.DUE_DATE },
                    onPickTime = { pickerTarget = RecurringPickerTarget.DUE_TIME },
                    onClearTime = { due?.let { changeDue(it.date(zone), null) } },
                    onClearDate = null,
                    dateError = if (dueError) stringResource(R.string.recurringTaskSnackbarDueRequired) else null,
                )
                Spacer(Modifier.height(AppTheme.spaceLarge))
            }

            item {
                FieldLabel(stringResource(R.string.recurrenceLabel))
                // labelBuilder isn't @Composable, so resolve the localized labels up front.
                val recurrenceUnitLabels =
                    RecurrenceUnit.entries.associateWith { recurrenceUnitName(it) }
                NeoSegmentedSelector(
                    values = RecurrenceUnit.entries,
                    selectedValue = unit,
                    isEditing = isEditing,
                    onSelected = { unit = it },
                    labelBuilder = { recurrenceUnitLabels.getValue(it) },
                    activeBgColor = TaskColor,
                )
                Spacer(Modifier.height(AppTheme.spaceLarge))
                NeoOutlinedTextField(
                    value = intervalText,
                    onValueChange = { new ->
                        if (new.length <= 3 && new.all(Char::isDigit)) {
                            intervalText = new
                            if ((new.toIntOrNull() ?: 0) >= 1) intervalError = false
                        }
                    },
                    enabled = isEditing,
                    singleLine = true,
                    label = { Text(stringResource(R.string.recurrenceEveryLabel) + " *") },
                    errorText = if (intervalError) stringResource(R.string.recurrenceEveryRequest) else null,
                    leadingIcon = { Icon(Icons.Filled.Repeat, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(160.dp)
                )

                if (unit == RecurrenceUnit.WEEK) {
                    Spacer(Modifier.height(AppTheme.spaceLarge))
                    WeekdayToggleRow(
                        selected = selectedWeekdays,
                        onToggle = { day ->
                            selectedWeekdays =
                                if (day in selectedWeekdays) selectedWeekdays - day
                                else selectedWeekdays + day
                        },
                        enabled = isEditing,
                        selectedColor = TaskColor,
                    )
                    // Allowed, since a one-off first date can be on purpose, but worth pointing out.
                    if (dueLocalDate != null && selectedWeekdays.isNotEmpty() &&
                        dueLocalDate.dayOfWeek.value !in selectedWeekdays
                    ) {
                        Text(
                            stringResource(
                                R.string.recurringTaskDueDayMismatch,
                                dueLocalDate.dayOfWeek.getDisplayName(TextStyle.FULL, LocalLocale.current.platformLocale),
                            ),
                            color = WarningColor.forText(),
                            fontSize = AppTheme.fsBodySmall,
                            modifier = Modifier.padding(top = AppTheme.spaceSmall),
                        )
                    }
                }

                if (unit == RecurrenceUnit.MONTH && dueLocalDate != null) {
                    Spacer(Modifier.height(AppTheme.spaceSmall))
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
            }

            // Reminder offsets, re-applied to every occurrence.
            item {
                // The reminder field hides itself in view mode when there are none.
                if (isEditing || reminderOffsets.isNotEmpty()) Spacer(Modifier.height(AppTheme.spaceLarge))
                val presets = due?.presets ?: ReminderPreset.entries
                val entries = reminderOffsets.sorted().map { minutes ->
                    val preset = presets.firstOrNull { it.minutes == minutes }
                    ReminderEntry(
                        key = minutes.toLong(),
                        preset = preset,
                        label = if (preset != null) reminderPresetLabel(preset, hasDueTime)
                        else stringResource(R.string.recurringTaskReminderMinutesBefore, minutes),
                    )
                }
                ReminderField(
                    entries = entries,
                    isEditing = isEditing,
                    sheetSubtitle = stringResource(R.string.reminderSheetEveryOccurrence),
                    warning = if (reminderOffsets.size >= ReminderUtils.MAX_REMINDERS) {
                        stringResource(R.string.reminderWarnLimit, ReminderUtils.MAX_REMINDERS)
                    } else null,
                    presets = presets,
                    hasTime = due?.hasTime ?: true,
                    canAddPreset = { reminderOffsets.size < ReminderUtils.MAX_REMINDERS },
                    onTogglePreset = { preset ->
                        reminderOffsets = if (preset.minutes in reminderOffsets) {
                            reminderOffsets - preset.minutes
                        } else (reminderOffsets + preset.minutes).sorted()
                    },
                    // Offsets outside the presets (from backups) stay removable.
                    onRemoveCustom = { entry ->
                        reminderOffsets = reminderOffsets - entry.key.toInt()
                    },
                    onAddCustom = null,
                )
            }

            // When the task starts showing in lists.
            item {
                FormSectionHeader(stringResource(R.string.recurringTaskWindowLabel), Icons.Filled.Visibility, TaskColor)
            }
            item {
                TaskWindowSection(
                    unit = windowUnit,
                    amountText = windowAmountText,
                    isError = windowError,
                    defaultDays = defaultWindowDays,
                    hasDueTime = hasDueTime,
                    isEditing = isEditing,
                    onUnitChange = { new ->
                        // Custom starts from the global value so it's valid right away.
                        if (windowUnit == null && new != null && windowAmountText.isEmpty()) {
                            windowAmountText = defaultWindowDays.toString()
                        }
                        windowUnit = new
                        windowError = false
                    },
                    onAmountChange = {
                        windowAmountText = it
                        windowError = false
                    },
                )
            }

            // Location; view mode hides it when empty.
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

            // Checklist; it unticks itself each time a round is completed. Empty, it has nothing
            // to show outside edit mode.
            if (isEditing || subtasks.isNotEmpty()) item {
                FormSectionHeader(
                    stringResource(R.string.taskDetailsSectionChecklist),
                    Icons.Filled.Checklist,
                    TaskColor,
                )
            }
            if (isEditing || subtasks.isNotEmpty()) item {
                // Hold a row to drag it; a lone item has nothing to reorder.
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
                            if (!isNew) viewModel.updateSubtasks(renumbered)
                        }
                    },
                ) { subtask, isDragged ->
                    SubtaskRow(
                        title = subtask.title,
                        isCompleted = subtask.isCompleted,
                        isEditing = isEditing,
                        onCheckedChange = { checked ->
                            val updated = subtask.copy(isCompleted = checked)
                            subtasks = subtasks.map { if (it.id == subtask.id) updated else it }
                            if (!isNew) viewModel.updateSubtask(updated)
                        },
                        onRemove = {
                            subtasks = subtasks.filter { it.id != subtask.id }
                            if (!isNew) viewModel.deleteSubtask(subtask)
                        },
                        showDragHandle = canReorder,
                        isDragged = isDragged,
                    )
                }
                if (isEditing) {
                    NewSubtaskField(onAdd = { title ->
                        val newSubtask = RecurringSubtaskEntity(
                            id = UUID.randomUUID().toString(),
                            recurringTaskId = recurringTaskId ?: newItemId,
                            title = title,
                            sortOrder = (subtasks.maxOfOrNull { it.sortOrder } ?: -1) + 1,
                        )
                        subtasks = subtasks + newSubtask
                        if (!isNew) viewModel.addSubtask(newSubtask)
                    })
                }
            }

            // Completion history: view mode only, like a habit's past-occurrence log.
            if (!isNew && !isEditing && existing != null) {
                item {
                    val completeTask = rememberRecurringCompleter(viewModel, snackbarHostState)
                    // Offered for any status: skipping a far-off round (a holiday) is legitimate.
                    val skipTask =
                        rememberRecurringCompleter(viewModel, snackbarHostState, OccurrenceStatus.SKIPPED)
                    Spacer(Modifier.height(AppTheme.spaceXLarge))
                    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceMedium)) {
                        NeoButton(
                            onClick = { skipTask(existing.task) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = null)
                            Spacer(Modifier.width(AppTheme.spaceSmall))
                            Text(stringResource(R.string.recurringTaskSkip))
                        }
                        NeoButton(
                            onClick = { completeTask(existing.task) },
                            modifier = Modifier.weight(2f),
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Spacer(Modifier.width(AppTheme.spaceSmall))
                            Text(stringResource(R.string.recurringTaskCompleteNow))
                        }
                    }
                }
                item {
                    FormSectionHeader(
                        stringResource(R.string.recurringTaskHistoryTitle),
                        Icons.Filled.History,
                        TaskColor,
                    ) {
                        NeoTextButton(onClick = {
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
                }
                val occurrences = existing.log.sortedByDescending { it.completedAt }
                val skippedCount = occurrences.count { it.status == OccurrenceStatus.SKIPPED }
                // Counts only once something was skipped; until then the list says it all.
                if (skippedCount > 0) item {
                    Text(
                        text = quantityString(
                            R.plurals.recurringTaskHistoryCompletedCount,
                            occurrences.size - skippedCount,
                            0,
                            occurrences.size - skippedCount,
                        ) + " | " + quantityString(
                            R.plurals.recurringTaskHistorySkippedCount,
                            skippedCount,
                            0,
                            skippedCount,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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

    // Date, time, and past-occurrence pickers.
    pickerTarget?.let { target ->
        // Due dates can't be in the past; backfilled occurrences can't be in the future.
        val todayUtcMillis =
            LocalDate.now(zone).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val selectableDates = remember(target, todayUtcMillis) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    if (target == RecurringPickerTarget.PAST_OCCURRENCE) utcTimeMillis <= todayUtcMillis
                    else utcTimeMillis >= todayUtcMillis
            }
        }
        val dueTime = if (hasDueTime) {
            dueTimeMinutes ?: dueDate?.atZone(zone)?.let { it.hour * 60 + it.minute }
        } else null
        when (target) {
            RecurringPickerTarget.DUE_DATE -> NeoDatePickerDialog(
                // A new weekly task starts on its first picked day rather than today.
                initialDate = dueDate ?: firstPickedDay(selectedWeekdays.takeIf { unit == RecurrenceUnit.WEEK })
                    .atStartOfDay(zone).toInstant(),
                selectableDates = selectableDates,
                onConfirm = { date ->
                    changeDue(date, dueTime)
                    pickerTarget = null
                },
                onDismiss = { pickerTarget = null },
            )

            RecurringPickerTarget.DUE_TIME -> NeoTimePickerDialog(
                initialHour = dueTime?.div(60) ?: DEFAULT_PICKER_HOUR,
                initialMinute = dueTime?.rem(60) ?: 0,
                title = { Text(stringResource(R.string.taskDetailsLabelDueTime)) },
                onConfirm = { hour, minute ->
                    due?.let { changeDue(it.date(zone), hour * 60 + minute) }
                    pickerTarget = null
                },
                onDismiss = { pickerTarget = null },
            )

            // The date picker allows today, so a later time today is clamped to now.
            RecurringPickerTarget.PAST_OCCURRENCE -> NeoDateTimePickerDialog(
                initialDate = Instant.now(),
                initialHour = DEFAULT_PICKER_HOUR,
                initialMinute = 0,
                timeTitle = stringResource(R.string.recurringTaskHistoryAddPast),
                selectableDates = selectableDates,
                onConfirm = { date, hour, minute ->
                    val instant = date.atTime(hour, minute).atZone(zone).toInstant()
                    recurringTaskId?.let { viewModel.addPastOccurrence(it, minOf(instant, Instant.now())) }
                    pickerTarget = null
                },
                onDismiss = { pickerTarget = null },
            )
        }
    }
}

private const val DEFAULT_PICKER_HOUR = 12
private const val HOURS_PER_DAY = 24

/**
 * Picks the task window: the global default, or a custom amount of hours, days or weeks before
 * the due date. Hours are offered only when the task has a due time.
 */
@Composable
private fun TaskWindowSection(
    unit: TaskWindowUnit?,
    amountText: String,
    isError: Boolean,
    defaultDays: Int,
    hasDueTime: Boolean,
    isEditing: Boolean,
    onUnitChange: (TaskWindowUnit?) -> Unit,
    onAmountChange: (String) -> Unit,
) {
    Text(
        stringResource(R.string.recurringTaskWindowHint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(AppTheme.spaceSmall))
    MonthlyChoiceOption(
        label = pluralStringResource(R.plurals.recurringTaskWindowDefault, defaultDays, defaultDays),
        selected = unit == null,
        enabled = isEditing,
        onSelect = { onUnitChange(null) },
    )
    if (isEditing && unit == null) {
        Text(
            stringResource(R.string.recurringTaskWindowDefaultHint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = RADIO_LABEL_INSET),
        )
    }
    MonthlyChoiceOption(
        label = stringResource(R.string.recurringTaskWindowCustom),
        selected = unit != null,
        enabled = isEditing,
        onSelect = { if (unit == null) onUnitChange(TaskWindowUnit.DAY) },
    )
    if (unit != null) {
        val units = if (hasDueTime) TaskWindowUnit.entries else TaskWindowUnit.entries - TaskWindowUnit.HOUR
        val unitLabels = mapOf(
            TaskWindowUnit.HOUR to stringResource(R.string.recurringTaskWindowHours),
            TaskWindowUnit.DAY to stringResource(R.string.recurringTaskWindowDays),
            TaskWindowUnit.WEEK to stringResource(R.string.recurringTaskWindowWeeks),
        )
        Spacer(Modifier.height(AppTheme.spaceSmall))
        Row(
            modifier = Modifier.padding(start = RADIO_LABEL_INSET),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceMedium),
        ) {
            NeoOutlinedTextField(
                value = amountText,
                onValueChange = { new -> if (new.length <= 2 && new.all(Char::isDigit)) onAmountChange(new) },
                enabled = isEditing,
                singleLine = true,
                label = { Text(stringResource(R.string.recurringTaskWindowAmount)) },
                errorText = if (isError) {
                    stringResource(R.string.recurringTaskWindowRequest, unit.minAmount, unit.maxAmount)
                } else null,
                supportingText = if (!isError && unit == TaskWindowUnit.DAY && amountText == "0") {
                    { Text(stringResource(R.string.recurringTaskWindowDueDayOnly)) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(120.dp),
            )
            NeoSegmentedSelector(
                values = units,
                selectedValue = unit,
                isEditing = isEditing,
                onSelected = { onUnitChange(it) },
                labelBuilder = { unitLabels.getValue(it) },
                activeBgColor = TaskColor,
                modifier = Modifier.weight(1f).padding(top = AppTheme.spaceSmall),
            )
        }
    }
}

/**
 * Lines helper text up with a radio option's label: the radio's drawn width (20dp dot plus its
 * 2dp padding on each side) and the gap before the label.
 */
private val RADIO_LABEL_INSET = 24.dp + AppTheme.spaceMedium

@Composable
private fun MonthlyChoiceOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    // The whole row takes the tap, so the radio needs no touch padding of its own and lines up
    // with the fields above it.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LocalMinimumInteractiveComponentSize.current)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceMedium),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled,
            // The selection is the value: keep it readable in view mode instead of the 38%
            // Material disabled fade; the label text next to it never fades either.
            colors = RadioButtonDefaults.colors(
                disabledSelectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledUnselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
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
        val skipped = occurrence.status == OccurrenceStatus.SKIPPED
        Icon(
            imageVector = if (skipped) Icons.AutoMirrored.Filled.Redo else Icons.Filled.Check,
            contentDescription = if (skipped) stringResource(R.string.recurringTaskHistorySkipped) else null,
            tint = if (skipped) MaterialTheme.colorScheme.onSurfaceVariant else SuccessColor,
            modifier = Modifier.size(AppTheme.iconSizeSmall),
        )
        Spacer(Modifier.width(AppTheme.spaceMedium))
        val done = occurrence.subtasksDone
        val total = occurrence.subtasksTotal
        val muted = MaterialTheme.colorScheme.onSurfaceVariant
        Text(
            text = buildAnnotatedString {
                append(AppDateFormatUtils.formatDate(occurrence.completedAt, dateFmt.dateFormatCode))
                append(" - ")
                append(AppDateFormatUtils.formatTime(occurrence.completedAt, dateFmt.use24HourFormat))
                // How much of the checklist was ticked in that round, kept quiet next to the date.
                if (done != null && total != null) {
                    append(" | ")
                    withStyle(SpanStyle(color = muted)) { append("$done/$total") }
                }
            },
            modifier = Modifier.weight(1f),
        )
        NeoIconButton(
            onClick = onDelete,
            icon = Icons.Filled.DeleteOutline,
            contentDescription = stringResource(R.string.commonLabelRemove),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Today, or the first of [weekdays] (ISO numbers) from today on when any are given. */
private fun firstPickedDay(weekdays: Set<Int>?): LocalDate {
    val today = LocalDate.now()
    if (weekdays.isNullOrEmpty()) return today
    return generateSequence(today) { it.plusDays(1) }.first { it.dayOfWeek.value in weekdays }
}

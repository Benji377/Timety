package io.github.benji377.timety.ui.screens.habit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.form.HabitInput
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.ui.components.common.AppearanceFields
import io.github.benji377.timety.ui.components.common.BackNavigationIcon
import io.github.benji377.timety.ui.components.common.ColorPickerDialog
import io.github.benji377.timety.ui.components.common.ConfirmationDialog
import io.github.benji377.timety.ui.components.common.DetailTopBarActions
import io.github.benji377.timety.ui.components.common.FieldLabel
import io.github.benji377.timety.ui.components.common.FormSectionHeader
import io.github.benji377.timety.ui.components.common.IconPickerDialog
import io.github.benji377.timety.ui.components.common.NeoCard
import io.github.benji377.timety.ui.components.common.NeoIconButton
import io.github.benji377.timety.ui.components.common.NeoOutlinedTextField
import io.github.benji377.timety.ui.components.common.NeoSegmentedSelector
import io.github.benji377.timety.ui.components.common.NeoSlider
import io.github.benji377.timety.ui.components.common.NeoTimePickerDialog
import io.github.benji377.timety.ui.components.common.NeoTopBar
import io.github.benji377.timety.ui.components.common.TagPicker
import io.github.benji377.timety.ui.components.common.TextInputDialog
import io.github.benji377.timety.ui.components.common.WeekdayToggleRow
import io.github.benji377.timety.ui.components.common.rememberAutoFocus
import io.github.benji377.timety.ui.components.habit.HabitHistorySheet
import io.github.benji377.timety.ui.components.habit.HabitTodayCard
import io.github.benji377.timety.ui.components.task.ReminderEntry
import io.github.benji377.timety.ui.components.task.ReminderField
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.PickerPalette
import io.github.benji377.timety.ui.utils.ColorSaver
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.ui.utils.quantityString
import io.github.benji377.timety.ui.utils.rememberFormSource
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.HabitViewModel
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import io.github.benji377.timety.ui.viewmodel.TagViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.habit.HabitUtils
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

/**
 * Creates, views, or edits a habit. With a null [habitId] the screen starts in creation mode;
 * otherwise it opens in read-only view mode until the user taps edit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    habitId: String? = null,
    onNavigateBack: () -> Unit,
    habitViewModel: HabitViewModel = viewModel(factory = AppViewModelProvider.Factory),
    tagViewModel: TagViewModel = activityScopedViewModel(),
    settingsViewModel: SettingsViewModel = activityScopedViewModel(),
) {
    val allHabits by habitViewModel.allHabits.collectAsState()
    val habitTags by tagViewModel.habitTags.collectAsState()
    val existingHabit = habitId?.let { id -> allHabits.find { it.id == id } }
    val habitsWithCompletions by habitViewModel.habitsWithCompletions.collectAsState()
    val existingWithCompletions = habitId?.let { id -> habitsWithCompletions.find { it.habit.id == id } }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    val isNewHabit = habitId == null

    var isEditing by rememberSaveable { mutableStateOf(isNewHabit) }
    // Fixed for the screen's lifetime so a repeated Save overwrites instead of duplicating.
    val newItemId = rememberSaveable { UUID.randomUUID().toString() }

    // Form state.
    val formHabit = rememberFormSource(existingHabit, isEditing)
    var name by rememberSaveable(formHabit) { mutableStateOf(existingHabit?.name ?: "") }
    var nameError by remember { mutableStateOf(false) }
    var notes by rememberSaveable(formHabit) { mutableStateOf(existingHabit?.notes ?: "") }
    var stackName by rememberSaveable(formHabit) { mutableStateOf(existingHabit?.stackName ?: "") }
    // Null until the user changes them, so a save doesn't overwrite tags still loading.
    var pickedTagIds by rememberSaveable(formHabit) { mutableStateOf<Set<String>?>(null) }

    var frequency by rememberSaveable(formHabit) {
        mutableStateOf(
            existingHabit?.frequency ?: HabitFrequency.DAILY
        )
    }
    var targetCount by rememberSaveable(formHabit) {
        mutableIntStateOf(
            existingHabit?.targetCount ?: 3
        )
    }
    var timesPerDay by rememberSaveable(formHabit) {
        mutableIntStateOf(existingHabit?.timesPerDay ?: 1)
    }
    // A weekly target can't exceed the days in a week, however the frequency got there.
    fun selectFrequency(newFrequency: HabitFrequency) {
        frequency = newFrequency
        if (newFrequency == HabitFrequency.WEEKLY_FLEXIBLE) {
            targetCount = targetCount.coerceAtMost(HabitUtils.MAX_TARGET_PER_WEEK)
        }
    }
    var selectedWeekdays by rememberSaveable(formHabit) {
        mutableStateOf(
            existingHabit?.targetWeekdays?.let { HabitUtils.parseWeekdays(it) }
                ?.takeIf { it.isNotEmpty() }
                ?: setOf(1, 3, 5)
        )
    }

    var selectedIconIndex by rememberSaveable(formHabit) { mutableStateOf(existingHabit?.iconCodePoint) }
    var selectedColor by rememberSaveable(formHabit, stateSaver = ColorSaver) {
        mutableStateOf(existingHabit?.colorValue?.let { Color(it) } ?: HabitColor)
    }
    var reminderTimes by rememberSaveable(formHabit) {
        mutableStateOf(HabitUtils.parseReminderTimes(existingHabit?.reminderTimes))
    }

    // Settings > Forms: an input shows when it's on or the habit has a value for it, checked against
    // the habit as it was when editing began, so clearing a field mid-edit doesn't hide it. View
    // mode follows the same rule, so an input that's off doesn't show up with its default.
    val skeleton by settingsViewModel.habitSkeleton.collectAsState()
    fun shows(input: HabitInput, hasValue: Boolean) = skeleton.shows(input, hasValue)
    val showsTags = shows(HabitInput.TAGS, !habitTags[habitId].isNullOrEmpty())
    val showsNotes = shows(HabitInput.NOTES, !formHabit?.notes.isNullOrBlank())
    val showsStack = shows(HabitInput.STACK, !formHabit?.stackName.isNullOrBlank())
    val showsIcon = shows(HabitInput.ICON, formHabit?.iconCodePoint != null)
    // Purple is the default, so only another colour counts as a value.
    val showsColor = shows(HabitInput.COLOR, formHabit != null && formHabit.colorValue != HabitColor.toArgb())
    val showsAppearance = showsIcon || showsColor
    val showsTimesPerDay = shows(HabitInput.TIMES_PER_DAY, (formHabit?.timesPerDay ?: 1) > 1)
    val showsReminders = shows(
        HabitInput.REMINDERS,
        HabitUtils.parseReminderTimes(formHabit?.reminderTimes).isNotEmpty(),
    )

    var showIconPicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var stackNameExpanded by remember { mutableStateOf(false) }
    var showNewStackDialog by remember { mutableStateOf(false) }
    val existingStacks = remember(allHabits) { HabitUtils.stackNamesInOrder(allHabits) }
    // A stack just created with "+" has no habits yet, so it is added to the options by hand.
    val stackOptions = remember(existingStacks, stackName) {
        val current = stackName.trim()
        if (current.isEmpty() || current in existingStacks) existingStacks else existingStacks + current
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val snackbarSaveNoDayMessage = stringResource(R.string.habitDetailSnackbarSaveNoDay)

    val appBarTitle = when {
        isNewHabit -> stringResource(R.string.habitDetailTitleNew)
        isEditing -> stringResource(R.string.habitDetailTitleEdit)
        else -> stringResource(R.string.habitDetailTitleView)
    }

    fun saveHabit() {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            nameError = true
            return
        }
        nameError = false

        if (frequency == HabitFrequency.WEEKLY_EXACT && selectedWeekdays.isEmpty()) {
            scope.launch { snackbarHostState.showSnackbar(snackbarSaveNoDayMessage) }
            return
        }

        // Stack/standalone order is only auto-appended when a habit newly joins a stack or
        // newly goes standalone; an unrelated edit shouldn't bump an existing habit to the end.
        // Manual repositioning happens by dragging in the list afterward.
        val trimmedStackName = stackName.trim().ifEmpty { null }
        val stayedInSameStack = existingHabit?.stackName?.trim() == trimmedStackName
        val computedStackOrder = when {
            trimmedStackName == null -> null
            stayedInSameStack -> existingHabit?.stackOrder
            else -> (allHabits.filter { it.stackName?.trim() == trimmedStackName }
                .maxOfOrNull { it.stackOrder ?: -1 } ?: -1) + 1
        }
        val wasStandalone = existingHabit != null && existingHabit.stackName.isNullOrBlank()
        val computedSortOrder = when {
            // The sort order of a stack's members places the whole stack: joining keeps the stack
            // where it is, and a new stack goes last.
            trimmedStackName != null && stayedInSameStack -> existingHabit?.sortOrder ?: 0
            trimmedStackName != null -> allHabits.filter { it.stackName?.trim() == trimmedStackName }
                .minOfOrNull { it.sortOrder }
                ?: ((allHabits.filter { !it.stackName.isNullOrBlank() }
                    .maxOfOrNull { it.sortOrder } ?: -1) + 1)
            wasStandalone -> existingHabit.sortOrder
            else -> (allHabits.filter { it.stackName.isNullOrBlank() }
                .maxOfOrNull { it.sortOrder } ?: -1) + 1
        }

        val habitToSave = HabitEntity(
            id = habitId ?: newItemId,
            name = trimmedName,
            frequency = frequency,
            targetCount = if (frequency.isFlexible) targetCount else null,
            timesPerDay = if (frequency.allowsTimesPerDay) timesPerDay else 1,
            targetWeekdays = if (frequency == HabitFrequency.WEEKLY_EXACT) HabitUtils.serializeWeekdays(
                selectedWeekdays
            ) else null,
            // Monthly habits never remind (they have no due days), so stale times aren't kept.
            reminderTimes = if (frequency == HabitFrequency.MONTHLY_FLEXIBLE) null
            else HabitUtils.serializeReminderTimes(reminderTimes),
            createdAt = existingHabit?.createdAt ?: Instant.now(),
            colorValue = selectedColor.toArgb(),
            notes = notes.trim().ifEmpty { null },
            iconCodePoint = selectedIconIndex,
            stackName = trimmedStackName,
            stackOrder = computedStackOrder,
            sortOrder = computedSortOrder,
            // Joining a stack drops the pin; Home shows a stack as one block.
            pinnedAt = existingHabit?.pinnedAt?.takeIf { trimmedStackName == null },
        )

        if (isNewHabit) {
            habitViewModel.addHabit(habitToSave, pickedTagIds.orEmpty())
            onNavigateBack()
        } else {
            habitViewModel.updateHabit(habitToSave, pickedTagIds)
            isEditing = false
        }
    }

    fun confirmAndDelete() {
        val habit = existingHabit ?: return
        habitViewModel.deleteHabit(habit)
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            NeoTopBar(
                title = appBarTitle,
                navigationIcon = {
                    BackNavigationIcon(onClick = onNavigateBack)
                },
                actions = {
                    DetailTopBarActions(
                        isViewing = !isEditing && !isNewHabit,
                        onDelete = { showDeleteDialog = true },
                        onEdit = { isEditing = true },
                        onSave = { saveHabit() },
                        // Pinning a stacked habit would pull it out of its stack on Home.
                        isPinned = existingHabit?.takeIf { it.stackName.isNullOrBlank() }?.let { it.pinnedAt != null },
                        onTogglePin = {
                            existingHabit?.let { habitViewModel.setPinned(it.id, it.pinnedAt == null) }
                        },
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        ConfirmationDialog(
            visible = showDeleteDialog,
            title = stringResource(R.string.habitDeleteTitle),
            content = stringResource(R.string.habitDeleteContent),
            confirmLabel = stringResource(R.string.commonLabelRemove),
            confirmColor = ErrorColor,
            onConfirm = {
                showDeleteDialog = false
                confirmAndDelete()
            },
            onDismiss = { showDeleteDialog = false },
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(horizontal = AppTheme.spaceLarge, vertical = AppTheme.spaceSmall)
        ) {
            // The basics: name, tags, notes, then the stack, which fewer people use.
            item {
                FormSectionHeader(stringResource(R.string.habitDetailSectionInfo), Icons.Outlined.Info, HabitColor)
            }
            item {
                NeoOutlinedTextField(
                    value = name,
                    onValueChange = { name = it; if (it.isNotBlank()) nameError = false },
                    enabled = isEditing,
                    label = { Text(stringResource(R.string.habitDetailLabelName) + " *") },
                    placeholder = { Text(stringResource(R.string.habitDetailLabelNameHint)) },
                    leadingIcon = { Icon(Icons.Filled.Stars, null, tint = selectedColor) },
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = AppTheme.fwBold),
                    errorText = if (nameError) stringResource(R.string.habitDetailLabelNameRequest) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(rememberAutoFocus(isNewHabit)),
                )
            }

            item {
                if (showsTags) TagPicker(
                    selectedIds = pickedTagIds
                        ?: habitTags[habitId].orEmpty().mapTo(LinkedHashSet()) { it.id },
                    onSelectionChange = { pickedTagIds = it },
                    isEditing = isEditing,
                    modifier = Modifier.padding(top = AppTheme.spaceLarge),
                )
                if ((isEditing || notes.isNotBlank()) && showsNotes) {
                    Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
                    NeoOutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        enabled = isEditing,
                        label = { Text(stringResource(R.string.habitDetailLabelNotes)) },
                        placeholder = { Text(stringResource(R.string.habitDetailLabelNotesHint)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null) },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Habit stack: picked from the existing stacks, or a new one from "+". Position within
            // the stack is set by dragging in the habit list. View mode hides empty optional fields.
            if ((isEditing || stackName.isNotBlank()) && showsStack) item {
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
                val noStackLabel = stringResource(R.string.habitStackNone)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ExposedDropdownMenuBox(
                        expanded = stackNameExpanded && isEditing,
                        onExpandedChange = { if (isEditing) stackNameExpanded = it },
                        modifier = Modifier.weight(1f),
                    ) {
                        NeoOutlinedTextField(
                            value = stackName.trim().ifEmpty { noStackLabel },
                            onValueChange = {},
                            readOnly = true,
                            enabled = isEditing,
                            singleLine = true,
                            label = { Text(stringResource(R.string.habitDetailLabelStack)) },
                            leadingIcon = { Icon(Icons.Filled.Layers, null) },
                            trailingIcon = if (isEditing) {
                                { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stackNameExpanded) }
                            } else null,
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = stackNameExpanded && isEditing,
                            onDismissRequest = { stackNameExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(noStackLabel) },
                                onClick = {
                                    stackName = ""
                                    stackNameExpanded = false
                                },
                            )
                            stackOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        stackName = option
                                        stackNameExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    if (isEditing) {
                        Spacer(modifier = Modifier.width(AppTheme.spaceSmall))
                        NeoIconButton(
                            onClick = { showNewStackDialog = true },
                            icon = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.habitStackNew),
                        )
                    }
                }
            }

            if (showsAppearance) item {
                FormSectionHeader(stringResource(R.string.detailSectionAppearance), Icons.Filled.Palette, HabitColor)
            }
            if (showsAppearance) item {
                AppearanceFields(
                    iconIndex = selectedIconIndex,
                    color = selectedColor,
                    colorLabel = stringResource(R.string.habitDetailLabelColor),
                    enabled = isEditing,
                    onPickIcon = { showIconPicker = true },
                    onPickColor = { showColorPicker = true },
                    showIcon = showsIcon,
                    showColor = showsColor,
                )
            }

            // Frequency and reminders.
            item {
                FormSectionHeader(
                    stringResource(R.string.taskDetailsSectionScheduling),
                    Icons.Filled.CalendarToday,
                    HabitColor,
                )
            }
            item {
                FieldLabel(stringResource(R.string.habitDetailLabelFrequency))
                val frequencyLabels = mapOf(
                    HabitFrequency.DAILY to stringResource(R.string.habitDetailLabelFrequencyDaily),
                    HabitFrequency.WEEKLY_FLEXIBLE to stringResource(R.string.habitDetailLabelFrequencyFlexible),
                    HabitFrequency.WEEKLY_EXACT to stringResource(R.string.habitDetailLabelFrequencyExact),
                )
                NeoSegmentedSelector(
                    values = listOf(
                        HabitFrequency.DAILY,
                        HabitFrequency.WEEKLY_FLEXIBLE,
                        HabitFrequency.WEEKLY_EXACT,
                    ),
                    // Monthly is the flexible option's second period, picked inside its card.
                    selectedValue = if (frequency.isFlexible) HabitFrequency.WEEKLY_FLEXIBLE else frequency,
                    isEditing = isEditing,
                    onSelected = { if (!frequency.isFlexible || !it.isFlexible) selectFrequency(it) },
                    labelBuilder = { frequencyLabels.getValue(it) },
                    activeBgColor = selectedColor,
                )

                // Each block below brings its own top gap, so hidden ones leave no space behind.
                if (frequency.isFlexible) {
                    Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
                    val isMonthly = frequency == HabitFrequency.MONTHLY_FLEXIBLE
                    val maxTarget = if (isMonthly) HabitUtils.MAX_TARGET_PER_MONTH else HabitUtils.MAX_TARGET_PER_WEEK
                    NeoCard(
                        containerColor = if (isEditing) MaterialTheme.colorScheme.surface else Color.Transparent,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppTheme.spaceLarge),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val periodLabels = mapOf(
                                HabitFrequency.WEEKLY_FLEXIBLE to stringResource(R.string.habitDetailLabelPeriodWeek),
                                HabitFrequency.MONTHLY_FLEXIBLE to stringResource(R.string.habitDetailLabelPeriodMonth),
                            )
                            NeoSegmentedSelector(
                                values = listOf(HabitFrequency.WEEKLY_FLEXIBLE, HabitFrequency.MONTHLY_FLEXIBLE),
                                selectedValue = frequency,
                                isEditing = isEditing,
                                onSelected = { selectFrequency(it) },
                                labelBuilder = { periodLabels.getValue(it) },
                                activeBgColor = selectedColor,
                            )
                            Spacer(modifier = Modifier.height(AppTheme.spaceMedium))
                            Text(
                                text = quantityString(
                                    if (isMonthly) R.plurals.habitDetailLabelGoalMonth else R.plurals.habitDetailLabelGoal,
                                    targetCount,
                                    zeroRes = R.string.habitDetailLabelGoalZero,
                                    targetCount,
                                ),
                                fontWeight = AppTheme.fwBold,
                            )
                            NeoSlider(
                                value = targetCount,
                                onValueChange = { if (isEditing) targetCount = it },
                                range = 1..maxTarget,
                                enabled = isEditing,
                                showSteps = true,
                                colors = habitSliderColors(selectedColor),
                            )
                        }
                    }
                } else if (frequency == HabitFrequency.WEEKLY_EXACT) {
                    Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
                    WeekdayToggleRow(
                        selected = selectedWeekdays,
                        onToggle = { day ->
                            selectedWeekdays =
                                if (day in selectedWeekdays) selectedWeekdays - day
                                else selectedWeekdays + day
                        },
                        enabled = isEditing,
                        selectedColor = selectedColor,
                    )
                }
                // Once a day is the default, so view mode only shows the card when it says more.
                val showsTimesPerDayCard = showsTimesPerDay && (isEditing || timesPerDay > 1)
                if (frequency.allowsTimesPerDay && showsTimesPerDayCard) {
                    Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
                    TimesPerDayCard(
                        timesPerDay = timesPerDay,
                        onChange = { timesPerDay = it },
                        isEditing = isEditing,
                        color = selectedColor,
                    )
                }
                // Logging, not editing the habit, so it lives in view mode only.
                if (!isEditing && existingWithCompletions != null && existingWithCompletions.habit.timesPerDay > 1) {
                    Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
                    HabitTodayCard(
                        habitWithCompletions = existingWithCompletions,
                        color = selectedColor,
                        viewModel = habitViewModel,
                        onOpenHistory = { showHistory = true },
                    )
                }
            }

            // Time reminders; monthly habits have no due days to remind on.
            if (frequency != HabitFrequency.MONTHLY_FLEXIBLE && showsReminders) item {
                // The reminder field hides itself in view mode when there are none.
                if (isEditing || reminderTimes.isNotEmpty()) Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
                val use24Hour = LocalDateFormatSettings.current.use24HourFormat
                val entries = reminderTimes.map { minutes ->
                    ReminderEntry(
                        key = minutes.toLong(),
                        preset = null,
                        label = AppDateFormatUtils.formatTime(
                            LocalTime.of(minutes / 60, minutes % 60),
                            use24Hour,
                        ),
                    )
                }
                val atLimit = reminderTimes.size >= HabitUtils.MAX_REMINDER_TIMES
                ReminderField(
                    entries = entries,
                    isEditing = isEditing,
                    sheetSubtitle = name,
                    warning = if (atLimit) {
                        stringResource(R.string.reminderWarnLimit, HabitUtils.MAX_REMINDER_TIMES)
                    } else null,
                    canAddPreset = { false },
                    onTogglePreset = {},
                    onRemoveCustom = { entry -> reminderTimes = reminderTimes - entry.key.toInt() },
                    onAddCustom = { showTimePicker = true },
                    customEnabled = !atLimit,
                    presets = emptyList(),
                    customLabel = stringResource(R.string.habitDetailReminderAddTime),
                )
            }
            item { Spacer(modifier = Modifier.height(AppTheme.space3XLarge)) }
        }

        if (showHistory && existingWithCompletions != null) {
            HabitHistorySheet(
                habitWithCompletions = existingWithCompletions,
                viewModel = habitViewModel,
                onDismissRequest = { showHistory = false },
            )
        }

        // An existing name just picks that stack; stacks are never duplicated.
        TextInputDialog(
            visible = showNewStackDialog,
            title = stringResource(R.string.habitStackNew),
            labelText = stringResource(R.string.habitStackName),
            hintText = stringResource(R.string.habitDetailLabelStackHint),
            onConfirm = { name ->
                stackName = name
                showNewStackDialog = false
            },
            onDismiss = { showNewStackDialog = false },
        )
        if (showIconPicker) {
            IconPickerDialog(
                title = stringResource(R.string.habitDetailLabelIcon),
                selectedIconIndex = selectedIconIndex,
                accentColor = selectedColor,
                onSelect = {
                    selectedIconIndex = it
                    showIconPicker = false
                },
                onDismiss = { showIconPicker = false },
            )
        }

        if (showColorPicker) {
            ColorPickerDialog(
                title = stringResource(R.string.habitDetailLabelColorPicker),
                colors = HABIT_DETAIL_COLORS,
                selectedColor = selectedColor,
                onSelect = {
                    selectedColor = it
                    showColorPicker = false
                },
                onDismiss = { showColorPicker = false },
            )
        }

        // Time picker dialog.
        if (showTimePicker) {
            NeoTimePickerDialog(
                initialHour = 8,
                initialMinute = 0,
                title = { Text(stringResource(R.string.habitDetailLabelReminder)) },
                onConfirm = { hour, minute ->
                    reminderTimes = (reminderTimes + (hour * 60 + minute)).distinct().sorted()
                        .take(HabitUtils.MAX_REMINDER_TIMES)
                    showTimePicker = false
                },
                onDismiss = { showTimePicker = false },
            )
        }
    }
}

private val HABIT_DETAIL_COLORS = listOf(HabitColor) + PickerPalette

/**
 * Slider colors drawn entirely from the habit's [color], ticks and empty track included. Disabled
 * keeps the same colors: the slider position is the value, so it must stay readable in view mode.
 */
@Composable
private fun habitSliderColors(color: Color): SliderColors {
    val inactiveTrack = color.copy(alpha = SLIDER_INACTIVE_TRACK_ALPHA)
    return SliderDefaults.colors(
        thumbColor = color,
        activeTrackColor = color,
        activeTickColor = Color.White,
        inactiveTrackColor = inactiveTrack,
        inactiveTickColor = color,
        disabledThumbColor = color,
        disabledActiveTrackColor = color,
        disabledActiveTickColor = Color.White,
        disabledInactiveTrackColor = inactiveTrack,
        disabledInactiveTickColor = color,
    )
}

private const val SLIDER_INACTIVE_TRACK_ALPHA = 0.24f

/** The check-ins a day needs, from once up to [HabitUtils.MAX_TIMES_PER_DAY]. */
@Composable
private fun TimesPerDayCard(timesPerDay: Int, onChange: (Int) -> Unit, isEditing: Boolean, color: Color) {
    NeoCard(containerColor = if (isEditing) MaterialTheme.colorScheme.surface else Color.Transparent) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.spaceLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = pluralStringResource(R.plurals.habitTimesPerDay, timesPerDay, timesPerDay),
                fontWeight = AppTheme.fwBold,
            )
            NeoSlider(
                value = timesPerDay,
                onValueChange = { if (isEditing) onChange(it) },
                range = 1..HabitUtils.MAX_TIMES_PER_DAY,
                enabled = isEditing,
                showSteps = true,
                colors = habitSliderColors(color),
            )
        }
    }
}

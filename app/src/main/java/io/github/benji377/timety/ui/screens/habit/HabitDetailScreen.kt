package io.github.benji377.timety.ui.screens.habit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.ui.components.common.BackNavigationIcon
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.ui.components.common.ColorPickerDialog
import io.github.benji377.timety.ui.components.common.ConfirmationDialog
import io.github.benji377.timety.ui.components.common.detailFieldColors
import io.github.benji377.timety.ui.components.common.detailSegmentedButtonColors
import io.github.benji377.timety.ui.components.common.DetailTopBarActions
import io.github.benji377.timety.ui.components.common.IconPickerDialog
import io.github.benji377.timety.ui.components.common.NeoFilterChip
import io.github.benji377.timety.ui.components.common.PickerField
import io.github.benji377.timety.ui.components.common.NeoTimePickerDialog
import io.github.benji377.timety.ui.components.common.NeoTopBar
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.PickerPalette
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.ui.utils.quantityString
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.HabitViewModel
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.habit.HabitIcons
import io.github.benji377.timety.util.habit.HabitUtils
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.util.UUID
import io.github.benji377.timety.ui.components.common.NeoOutlinedTextField as OutlinedTextField


/**
 * Creates, views, or edits a habit. With a null [habitId] the screen starts in creation mode;
 * otherwise it opens in read-only view mode until the user taps edit.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitDetailScreen(
    habitId: String? = null,
    onNavigateBack: () -> Unit,
    habitViewModel: HabitViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val allHabits by habitViewModel.allHabits.collectAsState()
    val existingHabit = habitId?.let { id -> allHabits.find { it.id == id } }
    val isNewHabit = habitId == null

    var isEditing by remember { mutableStateOf(isNewHabit) }

    // Form state.
    var name by remember(existingHabit) { mutableStateOf(existingHabit?.name ?: "") }
    var nameError by remember { mutableStateOf(false) }
    var notes by remember(existingHabit) { mutableStateOf(existingHabit?.notes ?: "") }
    var stackName by remember(existingHabit) { mutableStateOf(existingHabit?.stackName ?: "") }

    var frequency by remember(existingHabit) {
        mutableStateOf(
            existingHabit?.frequency ?: HabitFrequency.DAILY
        )
    }
    var targetDaysPerWeek by remember(existingHabit) {
        mutableIntStateOf(
            existingHabit?.targetDaysPerWeek ?: 3
        )
    }
    var selectedWeekdays by remember(existingHabit) {
        mutableStateOf(
            existingHabit?.targetWeekdays?.let { HabitUtils.parseWeekdays(it) }
                ?.takeIf { it.isNotEmpty() }
                ?: setOf(1, 3, 5)
        )
    }

    var selectedIconIndex by remember(existingHabit) { mutableStateOf(existingHabit?.iconCodePoint) }
    var selectedColor by remember(existingHabit) {
        mutableStateOf(existingHabit?.colorValue?.let { Color(it) } ?: HabitColor)
    }
    var targetTimeMinutes by remember(existingHabit) { mutableStateOf(existingHabit?.targetTimeMinutes) }

    var showIconPicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var stackNameExpanded by remember { mutableStateOf(false) }
    val existingStacks = remember(allHabits) {
        allHabits.mapNotNull { it.stackName?.trim() }.filter { it.isNotEmpty() }.distinct()
    }
    val filteredStacks = remember(stackName, existingStacks) {
        if (stackName.isEmpty()) existingStacks
        else existingStacks.filter { it.contains(stackName, ignoreCase = true) }
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
            trimmedStackName != null -> existingHabit?.sortOrder ?: 0
            wasStandalone -> existingHabit.sortOrder
            else -> (allHabits.filter { it.stackName.isNullOrBlank() }
                .maxOfOrNull { it.sortOrder } ?: -1) + 1
        }

        val habitToSave = HabitEntity(
            id = habitId ?: UUID.randomUUID().toString(),
            name = trimmedName,
            frequency = frequency,
            targetDaysPerWeek = if (frequency == HabitFrequency.WEEKLY_FLEXIBLE) targetDaysPerWeek else null,
            targetWeekdays = if (frequency == HabitFrequency.WEEKLY_EXACT) HabitUtils.serializeWeekdays(
                selectedWeekdays
            ) else null,
            targetTimeMinutes = targetTimeMinutes,
            createdAt = existingHabit?.createdAt ?: Instant.now(),
            colorValue = selectedColor.toArgb(),
            notes = notes.trim().ifEmpty { null },
            iconCodePoint = selectedIconIndex,
            stackName = trimmedStackName,
            stackOrder = computedStackOrder,
            sortOrder = computedSortOrder,
        )

        if (isNewHabit) {
            habitViewModel.addHabit(habitToSave)
            onNavigateBack()
        } else {
            habitViewModel.updateHabit(habitToSave)
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
            // Habit name.
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; if (it.isNotBlank()) nameError = false },
                    enabled = isEditing,
                    label = { Text(stringResource(R.string.habitDetailLabelName) + " *") },
                    placeholder = { Text(stringResource(R.string.habitDetailLabelNameHint)) },
                    leadingIcon = { Icon(Icons.Filled.Stars, null, tint = selectedColor) },
                    isError = nameError,
                    supportingText = {
                        if (nameError) Text(
                            stringResource(R.string.habitDetailLabelNameRequest),
                            color = ErrorColor
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
            }

            // Habit stacking: name autocomplete. Position within the stack is set by dragging
            // in the habit list, not entered here.
            item {
                ExposedDropdownMenuBox(
                    expanded = stackNameExpanded && isEditing && filteredStacks.isNotEmpty(),
                    onExpandedChange = { if (isEditing) stackNameExpanded = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = if (isEditing) stackName else stackName.ifBlank { "-" },
                        onValueChange = {
                            stackName = it
                            stackNameExpanded = true
                        },
                        enabled = isEditing,
                        singleLine = true,
                        label = { Text(stringResource(R.string.habitDetailLabelStack)) },
                        placeholder = { Text(stringResource(R.string.habitDetailLabelStackHint)) },
                        leadingIcon = { Icon(Icons.Filled.Layers, null) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .fillMaxWidth(),
                    )
                    if (filteredStacks.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = stackNameExpanded && isEditing,
                            onDismissRequest = { stackNameExpanded = false },
                        ) {
                            filteredStacks.forEach { option ->
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
                }
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
            }

            // Appearance: icon and color.
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    PickerField(
                        label = stringResource(R.string.habitDetailLabelIcon),
                        enabled = isEditing,
                        onClick = { showIconPicker = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = HabitIcons.iconAt(selectedIconIndex),
                            contentDescription = null,
                            tint = selectedColor,
                        )
                    }
                    Spacer(modifier = Modifier.width(AppTheme.spaceLarge))
                    PickerField(
                        label = stringResource(R.string.habitDetailLabelColor),
                        enabled = isEditing,
                        onClick = { showColorPicker = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    color = selectedColor,
                                    shape = CircleShape,
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
            }

            // Notes.
            item {
                OutlinedTextField(
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
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
            }

            // Frequency.
            item {
                Text(
                    stringResource(R.string.habitDetailLabelFrequency),
                    fontWeight = AppTheme.fwBold
                )
                Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
                val segmentedColors = detailSegmentedButtonColors()
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = frequency == HabitFrequency.DAILY,
                        onClick = { if (isEditing) frequency = HabitFrequency.DAILY },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        enabled = isEditing,
                        colors = segmentedColors
                    ) { Text(stringResource(R.string.habitDetailLabelFrequencyDaily)) }
                    SegmentedButton(
                        selected = frequency == HabitFrequency.WEEKLY_FLEXIBLE,
                        onClick = { if (isEditing) frequency = HabitFrequency.WEEKLY_FLEXIBLE },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                        enabled = isEditing,
                        colors = segmentedColors
                    ) { Text(stringResource(R.string.habitDetailLabelFrequencyFlexible)) }
                    SegmentedButton(
                        selected = frequency == HabitFrequency.WEEKLY_EXACT,
                        onClick = { if (isEditing) frequency = HabitFrequency.WEEKLY_EXACT },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        enabled = isEditing,
                        colors = segmentedColors
                    ) { Text(stringResource(R.string.habitDetailLabelFrequencyExact)) }
                }
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))

                if (frequency == HabitFrequency.WEEKLY_FLEXIBLE) {
                    Card(
                        shape = AppTheme.brNeo,
                        border = BorderStroke(
                            AppTheme.neoBorderWidth,
                            if (isEditing) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.12f
                            )
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isEditing) 6.dp else 0.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isEditing) MaterialTheme.colorScheme.surface else Color.Transparent)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppTheme.spaceLarge),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = quantityString(
                                    R.plurals.habitDetailLabelGoal,
                                    targetDaysPerWeek,
                                    zeroRes = R.string.habitDetailLabelGoalZero,
                                    targetDaysPerWeek,
                                ),
                                fontWeight = AppTheme.fwBold,
                            )
                            Slider(
                                value = targetDaysPerWeek.toFloat(),
                                onValueChange = { if (isEditing) targetDaysPerWeek = it.toInt() },
                                valueRange = 1f..7f,
                                steps = 5,
                                enabled = isEditing,
                                colors = SliderDefaults.colors(
                                    thumbColor = selectedColor,
                                    activeTrackColor = selectedColor,
                                    // The slider position IS the value: keep it readable in view
                                    // mode; the muted card chrome already signals inactive.
                                    disabledThumbColor = selectedColor,
                                    disabledActiveTrackColor = selectedColor,
                                ),
                            )
                        }
                    }
                } else if (frequency == HabitFrequency.WEEKLY_EXACT) {
                    Card(
                        shape = AppTheme.brNeo,
                        border = BorderStroke(
                            AppTheme.neoBorderWidth,
                            if (isEditing) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.12f
                            )
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isEditing) 6.dp else 0.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isEditing) MaterialTheme.colorScheme.surface else Color.Transparent)
                    ) {
                        val dayLabels = listOf(
                            1 to stringResource(R.string.calendarHeaderMon),
                            2 to stringResource(R.string.calendarHeaderTue),
                            3 to stringResource(R.string.calendarHeaderWed),
                            4 to stringResource(R.string.calendarHeaderThu),
                            5 to stringResource(R.string.calendarHeaderFri),
                            6 to stringResource(R.string.calendarHeaderSat),
                            7 to stringResource(R.string.calendarHeaderSun),
                        )
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppTheme.spaceLarge),
                            horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall),
                            verticalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall),
                        ) {
                            dayLabels.forEach { (day, label) ->
                                val isSelected = selectedWeekdays.contains(day)
                                NeoFilterChip(
                                    selected = isSelected,
                                    enabled = isEditing,
                                    onClick = {
                                        selectedWeekdays =
                                            if (isSelected) selectedWeekdays - day else selectedWeekdays + day
                                    },
                                    label = label,
                                    selectedColor = selectedColor,
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
            }

            // Time reminder.
            item {
                Text(
                    stringResource(R.string.habitDetailLabelReminder),
                    fontWeight = AppTheme.fwBold
                )
                Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
                val timeLabel = targetTimeMinutes?.let {
                    val time = LocalTime.of(it / 60, it % 60)
                    AppDateFormatUtils.formatTime(
                        time,
                        LocalDateFormatSettings.current.use24HourFormat
                    )
                } ?: stringResource(R.string.habitDetailLabelReminderNoTime)

                Box(modifier = Modifier.clickable(enabled = isEditing) { showTimePicker = true }) {
                    OutlinedTextField(
                        value = timeLabel,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        leadingIcon = { Icon(Icons.Filled.NotificationsActive, null) },
                        trailingIcon = { if (isEditing) Icon(Icons.Filled.Edit, null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = detailFieldColors(isEditing),
                    )
                }
                Spacer(modifier = Modifier.height(AppTheme.space3XLarge))
            }
        }

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
            val initial = targetTimeMinutes
            NeoTimePickerDialog(
                initialHour = initial?.let { it / 60 } ?: 8,
                initialMinute = initial?.let { it % 60 } ?: 0,
                title = { Text(stringResource(R.string.habitDetailLabelReminder)) },
                onConfirm = { hour, minute ->
                    targetTimeMinutes = hour * 60 + minute
                    showTimePicker = false
                },
                onDismiss = { showTimePicker = false },
            )
        }
    }
}


private val HABIT_DETAIL_COLORS = listOf(HabitColor) + PickerPalette

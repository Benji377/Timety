package io.github.benji377.timety.ui.screens.habit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import io.github.benji377.timety.ui.components.common.TimetyOutlinedTextField as OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.ui.components.common.ConfirmationDialog
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.utils.quantityString
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.HabitViewModel
import io.github.benji377.timety.util.habit.HabitIcons
import io.github.benji377.timety.util.habit.HabitUtils
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

/**
 * Mirrors `HabitDetailScreen` in `screens/habit/habit_detail_screen.dart`: a combined
 * create/view/edit screen for a single habit, gated by an `isEditing` flag (always true
 * for brand-new habits).
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

    // --- Form state ---
    var name by remember(existingHabit) { mutableStateOf(existingHabit?.name ?: "") }
    var nameError by remember { mutableStateOf(false) }
    var notes by remember(existingHabit) { mutableStateOf(existingHabit?.notes ?: "") }
    var stackName by remember(existingHabit) { mutableStateOf(existingHabit?.stackName ?: "") }
    var stackOrder by remember(existingHabit) { mutableStateOf(existingHabit?.stackOrder) }

    var frequency by remember(existingHabit) {
        mutableStateOf(
            existingHabit?.frequency ?: HabitFrequency.DAILY
        )
    }
    var targetDaysPerWeek by remember(existingHabit) {
        mutableStateOf(
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

    var stackOrderExpanded by remember { mutableStateOf(false) }
    val orderOptions: List<Int?> = remember { listOf(null) + (1..10).toList() }

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
            stackName = stackName.trim().ifEmpty { null },
            stackOrder = stackOrder,
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
            TopAppBar(
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background),
                title = { Text(appBarTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (!isEditing && !isNewHabit) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Filled.DeleteOutline,
                                stringResource(R.string.commonLabelDelete),
                                tint = ErrorColor
                            )
                        }
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Filled.Edit, "Edit")
                        }
                    } else {
                        IconButton(onClick = { saveHabit() }) {
                            Icon(Icons.Filled.Check, stringResource(R.string.commonLabelSave))
                        }
                    }
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
            // --- HABIT NAME ---
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; if (it.isNotBlank()) nameError = false },
                    enabled = isEditing,
                    label = { Text(stringResource(R.string.habitDetailLabelName)) },
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

            // --- HABIT STACKING (name autocomplete + order) ---
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = stackNameExpanded && isEditing && filteredStacks.isNotEmpty(),
                        onExpandedChange = { if (isEditing) stackNameExpanded = it },
                        modifier = Modifier.weight(2f),
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
                    Spacer(modifier = Modifier.width(AppTheme.spaceMedium))
                    ExposedDropdownMenuBox(
                        expanded = stackOrderExpanded && isEditing,
                        onExpandedChange = { if (isEditing) stackOrderExpanded = it },
                        modifier = Modifier.weight(1f),
                    ) {
                        OutlinedTextField(
                            value = stackOrder?.toString() ?: "-",
                            onValueChange = {},
                            readOnly = true,
                            enabled = isEditing,
                            label = { Text(stringResource(R.string.habitDetailLabelStackOrder)) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = stackOrderExpanded && isEditing,
                            onDismissRequest = { stackOrderExpanded = false },
                        ) {
                            orderOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option?.toString() ?: "-") },
                                    onClick = {
                                        stackOrder = option
                                        stackOrderExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
            }

            // --- APPEARANCE (icon & color) ---
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
                            tint = if (isEditing) selectedColor else selectedColor.copy(alpha = 0.5f),
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
                                    color = if (isEditing) selectedColor else selectedColor.copy(
                                        alpha = 0.5f
                                    ),
                                    shape = CircleShape,
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
            }

            // --- NOTES ---
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

            // --- FREQUENCY ---
            item {
                Text(
                    stringResource(R.string.habitDetailLabelFrequency),
                    fontWeight = AppTheme.fwBold
                )
                Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = frequency == HabitFrequency.DAILY,
                        onClick = { if (isEditing) frequency = HabitFrequency.DAILY },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    ) { Text(stringResource(R.string.habitDetailLabelFrequencyDaily)) }
                    SegmentedButton(
                        selected = frequency == HabitFrequency.WEEKLY_FLEXIBLE,
                        onClick = { if (isEditing) frequency = HabitFrequency.WEEKLY_FLEXIBLE },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    ) { Text(stringResource(R.string.habitDetailLabelFrequencyFlexible)) }
                    SegmentedButton(
                        selected = frequency == HabitFrequency.WEEKLY_EXACT,
                        onClick = { if (isEditing) frequency = HabitFrequency.WEEKLY_EXACT },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    ) { Text(stringResource(R.string.habitDetailLabelFrequencyExact)) }
                }
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))

                if (frequency == HabitFrequency.WEEKLY_FLEXIBLE) {
                    Card(
                        shape = AppTheme.brNeo,
                        border = BorderStroke(
                            AppTheme.neoBorderWidth,
                            MaterialTheme.colorScheme.outline
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
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
                                ),
                            )
                        }
                    }
                } else if (frequency == HabitFrequency.WEEKLY_EXACT) {
                    Card(
                        shape = AppTheme.brNeo,
                        border = BorderStroke(
                            AppTheme.neoBorderWidth,
                            MaterialTheme.colorScheme.outline
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
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
                                FilterChip(
                                    selected = isSelected,
                                    enabled = isEditing,
                                    onClick = {
                                        selectedWeekdays =
                                            if (isSelected) selectedWeekdays - day else selectedWeekdays + day
                                    },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = selectedColor.copy(alpha = 0.3f),
                                    ),
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
            }

            // --- TIME REMINDER ---
            item {
                Text(
                    stringResource(R.string.habitDetailLabelReminder),
                    fontWeight = AppTheme.fwBold
                )
                Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
                val timeLabel = targetTimeMinutes?.let {
                    val time = LocalTime.of(it / 60, it % 60)
                    io.github.benji377.timety.util.datetime.AppDateFormatUtils.formatTime(
                        time,
                        io.github.benji377.timety.ui.utils.LocalDateFormatSettings.current.use24HourFormat
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
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = if (isEditing) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledBorderColor = if (isEditing) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
                            disabledLeadingIconColor = if (isEditing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outlineVariant,
                        ),
                    )
                }
                Spacer(modifier = Modifier.height(AppTheme.space3XLarge))
            }
        }

        // --- Icon picker dialog ---
        if (showIconPicker) {
            AlertDialog(
                onDismissRequest = { showIconPicker = false },
                title = { Text(stringResource(R.string.habitDetailLabelIcon)) },
                text = {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceLarge),
                        verticalArrangement = Arrangement.spacedBy(AppTheme.spaceLarge),
                        modifier = Modifier.height(300.dp),
                    ) {
                        items(HabitIcons.availableIcons.size) { index ->
                            val isSelected = index == selectedIconIndex
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = if (isSelected) selectedColor.copy(alpha = 0.2f) else Color.Transparent,
                                        shape = CircleShape,
                                    )
                                    .clickable {
                                        selectedIconIndex = index
                                        showIconPicker = false
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = HabitIcons.availableIcons[index],
                                    contentDescription = null,
                                    tint = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showIconPicker = false
                    }) { Text(stringResource(R.string.commonLabelCancel)) }
                },
            )
        }

        // --- Color picker dialog ---
        if (showColorPicker) {
            AlertDialog(
                onDismissRequest = { showColorPicker = false },
                title = { Text(stringResource(R.string.habitDetailLabelColorPicker)) },
                text = {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceLarge),
                        verticalArrangement = Arrangement.spacedBy(AppTheme.spaceLarge),
                        modifier = Modifier.height(220.dp),
                    ) {
                        items(HABIT_DETAIL_COLORS.size) { index ->
                            val optionColor = HABIT_DETAIL_COLORS[index]
                            val isSelected = optionColor == selectedColor
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(optionColor, CircleShape)
                                    .then(
                                        if (isSelected) Modifier.border(
                                            3.dp,
                                            MaterialTheme.colorScheme.onSurface,
                                            CircleShape
                                        )
                                        else Modifier
                                    )
                                    .clickable {
                                        selectedColor = optionColor
                                        showColorPicker = false
                                    }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showColorPicker = false
                    }) { Text(stringResource(R.string.commonLabelCancel)) }
                },
            )
        }

        // --- Time picker dialog ---
        if (showTimePicker) {
            val initial = targetTimeMinutes
            val timePickerState = rememberTimePickerState(
                initialHour = initial?.let { it / 60 } ?: 8,
                initialMinute = initial?.let { it % 60 } ?: 0,
                is24Hour = io.github.benji377.timety.ui.utils.LocalDateFormatSettings.current.use24HourFormat,
            )
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                title = { Text(stringResource(R.string.habitDetailLabelReminder)) },
                text = { TimePicker(state = timePickerState) },
                confirmButton = {
                    TextButton(onClick = {
                        targetTimeMinutes = timePickerState.hour * 60 + timePickerState.minute
                        showTimePicker = false
                    }) { Text(stringResource(R.string.commonLabelConfirm)) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showTimePicker = false
                    }) { Text(stringResource(R.string.commonLabelCancel)) }
                },
            )
        }
    }
}

/** Standard 15-color palette offered by the Flutter color picker dialog (habit_detail_screen.dart). */
private val HABIT_DETAIL_COLORS = listOf(
    HabitColor,
    Color(0xFFF44336), // Colors.red
    Color(0xFFE91E63), // Colors.pink
    Color(0xFFFFC107), // Colors.amber
    Color(0xFFFF9800), // Colors.orange
    Color(0xFF4CAF50), // Colors.green
    Color(0xFF8BC34A), // Colors.lightGreen
    Color(0xFF009688), // Colors.teal
    Color(0xFF2196F3), // Colors.blue
    Color(0xFF00BCD4), // Colors.cyan
    Color(0xFF3F51B5), // Colors.indigo
    Color(0xFF9C27B0), // Colors.purple
    Color(0xFF673AB7), // Colors.deepPurple
    Color(0xFF795548), // Colors.brown
    Color(0xFF607D8B), // Colors.blueGrey
)

/** A clickable outlined field mimicking Flutter's `InputDecorator` wrapping an arbitrary [content] (icon/color swatch). */
@Composable
private fun PickerField(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.clickable(enabled = enabled) { onClick() }) {
        OutlinedTextField(
            value = "",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            leadingIcon = { Box(contentAlignment = Alignment.Center) { content() } },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}

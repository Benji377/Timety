package io.github.benji377.timety.ui.screens.habit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.habit.QuickHabitEntity
import io.github.benji377.timety.ui.components.common.ConfirmationDialog
import io.github.benji377.timety.ui.components.common.TimetyTimePickerDialog
import io.github.benji377.timety.ui.components.common.TimetyTopBar
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.ui.utils.quantityString
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.QuickHabitViewModel
import io.github.benji377.timety.util.habit.HabitUtils
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import io.github.benji377.timety.ui.components.common.TimetyButton as Button
import io.github.benji377.timety.ui.components.common.TimetyOutlinedTextField as OutlinedTextField

/**
 * Lists the user's quick habits (interval reminders) and lets them add, edit, pause, or delete
 * each one. Quick habits are reminder-only, so there is no completion or stats surface here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickHabitsScreen(
    onNavigateBack: () -> Unit,
    viewModel: QuickHabitViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val quickHabits by viewModel.quickHabits.collectAsState()
    val use24Hour = LocalDateFormatSettings.current.use24HourFormat
    val locale = LocalLocale.current.platformLocale

    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<QuickHabitEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<QuickHabitEntity?>(null) }

    Scaffold(
        topBar = {
            TimetyTopBar(
                title = stringResource(R.string.quickHabitsTitle),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = AppTheme.space3XLarge),
        ) {
            item {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppTheme.spaceLarge),
                    colors = ButtonDefaults.buttonColors(containerColor = HabitColor),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(AppTheme.spaceSmall))
                    Text(stringResource(R.string.quickHabitAddLabel))
                }
            }
            if (quickHabits.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.quickHabitsScreenEmpty),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppTheme.spaceLarge),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                items(quickHabits, key = { it.id }) { quickHabit ->
                    QuickHabitCard(
                        quickHabit = quickHabit,
                        subtitle = quickHabitSubtitle(quickHabit, use24Hour, locale),
                        onToggleEnabled = { viewModel.setEnabled(quickHabit, it) },
                        onEdit = { editTarget = quickHabit },
                        onDelete = { pendingDelete = quickHabit },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        QuickHabitEditDialog(
            initial = null,
            use24Hour = use24Hour,
            locale = locale,
            onDismiss = { showAddDialog = false },
            onConfirm = {
                viewModel.addQuickHabit(it)
                showAddDialog = false
            },
        )
    }
    editTarget?.let { target ->
        QuickHabitEditDialog(
            initial = target,
            use24Hour = use24Hour,
            locale = locale,
            onDismiss = { editTarget = null },
            onConfirm = {
                viewModel.updateQuickHabit(it)
                editTarget = null
            },
        )
    }

    val deleteTarget = pendingDelete
    ConfirmationDialog(
        visible = deleteTarget != null,
        title = stringResource(R.string.quickHabitDeleteTitle),
        content = stringResource(R.string.quickHabitDeleteContent, deleteTarget?.name ?: ""),
        confirmLabel = stringResource(R.string.commonLabelDelete),
        confirmColor = ErrorColor,
        onConfirm = {
            deleteTarget?.let { viewModel.deleteQuickHabit(it) }
            pendingDelete = null
        },
        onDismiss = { pendingDelete = null },
    )
}

@Composable
private fun QuickHabitCard(
    quickHabit: QuickHabitEntity,
    subtitle: String,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.spaceSmall, vertical = AppTheme.spaceXSmall),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = AppTheme.spaceMedium, top = AppTheme.spaceSmall, bottom = AppTheme.spaceSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(quickHabit.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(AppTheme.spaceSmall))
            Switch(
                checked = quickHabit.isEnabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(checkedTrackColor = SuccessColor),
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.quickHabitEditTitle))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.commonLabelDelete),
                    tint = ErrorColor,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun QuickHabitEditDialog(
    initial: QuickHabitEntity?,
    use24Hour: Boolean,
    locale: Locale,
    onDismiss: () -> Unit,
    onConfirm: (QuickHabitEntity) -> Unit,
) {
    val isEditing = initial != null
    var name by remember { mutableStateOf(initial?.name ?: "") }
    // Interval is entered freely as a number plus a minutes/hours unit. Default to 2 hours.
    val initialInterval = initial?.intervalMinutes ?: 120
    val initialInHours = initialInterval % 60 == 0 && initialInterval >= 60
    var unitIsHours by remember { mutableStateOf(initialInHours) }
    var intervalText by remember {
        mutableStateOf((if (initialInHours) initialInterval / 60 else initialInterval).toString())
    }
    val intervalValue = intervalText.toIntOrNull()
    val intervalMinutes = (intervalValue ?: 0) * (if (unitIsHours) 60 else 1)
    var specificDays by remember {
        mutableStateOf(HabitUtils.parseWeekdays(initial?.targetWeekdays).isNotEmpty())
    }
    var selectedWeekdays by remember {
        mutableStateOf(HabitUtils.parseWeekdays(initial?.targetWeekdays))
    }
    var allDay by remember {
        mutableStateOf(initial?.startMinuteOfDay == null || initial.endMinuteOfDay == null)
    }
    var startMinute by remember { mutableIntStateOf(initial?.startMinuteOfDay ?: (8 * 60)) }
    var endMinute by remember { mutableIntStateOf(initial?.endMinuteOfDay ?: (20 * 60)) }
    var editingWindowStart by remember { mutableStateOf<Boolean?>(null) }

    val canSave = name.isNotBlank() &&
        intervalValue != null && intervalValue >= 1 &&
        (!specificDays || selectedWeekdays.isNotEmpty())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEditing) stringResource(R.string.quickHabitEditTitle)
                else stringResource(R.string.quickHabitAddLabel)
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.quickHabitNameLabel)) },
                    placeholder = { Text(stringResource(R.string.quickHabitNameHint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))

                Text(stringResource(R.string.quickHabitIntervalLabel))
                Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall),
                ) {
                    OutlinedTextField(
                        value = intervalText,
                        onValueChange = { new ->
                            if (new.length <= 4 && new.all(Char::isDigit)) intervalText = new
                        },
                        singleLine = true,
                        isError = intervalValue == null || intervalValue < 1,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(96.dp),
                    )
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = !unitIsHours,
                            onClick = { unitIsHours = false },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        ) { Text(stringResource(R.string.quickHabitUnitMinutes)) }
                        SegmentedButton(
                            selected = unitIsHours,
                            onClick = { unitIsHours = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        ) { Text(stringResource(R.string.quickHabitUnitHours)) }
                    }
                }
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))

                Text(stringResource(R.string.quickHabitDaysLabel))
                Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall)) {
                    FilterChip(
                        selected = !specificDays,
                        onClick = { specificDays = false },
                        label = { Text(stringResource(R.string.quickHabitEveryDay)) },
                    )
                    FilterChip(
                        selected = specificDays,
                        onClick = { specificDays = true },
                        label = { Text(stringResource(R.string.quickHabitSpecificDays)) },
                    )
                }
                if (specificDays) {
                    Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
                    WeekdayChips(
                        selected = selectedWeekdays,
                        onToggle = { day ->
                            selectedWeekdays =
                                if (day in selectedWeekdays) selectedWeekdays - day
                                else selectedWeekdays + day
                        },
                    )
                }
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.quickHabitAllDayLabel), modifier = Modifier.weight(1f))
                    Switch(
                        checked = allDay,
                        onCheckedChange = { allDay = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = SuccessColor),
                    )
                }
                if (!allDay) {
                    Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall),
                    ) {
                        WindowTimeButton(
                            label = stringResource(R.string.quickHabitStartLabel),
                            minuteOfDay = startMinute,
                            use24Hour = use24Hour,
                            locale = locale,
                            modifier = Modifier.weight(1f),
                            onClick = { editingWindowStart = true },
                        )
                        WindowTimeButton(
                            label = stringResource(R.string.quickHabitEndLabel),
                            minuteOfDay = endMinute,
                            use24Hour = use24Hour,
                            locale = locale,
                            modifier = Modifier.weight(1f),
                            onClick = { editingWindowStart = false },
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.commonLabelCancel)) }
        },
        confirmButton = {
            Button(
                enabled = canSave,
                onClick = {
                    onConfirm(
                        (initial ?: QuickHabitEntity(
                            id = UUID.randomUUID().toString(),
                            name = name.trim(),
                            intervalMinutes = intervalMinutes,
                            createdAt = java.time.Instant.now(),
                        )).copy(
                            name = name.trim(),
                            intervalMinutes = intervalMinutes,
                            startMinuteOfDay = if (allDay) null else startMinute,
                            endMinuteOfDay = if (allDay) null else endMinute,
                            targetWeekdays = if (specificDays && selectedWeekdays.isNotEmpty())
                                HabitUtils.serializeWeekdays(selectedWeekdays) else null,
                        )
                    )
                },
            ) {
                Text(
                    if (isEditing) stringResource(R.string.commonLabelSave)
                    else stringResource(R.string.quickHabitAddLabel)
                )
            }
        },
    )

    editingWindowStart?.let { isStart ->
        val initialMinuteOfDay = if (isStart) startMinute else endMinute
        TimetyTimePickerDialog(
            initialHour = initialMinuteOfDay / 60,
            initialMinute = initialMinuteOfDay % 60,
            confirmLabel = stringResource(R.string.commonLabelSave),
            onConfirm = { hour, minute ->
                val minuteOfDay = hour * 60 + minute
                if (isStart) startMinute = minuteOfDay else endMinute = minuteOfDay
                editingWindowStart = null
            },
            onDismiss = { editingWindowStart = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekdayChips(selected: Set<Int>, onToggle: (Int) -> Unit) {
    val dayLabels = listOf(
        1 to stringResource(R.string.calendarHeaderMon),
        2 to stringResource(R.string.calendarHeaderTue),
        3 to stringResource(R.string.calendarHeaderWed),
        4 to stringResource(R.string.calendarHeaderThu),
        5 to stringResource(R.string.calendarHeaderFri),
        6 to stringResource(R.string.calendarHeaderSat),
        7 to stringResource(R.string.calendarHeaderSun),
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall)) {
        dayLabels.forEach { (day, label) ->
            FilterChip(
                selected = day in selected,
                onClick = { onToggle(day) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun WindowTimeButton(
    label: String,
    minuteOfDay: Int,
    use24Hour: Boolean,
    locale: Locale,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(formatMinuteOfDay(minuteOfDay, use24Hour, locale))
        }
    }
}

/** "Every 2 hours" / "Every 30 minutes", using hours when the interval divides evenly. */
@Composable
private fun intervalLabel(minutes: Int): String = if (minutes % 60 == 0) {
    quantityString(R.plurals.quickHabitEveryHours, minutes / 60, 0, minutes / 60)
} else {
    quantityString(R.plurals.quickHabitEveryMinutes, minutes, 0, minutes)
}

@Composable
private fun quickHabitSubtitle(
    quickHabit: QuickHabitEntity,
    use24Hour: Boolean,
    locale: Locale,
): String {
    val interval = intervalLabel(quickHabit.intervalMinutes)
    val window = if (quickHabit.startMinuteOfDay == null || quickHabit.endMinuteOfDay == null) {
        stringResource(R.string.quickHabitAllDayLabel)
    } else {
        stringResource(
            R.string.quickHabitWindowFormat,
            formatMinuteOfDay(quickHabit.startMinuteOfDay, use24Hour, locale),
            formatMinuteOfDay(quickHabit.endMinuteOfDay, use24Hour, locale),
        )
    }
    val days = HabitUtils.parseWeekdays(quickHabit.targetWeekdays)
    val base = stringResource(R.string.quickHabitCardSubtitle, interval, window)
    return if (days.isEmpty()) base
    else stringResource(R.string.quickHabitCardSubtitle, base, weekdaysSummary(days))
}

/** A compact weekday summary like "Mon, Wed, Fri" in Mon..Sun order. */
@Composable
private fun weekdaysSummary(days: Set<Int>): String {
    val labels = listOf(
        stringResource(R.string.calendarHeaderMon), stringResource(R.string.calendarHeaderTue),
        stringResource(R.string.calendarHeaderWed), stringResource(R.string.calendarHeaderThu),
        stringResource(R.string.calendarHeaderFri), stringResource(R.string.calendarHeaderSat),
        stringResource(R.string.calendarHeaderSun),
    )
    return days.sorted().joinToString(", ") { labels[it - 1] }
}

private fun formatMinuteOfDay(minuteOfDay: Int, use24Hour: Boolean, locale: Locale): String {
    val time = LocalTime.of((minuteOfDay / 60).coerceIn(0, 23), (minuteOfDay % 60).coerceIn(0, 59))
    val pattern = if (use24Hour) "HH:mm" else "hh:mm a"
    return time.format(DateTimeFormatter.ofPattern(pattern, locale))
}

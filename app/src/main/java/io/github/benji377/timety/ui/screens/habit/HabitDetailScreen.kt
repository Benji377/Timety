package io.github.benji377.timety.ui.screens.habit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.ui.screens.task.SectionHeader
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.HabitViewModel
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    habitId: String? = null,
    onNavigateBack: () -> Unit,
    habitViewModel: HabitViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val allHabits by habitViewModel.allHabits.collectAsState()
    val existingHabit = habitId?.let { id -> allHabits.find { it.id == id } }

    var isEditing by remember { mutableStateOf(habitId == null) }

    // Form State
    var name by remember(existingHabit) { mutableStateOf(existingHabit?.name ?: "") }
    var notes by remember(existingHabit) { mutableStateOf(existingHabit?.notes ?: "") }
    var stackName by remember(existingHabit) { mutableStateOf(existingHabit?.stackName ?: "") }
    var stackOrder by remember(existingHabit) { mutableStateOf(existingHabit?.stackOrder ?: 1) }

    var frequency by remember(existingHabit) { mutableStateOf(existingHabit?.frequency ?: HabitFrequency.DAILY) }
    var targetDaysPerWeek by remember(existingHabit) { mutableStateOf(existingHabit?.targetDaysPerWeek ?: 3) }
    var selectedWeekdays by remember(existingHabit) { 
        mutableStateOf(existingHabit?.targetWeekdays?.removePrefix("[")?.removeSuffix("]")?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet() ?: setOf(1, 3, 5)) 
    }

    var selectedColor by remember(existingHabit) { mutableStateOf(existingHabit?.colorValue?.let { Color(it) } ?: HabitColor) }
    var targetTimeMinutes by remember(existingHabit) { mutableStateOf(existingHabit?.targetTimeMinutes) }

    val appBarTitle = when {
        habitId == null -> "New Habit"
        isEditing -> "Edit Habit"
        else -> "Habit Details"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appBarTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (!isEditing && habitId != null) {
                        IconButton(onClick = { 
                            habitViewModel.deleteHabit(existingHabit!!)
                            onNavigateBack()
                        }) {
                            Icon(Icons.Filled.DeleteOutline, "Delete", tint = ErrorColor)
                        }
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Filled.Edit, "Edit")
                        }
                    } else {
                        IconButton(onClick = {
                            if (name.isNotBlank()) {
                                val habitToSave = HabitEntity(
                                    id = habitId ?: UUID.randomUUID().toString(),
                                    name = name,
                                    notes = notes.takeIf { it.isNotBlank() },
                                    stackName = stackName.takeIf { it.isNotBlank() },
                                    stackOrder = stackOrder,
                                    frequency = frequency,
                                    targetDaysPerWeek = if (frequency == HabitFrequency.WEEKLY_FLEXIBLE) targetDaysPerWeek else null,
                                    targetWeekdays = if (frequency == HabitFrequency.WEEKLY_EXACT) selectedWeekdays.joinToString(",", "[", "]") else null,
                                    targetTimeMinutes = targetTimeMinutes,
                                    colorValue = selectedColor.toArgb(),
                                    createdAt = existingHabit?.createdAt ?: Instant.now()
                                )
                                if (habitId == null) {
                                    habitViewModel.addHabit(habitToSave)
                                } else {
                                    habitViewModel.updateHabit(habitToSave)
                                }
                                onNavigateBack()
                            }
                        }) {
                            Icon(Icons.Filled.Check, "Save")
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
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit Name") },
                    enabled = isEditing,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Filled.Stars, null, tint = selectedColor) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // -- HABIT STACKING --
            item { SectionHeader("STACKING & ORDER", Icons.Filled.Layers) }
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = stackName,
                        onValueChange = { stackName = it },
                        label = { Text("Stack") },
                        enabled = isEditing,
                        modifier = Modifier.weight(2f),
                        leadingIcon = { Icon(Icons.Filled.Layers, null) }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    OutlinedTextField(
                        value = stackOrder.toString(),
                        onValueChange = { stackOrder = it.toIntOrNull() ?: 1 },
                        label = { Text("Order") },
                        enabled = isEditing,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // -- APPEARANCE --
            item { SectionHeader("APPEARANCE", Icons.Filled.Palette) }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(selectedColor.copy(alpha = 0.2f))
                            .border(2.dp, selectedColor, CircleShape)
                            .clickable(enabled = isEditing) { /* Pick Icon/Color */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Circle, null, tint = selectedColor)
                    }
                }
            }

            // -- NOTES --
            item { SectionHeader("NOTES", Icons.Filled.Notes) }
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    enabled = isEditing,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    leadingIcon = { Icon(Icons.Filled.Notes, null) }
                )
            }

            // -- FREQUENCY --
            item { SectionHeader("FREQUENCY", Icons.Filled.Repeat) }
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = frequency == HabitFrequency.DAILY,
                        onClick = { if (isEditing) frequency = HabitFrequency.DAILY },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text("Daily")
                    }
                    SegmentedButton(
                        selected = frequency == HabitFrequency.WEEKLY_FLEXIBLE,
                        onClick = { if (isEditing) frequency = HabitFrequency.WEEKLY_FLEXIBLE },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text("Flexible")
                    }
                    SegmentedButton(
                        selected = frequency == HabitFrequency.WEEKLY_EXACT,
                        onClick = { if (isEditing) frequency = HabitFrequency.WEEKLY_EXACT },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text("Exact")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                if (frequency == HabitFrequency.WEEKLY_FLEXIBLE) {
                    Text("$targetDaysPerWeek Days per week", fontWeight = FontWeight.Bold)
                    Slider(
                        value = targetDaysPerWeek.toFloat(),
                        onValueChange = { if (isEditing) targetDaysPerWeek = it.toInt() },
                        valueRange = 1f..7f,
                        steps = 5,
                        colors = SliderDefaults.colors(thumbColor = selectedColor, activeTrackColor = selectedColor)
                    )
                } else if (frequency == HabitFrequency.WEEKLY_EXACT) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf(1 to "M", 2 to "T", 3 to "W", 4 to "T", 5 to "F", 6 to "S", 7 to "S").forEach { (day, label) ->
                            FilterChip(
                                selected = selectedWeekdays.contains(day),
                                onClick = {
                                    if (isEditing) {
                                        selectedWeekdays = if (selectedWeekdays.contains(day)) {
                                            selectedWeekdays - day
                                        } else {
                                            selectedWeekdays + day
                                        }
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            // -- TIME REMINDER --
            item { SectionHeader("REMINDER", Icons.Filled.NotificationsActive) }
            item {
                OutlinedTextField(
                    value = targetTimeMinutes?.let { "${it / 60}:${String.format("%02d", it % 60)}" } ?: "No Reminder",
                    onValueChange = {},
                    label = { Text("Reminder Time") },
                    enabled = false, // Stubbed picker
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isEditing) { /* Open Time Picker */ },
                    leadingIcon = { Icon(Icons.Filled.NotificationsActive, null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = if (isEditing) MaterialTheme.colorScheme.onSurface else Color.Gray,
                        disabledBorderColor = if (isEditing) MaterialTheme.colorScheme.outline else Color.Gray
                    )
                )
            }
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

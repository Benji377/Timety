package com.github.benji377.timety.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.benji377.timety.data.Task
import com.github.benji377.timety.ui.components.TaskCard
import com.github.benji377.timety.viewmodel.TasksViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
    onTaskClick: (Int) -> Unit,
    onAddTaskClick: () -> Unit
) {
    val tasks by viewModel.tasksByDate.collectAsState()
    val upcomingTasks by viewModel.upcomingTasks.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    // Calculate task count by date (normalized to start of day)
    val taskCountByDate = remember(tasks, upcomingTasks) {
        val map = mutableMapOf<Long, Int>()
        (tasks + upcomingTasks).forEach { task ->
            task.dueDate?.let { dueDate ->
                val cal = Calendar.getInstance().apply {
                    timeInMillis = dueDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val key = cal.timeInMillis
                map[key] = (map[key] ?: 0) + 1
            }
        }
        map
    }

    val datePickerState = rememberDatePickerState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks") },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Open Calendar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTaskClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            CalendarStrip(
                selectedDate = selectedDate,
                onDateSelected = { viewModel.selectDate(it) },
                taskCountByDate = taskCountByDate
            )
            
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                items(tasks) { task ->
                    TaskCard(
                        task = task,
                        onCheckedChange = { viewModel.toggleTaskStatus(task) },
                        onClick = { onTaskClick(task.id) }
                    )
                }

                if (upcomingTasks.isNotEmpty()) {
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        Text(
                            text = "Upcoming",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(upcomingTasks) { task ->
                        TaskCard(
                            task = task,
                            onCheckedChange = { viewModel.toggleTaskStatus(task) },
                            onClick = { onTaskClick(task.id) }
                        )
                    }
                }
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            viewModel.selectDate(it)
                        }
                        showDatePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

@Composable
fun CalendarStrip(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    taskCountByDate: Map<Long, Int> = emptyMap()
) {
    val days = remember {
        (0..13).map { i ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, i - 3) // Show 3 days before today
            cal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(days) { date ->
            val isSelected = isSameDay(date, selectedDate)
            val taskCount = taskCountByDate[date] ?: 0
            DayItem(
                date = date,
                isSelected = isSelected,
                taskCount = taskCount,
                onClick = { onDateSelected(date) }
            )
        }
    }
}

@Composable
fun DayItem(date: Long, isSelected: Boolean, taskCount: Int = 0, onClick: () -> Unit) {
    val cal = Calendar.getInstance().apply { timeInMillis = date }
    val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)
    val dayNumber = cal.get(Calendar.DAY_OF_MONTH).toString()

    Column(
        modifier = Modifier
            .width(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = dayName,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = dayNumber,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Task count dots
        if (taskCount > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(minOf(taskCount, 3)) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
                if (taskCount > 3) {
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        fontSize = androidx.compose.ui.unit.TextUnit.Unspecified
                    )
                }
            }
        }
    }
}

fun isSameDay(date1: Long, date2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}


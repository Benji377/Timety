package com.github.benji377.timety.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.benji377.timety.data.Task
import com.github.benji377.timety.ui.components.TaskCard
import com.github.benji377.timety.utils.DateUtils
import com.github.benji377.timety.viewmodel.TasksViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
    onTaskClick: (Int) -> Unit,
    onAddTaskClick: () -> Unit
) {
    val overdueTasks by viewModel.overdueTasks.collectAsState()
    val todayTasks by viewModel.todayTasks.collectAsState()
    val upcomingTasks by viewModel.upcomingTasksList.collectAsState()
    val doneTasks by viewModel.doneTasks.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var doneExpanded by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedTasks by remember { mutableStateOf(setOf<Task>()) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showMoveMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (isSelectionMode) {
                            Text("${selectedTasks.size} Selected")
                        } else {
                            TextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Search tasks...") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                singleLine = true
                            )
                        }
                    },
                    actions = {
                        if (isSelectionMode) {
                            IconButton(onClick = { showMoveMenu = true }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.DriveFileMove,
                                    contentDescription = "Move"
                                )
                            }
                            IconButton(onClick = {
                                viewModel.deleteTasks(selectedTasks.toList())
                                isSelectionMode = false
                                selectedTasks = emptySet()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                            IconButton(onClick = {
                                isSelectionMode = false
                                selectedTasks = emptySet()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                            }

                            DropdownMenu(
                                expanded = showMoveMenu,
                                onDismissRequest = { showMoveMenu = false }) {
                                categories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category.name) },
                                        onClick = {
                                            viewModel.moveTasksToCategory(
                                                selectedTasks.toList(),
                                                category.id
                                            )
                                            showMoveMenu = false
                                            isSelectionMode = false
                                            selectedTasks = emptySet()
                                        }
                                    )
                                }
                            }
                        } else {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }) {
                                TasksViewModel.SortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(order.name) },
                                        onClick = {
                                            viewModel.setSortOrder(order)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                )

                // Category Filter
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text("All") }
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategoryId == category.id,
                            onClick = { viewModel.selectCategory(category.id) },
                            label = { Text(category.name) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(onClick = onAddTaskClick) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // 1. Overdue
            if (overdueTasks.isNotEmpty()) {
                item {
                    Text(
                        "Overdue",
                        color = Color.Red,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(overdueTasks) { task ->
                    val isSelected = selectedTasks.contains(task)
                    TaskCard(
                        task = task,
                        onCheckedChange = {
                            if (isSelectionMode) {
                                selectedTasks =
                                    if (isSelected) selectedTasks - task else selectedTasks + task
                            } else {
                                viewModel.toggleTaskStatus(task)
                            }
                        },
                        onClick = {
                            if (isSelectionMode) {
                                selectedTasks =
                                    if (isSelected) selectedTasks - task else selectedTasks + task
                            } else {
                                onTaskClick(task.id)
                            }
                        },
                        onLongClick = {
                            isSelectionMode = true
                            selectedTasks = selectedTasks + task
                        },
                        isSelected = isSelected && isSelectionMode
                    )
                }
            }

            // 2. Today
            if (todayTasks.isNotEmpty()) {
                item {
                    Text(
                        "Today",
                        color = Color.Yellow,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(todayTasks) { task ->
                    val isSelected = selectedTasks.contains(task)
                    TaskCard(
                        task = task,
                        onCheckedChange = {
                            if (isSelectionMode) {
                                selectedTasks =
                                    if (isSelected) selectedTasks - task else selectedTasks + task
                            } else {
                                viewModel.toggleTaskStatus(task)
                            }
                        },
                        onClick = {
                            if (isSelectionMode) {
                                selectedTasks =
                                    if (isSelected) selectedTasks - task else selectedTasks + task
                            } else {
                                onTaskClick(task.id)
                            }
                        },
                        onLongClick = {
                            isSelectionMode = true
                            selectedTasks = selectedTasks + task
                        },
                        isSelected = isSelected && isSelectionMode
                    )
                }
            }

            // 3. Upcoming
            if (upcomingTasks.isNotEmpty()) {
                item {
                    Text(
                        "Upcoming",
                        color = Color.Blue,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(upcomingTasks) { task ->
                    val isSelected = selectedTasks.contains(task)
                    TaskCard(
                        task = task,
                        onCheckedChange = {
                            if (isSelectionMode) {
                                selectedTasks =
                                    if (isSelected) selectedTasks - task else selectedTasks + task
                            } else {
                                viewModel.toggleTaskStatus(task)
                            }
                        },
                        onClick = {
                            if (isSelectionMode) {
                                selectedTasks =
                                    if (isSelected) selectedTasks - task else selectedTasks + task
                            } else {
                                onTaskClick(task.id)
                            }
                        },
                        onLongClick = {
                            isSelectionMode = true
                            selectedTasks = selectedTasks + task
                        },
                        isSelected = isSelected && isSelectionMode
                    )
                }
            }

            // 4. Done
            if (doneTasks.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { doneExpanded = !doneExpanded }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Done",
                            color = Color.Green,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (doneExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (doneExpanded) "Collapse" else "Expand",
                            tint = Color.Green
                        )
                    }
                }
                if (doneExpanded) {
                    items(doneTasks) { task ->
                        val isSelected = selectedTasks.contains(task)
                        TaskCard(
                            task = task,
                            onCheckedChange = {
                                if (isSelectionMode) {
                                    selectedTasks =
                                        if (isSelected) selectedTasks - task else selectedTasks + task
                                } else {
                                    viewModel.toggleTaskStatus(task)
                                }
                            },
                            onClick = {
                                if (isSelectionMode) {
                                    selectedTasks =
                                        if (isSelected) selectedTasks - task else selectedTasks + task
                                } else {
                                    onTaskClick(task.id)
                                }
                            },
                            onLongClick = {
                                isSelectionMode = true
                                selectedTasks = selectedTasks + task
                            },
                            isSelected = isSelected && isSelectionMode
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(days) { date ->
            val isSelected = DateUtils.isSameDay(date, selectedDate)
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



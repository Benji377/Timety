package io.github.benji377.timety.ui.screens.task

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.data.model.task.Priority
import io.github.benji377.timety.data.model.task.Subtask
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.task.TaskSize
import io.github.benji377.timety.ui.theme.*
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String? = null,
    onNavigateBack: () -> Unit,
    taskViewModel: TaskViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val allTasks by taskViewModel.allTasks.collectAsState()
    val existingTask = taskId?.let { id -> allTasks.find { it.id == id } }

    var isEditing by remember { mutableStateOf(taskId == null) }

    // Form state
    var title by remember(existingTask) { mutableStateOf(existingTask?.title ?: "") }
    var description by remember(existingTask) { mutableStateOf(existingTask?.description ?: "") }
    var location by remember(existingTask) { mutableStateOf(existingTask?.location ?: "") }
    var priority by remember(existingTask) { mutableStateOf(existingTask?.priority ?: Priority.MEDIUM) }
    var size by remember(existingTask) { mutableStateOf(existingTask?.size ?: TaskSize.MEDIUM) }
    var category by remember(existingTask) { mutableStateOf(existingTask?.category ?: "") }
    var dueDate by remember(existingTask) { mutableStateOf(existingTask?.dueDate) }
    var subtasks by remember(existingTask) { mutableStateOf(existingTask?.subtasks ?: emptyList()) }
    var reminders by remember(existingTask) { mutableStateOf(existingTask?.reminders ?: emptyList()) }

    var newSubtaskTitle by remember { mutableStateOf("") }

    val appBarTitle = when {
        taskId == null -> "New Task"
        isEditing -> "Edit Task"
        else -> "Task Details"
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
                    if (!isEditing && taskId != null) {
                        IconButton(onClick = { 
                            taskViewModel.deleteTask(existingTask!!)
                            onNavigateBack()
                        }) {
                            Icon(Icons.Filled.DeleteOutline, "Delete", tint = ErrorColor)
                        }
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Filled.Edit, "Edit")
                        }
                    } else {
                        IconButton(onClick = {
                            if (title.isNotBlank()) {
                                val taskToSave = TaskEntity(
                                    id = taskId ?: UUID.randomUUID().toString(),
                                    title = title,
                                    description = description,
                                    location = location,
                                    priority = priority,
                                    size = size,
                                    category = category,
                                    dueDate = dueDate,
                                    subtasks = subtasks,
                                    reminders = reminders,
                                    isCompleted = existingTask?.isCompleted ?: false,
                                    completedAt = existingTask?.completedAt,
                                    createdAt = existingTask?.createdAt ?: Instant.now()
                                )
                                if (taskId == null) {
                                    taskViewModel.addTask(taskToSave)
                                } else {
                                    taskViewModel.updateTask(taskToSave)
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
            // -- THE BASICS --
            item { SectionHeader("THE BASICS", Icons.Filled.Info) }
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    enabled = isEditing,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Filled.Title, null) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Stub for Category Dropdown
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    enabled = isEditing,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Filled.Label, null) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    enabled = isEditing,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    leadingIcon = { Icon(Icons.Filled.Notes, null) }
                )
            }

            // -- PRIORITY & EFFORT --
            item { SectionHeader("PRIORITY & EFFORT", Icons.Filled.BarChart) }
            item {
                Text("Priority", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                // Simple Row for Priority
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Priority.values().forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { if (isEditing) priority = p },
                            label = { Text(p.name) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Effort / Size", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TaskSize.values().forEach { s ->
                        FilterChip(
                            selected = size == s,
                            onClick = { if (isEditing) size = s },
                            label = { Text(s.name) }
                        )
                    }
                }
            }

            // -- SCHEDULING --
            item { SectionHeader("SCHEDULING", Icons.Filled.CalendarToday) }
            item {
                OutlinedTextField(
                    value = dueDate?.toString() ?: "No Due Date",
                    onValueChange = {},
                    label = { Text("Due Date") },
                    enabled = false, // Stubbed picker
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isEditing) { /* Open Date Picker */ },
                    leadingIcon = { Icon(Icons.Filled.Event, null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = if (isEditing) MaterialTheme.colorScheme.onSurface else Color.Gray,
                        disabledBorderColor = if (isEditing) MaterialTheme.colorScheme.outline else Color.Gray
                    )
                )
            }

            // -- LOCATION --
            item { SectionHeader("LOCATION", Icons.Filled.LocationOn) }
            item {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    enabled = isEditing,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Filled.Map, null) }
                )
            }

            // -- CHECKLIST --
            item { SectionHeader("CHECKLIST", Icons.Filled.Checklist) }
            items(subtasks) { subtask ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = subtask.isCompleted,
                        onCheckedChange = { checked ->
                            subtasks = subtasks.map { 
                                if (it.id == subtask.id) it.copy(isCompleted = checked) else it 
                            }
                        }
                    )
                    Text(
                        text = subtask.title,
                        modifier = Modifier.weight(1f),
                        color = if (subtask.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                    )
                    if (isEditing) {
                        IconButton(onClick = { subtasks = subtasks.filter { it.id != subtask.id } }) {
                            Icon(Icons.Filled.Close, "Remove", tint = ErrorColor)
                        }
                    }
                }
            }
            item {
                if (isEditing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Icon(Icons.Filled.SubdirectoryArrowRight, null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(16.dp))
                        OutlinedTextField(
                            value = newSubtaskTitle,
                            onValueChange = { newSubtaskTitle = it },
                            placeholder = { Text("Add subtask...") },
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (newSubtaskTitle.isNotBlank()) {
                                        subtasks = subtasks + Subtask(UUID.randomUUID().toString(), newSubtaskTitle)
                                        newSubtaskTitle = ""
                                    }
                                }) {
                                    Icon(Icons.Filled.AddCircle, "Add", tint = TaskColor)
                                }
                            }
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
        )
        Spacer(modifier = Modifier.width(16.dp))
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

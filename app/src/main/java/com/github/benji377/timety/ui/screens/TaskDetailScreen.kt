package com.github.benji377.timety.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.benji377.timety.data.TaskStatus
import com.github.benji377.timety.viewmodel.TasksViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: Int,
    viewModel: TasksViewModel,
    onBack: () -> Unit,
    onStartFocus: (Int) -> Unit
) {
    val tasks by viewModel.allTasks.collectAsState()
    val task = tasks.find { it.id == taskId }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var priority by remember { mutableStateOf(com.github.benji377.timety.data.TaskPriority.MEDIUM) }
    var size by remember { mutableStateOf(com.github.benji377.timety.data.TaskSize.MEDIUM) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(task) {
        task?.let {
            title = it.title
            description = it.description ?: ""
            location = it.location ?: ""
            dueDate = it.dueDate
            priority = it.priority
            size = it.size
        }
    }

    val datePickerState = rememberDatePickerState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    task?.let {
                        IconButton(onClick = { 
                            viewModel.updateTask(it.copy(
                                title = title,
                                description = description.ifBlank { null },
                                location = location.ifBlank { null },
                                dueDate = dueDate,
                                priority = priority,
                                size = size
                            ))
                            onBack()
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                        IconButton(onClick = { 
                            viewModel.deleteTask(it)
                            onBack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { padding ->
        task?.let {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (dueDate == null) "Select Due Date" else SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(dueDate!!)))
                }

                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                dueDate = datePickerState.selectedDateMillis
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
                
                HorizontalDivider()
                
                Text(text = "Status: ${it.status}", style = MaterialTheme.typography.bodyMedium)
                
                // Priority and Size Display
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Priority", style = MaterialTheme.typography.labelSmall)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = priority.getIcon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(priority.label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Size", style = MaterialTheme.typography.labelSmall)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                com.github.benji377.timety.ui.components.TaskSizeBadge(size.badgeText)
                                Column {
                                    Text(size.label, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "~${size.estimatedMinutes} min",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Task Duration Tracking
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Focus Sessions", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "Total focus time on this task will appear here after you complete focus sessions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Note: In v1.2, this will show:
                        // - Total time spent on task
                        // - Number of sessions
                        // - Average session length
                        // - Comparison to estimated time
                    }
                }

                Button(
                    onClick = { onStartFocus(it.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Focus Session")
                }

                Button(
                    onClick = { viewModel.toggleTaskStatus(it) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (it.status == TaskStatus.DONE) "Mark as Todo" else "Mark as Done")
                }
            }
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Task not found")
            }
        }
    }
}

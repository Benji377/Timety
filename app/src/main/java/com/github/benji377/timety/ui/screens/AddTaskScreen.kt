package com.github.benji377.timety.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.benji377.timety.viewmodel.TasksViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    viewModel: TasksViewModel,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var reminders by remember { mutableStateOf(listOf<Long>()) }
    var selectedIcon by remember { mutableStateOf("Task") } // Default icon
    var selectedPriority by remember { mutableStateOf(com.github.benji377.timety.data.TaskPriority.MEDIUM) }
    var selectedSize by remember { mutableStateOf(com.github.benji377.timety.data.TaskSize.MEDIUM) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState()

    var showIconPicker by remember { mutableStateOf(false) }
    var showPriorityPicker by remember { mutableStateOf(false) }
    var showSizePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Task") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                // Note: addTask needs to be updated to accept priority and size
                                // For now using simplified call
                                viewModel.addTask(title, desc.ifBlank { null }, location.ifBlank { null }, dueDate, reminders, selectedIcon)
                                onBack()
                            }
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Icon:", modifier = Modifier.weight(1f))
                    Button(onClick = { showIconPicker = true }) {
                        Text(selectedIcon)
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Priority:", style = MaterialTheme.typography.labelMedium)
                        Button(
                            onClick = { showPriorityPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = selectedPriority.getIcon(),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(selectedPriority.label)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Size:", style = MaterialTheme.typography.labelMedium)
                        Button(
                            onClick = { showSizePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = selectedSize.getIcon(),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(selectedSize.label)
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                )
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = if (dueDate == null) "No due date" else SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).format(Date(dueDate!!)),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { showDatePicker = true }) {
                        Text(if (dueDate == null) "Set Date" else "Change")
                    }
                }
            }
            
            item {
                HorizontalDivider()
                Text("Reminders", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            }
            
            items(reminders) { reminder ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(reminder)),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { reminders = reminders - reminder }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
            
            item {
                TextButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Reminder")
                }
            }
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
        
        if (showTimePicker) {
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val cal = Calendar.getInstance()
                        // Default to today if no due date, or use due date
                        dueDate?.let { cal.timeInMillis = it }
                        cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        cal.set(Calendar.MINUTE, timePickerState.minute)
                        reminders = reminders + cal.timeInMillis
                        showTimePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text("Cancel")
                    }
                },
                text = {
                    TimePicker(state = timePickerState)
                }
            )
        }

        if (showIconPicker) {
            val iconOptions = listOf(
                "Task", "CheckCircle", "Schedule", "WorkspacePremium",
                "EmojiEvents", "Lightbulb", "School", "Code",
                "Book", "Favorite", "ShoppingCart", "LocalCafe"
            )
            AlertDialog(
                onDismissRequest = { showIconPicker = false },
                title = { Text("Select Icon") },
                text = {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(iconOptions) { icon ->
                            TextButton(
                                onClick = {
                                    selectedIcon = icon
                                    showIconPicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(icon, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }

        if (showPriorityPicker) {
            AlertDialog(
                onDismissRequest = { showPriorityPicker = false },
                title = { Text("Select Priority") },
                text = {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(com.github.benji377.timety.data.TaskPriority.values()) { priority ->
                            TextButton(
                                onClick = {
                                    selectedPriority = priority
                                    showPriorityPicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = priority.getIcon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(priority.label, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }

        if (showSizePicker) {
            AlertDialog(
                onDismissRequest = { showSizePicker = false },
                title = { Text("Select Size") },
                text = {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(com.github.benji377.timety.data.TaskSize.values()) { size ->
                            TextButton(
                                onClick = {
                                    selectedSize = size
                                    showSizePicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = size.getIcon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${size.label} (~${size.estimatedMinutes}min)", modifier = Modifier.weight(1f))
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
}

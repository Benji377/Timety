package io.github.benji377.timety.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Stubs for Settings State
    var use24HourFormat by remember { mutableStateOf(true) }
    var autoCompleteFocus by remember { mutableStateOf(false) }
    var dailyGoalMins by remember { mutableStateOf(120) }
    var showNumberDialog by remember { mutableStateOf<String?>(null) }
    
    var tempSliderValue by remember { mutableStateOf(120f) }

    if (showNumberDialog != null) {
        AlertDialog(
            onDismissRequest = { showNumberDialog = null },
            title = { Text(showNumberDialog!!) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${tempSliderValue.toInt()} mins", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Slider(
                        value = tempSliderValue,
                        onValueChange = { tempSliderValue = it },
                        valueRange = 10f..480f,
                        steps = 46
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    if (showNumberDialog == "Focus Goal") dailyGoalMins = tempSliderValue.toInt()
                    showNumberDialog = null 
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNumberDialog = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // --- APPEARANCE ---
            item { SettingsHeader("Appearance") }
            item {
                ListItem(
                    headlineContent = { Text("Theme") },
                    leadingContent = { Icon(Icons.Filled.DarkMode, null) },
                    trailingContent = { Text("System Default", color = Color.Gray) },
                    modifier = Modifier.clickable { /* Show theme dialog */ }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

            // --- LOCALE & FORMATTING ---
            item { SettingsHeader("Locale & Formatting") }
            item {
                ListItem(
                    headlineContent = { Text("Language") },
                    leadingContent = { Icon(Icons.Filled.Language, null, tint = TaskColor) },
                    trailingContent = { Text("System", color = Color.Gray) },
                    modifier = Modifier.clickable { /* Show language dialog */ }
                )
                ListItem(
                    headlineContent = { Text("24-Hour Time Format") },
                    trailingContent = { Switch(checked = use24HourFormat, onCheckedChange = { use24HourFormat = it }) },
                    leadingContent = { Icon(Icons.Filled.AccessTime, null, tint = FocusColor) }
                )
                ListItem(
                    headlineContent = { Text("Date Format") },
                    leadingContent = { Icon(Icons.Filled.CalendarToday, null, tint = HabitColor) },
                    trailingContent = { Text("System", color = Color.Gray) },
                    modifier = Modifier.clickable { /* Show format dialog */ }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

            // --- FOCUS & PRODUCTIVITY ---
            item { SettingsHeader("Focus & Productivity") }
            item {
                ListItem(
                    headlineContent = { Text("Daily Focus Goal") },
                    supportingContent = { Text("$dailyGoalMins mins") },
                    leadingContent = { Icon(Icons.Filled.TrackChanges, null, tint = FocusColor) },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
                    modifier = Modifier.clickable { 
                        tempSliderValue = dailyGoalMins.toFloat()
                        showNumberDialog = "Focus Goal" 
                    }
                )
                ListItem(
                    headlineContent = { Text("Auto-complete Targets") },
                    supportingContent = { Text("Mark task/habit as completed when session ends.") },
                    trailingContent = { Switch(checked = autoCompleteFocus, onCheckedChange = { autoCompleteFocus = it }) },
                    leadingContent = { Icon(Icons.Filled.TaskAlt, null, tint = TaskColor) }
                )
                ListItem(
                    headlineContent = { Text("Max Stopwatch Duration") },
                    supportingContent = { Text("120 mins") },
                    leadingContent = { Icon(Icons.Filled.Timer, null, tint = WarningColor) },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
                    modifier = Modifier.clickable { }
                )
                ListItem(
                    headlineContent = { Text("Upcoming Tasks Horizon") },
                    supportingContent = { Text("7 days") },
                    leadingContent = { Icon(Icons.Filled.Schedule, null, tint = TaskColor) },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
                    modifier = Modifier.clickable { }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

            // --- NOTIFICATIONS ---
            item { SettingsHeader("Notifications") }
            item {
                ListItem(
                    headlineContent = { Text("Daily Motivation") },
                    supportingContent = { Text("08:00") },
                    leadingContent = { Icon(Icons.Filled.Schedule, null, tint = WarningColor) },
                    trailingContent = { Icon(Icons.Filled.Edit, null) },
                    modifier = Modifier.clickable { }
                )
                ListItem(
                    headlineContent = { Text("End of Day Checkup") },
                    supportingContent = { Text("20:00") },
                    leadingContent = { Icon(Icons.Filled.NightlightRound, null, tint = HabitColor) },
                    trailingContent = { Icon(Icons.Filled.Edit, null) },
                    modifier = Modifier.clickable { }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

            // --- DATA & BACKUP ---
            item { SettingsHeader("Data & Backup") }
            item {
                ListItem(
                    headlineContent = { Text("Export Data") },
                    supportingContent = { Text("Save your data to a file") },
                    leadingContent = { Icon(Icons.Filled.UploadFile, null, tint = TaskColor) },
                    modifier = Modifier.clickable { }
                )
                ListItem(
                    headlineContent = { Text("Import Data") },
                    supportingContent = { Text("Load your data from a file") },
                    leadingContent = { Icon(Icons.Filled.Download, null, tint = FocusColor) },
                    modifier = Modifier.clickable { }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

            // --- SUPPORT ---
            item { SettingsHeader("Support & Feedback") }
            item {
                ListItem(
                    headlineContent = { Text("Community Discussions") },
                    supportingContent = { Text("Join the conversation on GitHub") },
                    leadingContent = { Icon(Icons.Filled.Forum, null, tint = FocusColor) },
                    trailingContent = { Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(16.dp), tint = Color.Gray) },
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Benji377/Timety/discussions"))
                        context.startActivity(intent)
                    }
                )
                ListItem(
                    headlineContent = { Text("Submit Feedback") },
                    supportingContent = { Text("Report a bug or suggest a feature") },
                    leadingContent = { Icon(Icons.Filled.BugReport, null, tint = HabitColor) },
                    trailingContent = { Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(16.dp), tint = Color.Gray) },
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Benji377/Timety/issues"))
                        context.startActivity(intent)
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

            // --- ABOUT ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, tint = TaskColor, modifier = Modifier.size(40.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Timety", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("v1.0.0", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        ListItem(
                            headlineContent = { Text("Built by Benji377") },
                            supportingContent = { Text("Maintainer") },
                            leadingContent = { Icon(Icons.Filled.Person, null, tint = Color(0xFFFF5722)) }
                        )
                        ListItem(
                            headlineContent = { Text("Source Code") },
                            supportingContent = { Text("View on GitHub") },
                            leadingContent = { Icon(Icons.Filled.Code, null, tint = Color.Blue) },
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Benji377/Timety"))
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.bodySmall.copy(
            color = TaskColor,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

package com.github.benji377.timety.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.benji377.timety.data.FocusRating
import com.github.benji377.timety.ui.components.RadialGraph
import com.github.benji377.timety.viewmodel.FocusViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(viewModel: FocusViewModel, taskId: Int? = null) {
    val timerMillis by viewModel.timerMillis.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val showReviewModal by viewModel.showReviewModal.collectAsState()
    val isStopwatchMode by viewModel.isStopwatchMode.collectAsState()

    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(categories) {
        if (selectedCategoryId == null && categories.isNotEmpty()) {
            selectedCategoryId = categories.first().id
        }
    }

    LaunchedEffect(taskId) {
        if (taskId != null) {
            // If we have a taskId, we might want to ensure a category is selected if possible
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Mode Toggle
            TabRow(
                selectedTabIndex = if (isStopwatchMode) 1 else 0,
                modifier = Modifier.width(200.dp),
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                divider = {}
            ) {
                Tab(
                    selected = !isStopwatchMode,
                    onClick = { viewModel.setMode(false) },
                    text = { Text("Timer") }
                )
                Tab(
                    selected = isStopwatchMode,
                    onClick = { viewModel.setMode(true) },
                    text = { Text("Stopwatch") }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            val minutes = (timerMillis / 1000) / 60
            val seconds = (timerMillis / 1000) % 60
            val timeText = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .clickable(enabled = !isRunning && !isStopwatchMode) {
                        showTimePicker = true
                    },
                contentAlignment = Alignment.Center
            ) {
                RadialGraph(
                    progress = if (!isStopwatchMode && !isRunning && timerMillis == 0L) 1f 
                              else if (isStopwatchMode) 1f 
                              else timerMillis.toFloat() / (25 * 60 * 1000L), // Simplified progress
                    text = timeText,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Start Button
                Button(
                    onClick = { selectedCategoryId?.let { viewModel.startTimer(it, taskId) } },
                    enabled = !isRunning && (isStopwatchMode || timerMillis > 0)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start")
                }

                // Pause Button
                Button(
                    onClick = { viewModel.pauseTimer() },
                    enabled = isRunning
                ) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pause")
                }

                // Stop Button
                Button(
                    onClick = { viewModel.stopTimerManual() },
                    enabled = isRunning || (!isRunning && timerMillis > 0 && isStopwatchMode),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop")
                }
            }

            if (showReviewModal) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissReview() },
                    title = { Text("How was your focus?") },
                    text = { Text("Rate your session to earn XP.") },
                    confirmButton = {
                        Column {
                            FocusRating.values().forEach { rating ->
                                Button(
                                    onClick = { viewModel.saveSession(rating) },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Text(rating.name)
                                }
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissReview() }) {
                            Text("Discard")
                        }
                    }
                )
            }

            if (showTimePicker) {
                // Simplified Time Picker Modal
                var inputMinutes by remember { mutableStateOf("25") }
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    title = { Text("Set Timer (minutes)") },
                    text = {
                        OutlinedTextField(
                            value = inputMinutes,
                            onValueChange = { inputMinutes = it.filter { char -> char.isDigit() } },
                            label = { Text("Minutes") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.setTimerDuration(inputMinutes.toIntOrNull() ?: 25)
                            showTimePicker = false
                        }) {
                            Text("Set")
                        }
                    }
                )
            }
            
            if (categories.isEmpty()) {
                Text(
                    "Please add a category in Settings first",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

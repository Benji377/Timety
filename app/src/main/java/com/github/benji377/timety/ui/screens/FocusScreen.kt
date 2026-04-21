package com.github.benji377.timety.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.benji377.timety.data.Category
import com.github.benji377.timety.data.FocusRating
import com.github.benji377.timety.data.FocusSession
import com.github.benji377.timety.data.FocusStepType
import com.github.benji377.timety.ui.components.RadialGraph
import com.github.benji377.timety.viewmodel.FocusViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)
@Composable
fun FocusScreen(
    viewModel: FocusViewModel,
    taskId: Int? = null,
    onNavigateToStats: () -> Unit,
    onNavigateToModes: () -> Unit
) {
    val timerMillis by viewModel.timerMillis.collectAsState()
    val targetMillis by viewModel.targetMillis.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val focusModes by viewModel.allFocusModes.collectAsState()
    val currentFocusMode by viewModel.currentFocusMode.collectAsState()
    val currentStepIndex by viewModel.currentStepIndex.collectAsState()
    val totalFocusTimeToday by viewModel.totalFocusTimeToday.collectAsState()
    val dailyTargetMins by viewModel.dailyTargetMins.collectAsState()
    val recentSessions by viewModel.recentSessions.collectAsState()
    val showReviewModal by viewModel.showReviewModal.collectAsState()

    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var showTimeMachine by remember { mutableStateOf(false) }
    var showAlerts by remember { mutableStateOf(false) }
    var showCategorySelector by remember { mutableStateOf(false) }

    LaunchedEffect(categories) {
        if (selectedCategoryId == null && categories.isNotEmpty()) {
            selectedCategoryId = categories.first().id
        }
    }

    LaunchedEffect(focusModes) {
        if (currentFocusMode == null && focusModes.isNotEmpty()) {
            viewModel.selectFocusMode(focusModes.first())
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header (Top Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Column(
                    modifier = Modifier.clickable { onNavigateToStats() },
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "Minutes Today:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${totalFocusTimeToday / 60000} / $dailyTargetMins",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Mode Selector Carousel
            if (focusModes.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { focusModes.size })

                LaunchedEffect(pagerState.currentPage) {
                    viewModel.selectFocusMode(focusModes[pagerState.currentPage])
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToModes() }
                    ) {
                        Text(
                            text = currentFocusMode?.title ?: "",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }

                    val currentStep = currentFocusMode?.steps?.getOrNull(currentStepIndex)
                    if (currentStep != null) {
                        Text(
                            text = if (currentStep.type == FocusStepType.REST) "REST" else "FOCUS",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (currentStep.type == FocusStepType.REST) Color.Blue else Color.Red,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        contentPadding = PaddingValues(horizontal = 32.dp)
                    ) { page ->
                        // The pager itself can be transparent or show mode names if needed
                        // But SPECS says dots indicate steps of the selected mode.
                    }

                    // Step indicator dots for CURRENT mode
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        currentFocusMode?.steps?.forEachIndexed { index, step ->
                            Box(
                                modifier = Modifier
                                    .size(if (index == currentStepIndex) 8.dp else 6.dp)
                                    .background(
                                        color = if (index == currentStepIndex) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Controls Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time Machine Button
                IconButton(
                    onClick = { showTimeMachine = true },
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    )
                ) {
                    Icon(Icons.Default.History, contentDescription = "Time Machine")
                }

                if (showTimeMachine) {
                    TimeMachineDialog(
                        sessions = recentSessions,
                        categories = categories,
                        onDismiss = { showTimeMachine = false },
                        onDeleteSession = { viewModel.deleteSession(it) },
                        onStartTimeMachine = { start, duration, categoryId, rating, note ->
                            viewModel.startTimeMachineSession(
                                start,
                                duration,
                                categoryId,
                                rating,
                                note
                            )
                        }
                    )
                }

                // Alerts Button
                IconButton(
                    onClick = { showAlerts = true },
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    )
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "Alerts")
                }

                if (showAlerts) {
                    AlertsDialog(
                        onDismiss = { showAlerts = false },
                        onLogEvent = { viewModel.logDailyEvent(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Timer Module
            var showManualTimerAdjust by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clickable(enabled = !isRunning) { showManualTimerAdjust = true },
                contentAlignment = Alignment.Center
            ) {
                val progress =
                    if (targetMillis > 0) 1f - (timerMillis.toFloat() / targetMillis) else 0f
                val minutes = (timerMillis / 1000) / 60
                val seconds = (timerMillis / 1000) % 60
                val timerText = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)

                RadialGraph(
                    progress = progress,
                    text = timerText,
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Green
                )

                if (showManualTimerAdjust) {
                    var inputMins by remember { mutableStateOf((timerMillis / 60000).toInt()) }
                    AlertDialog(
                        onDismissRequest = { showManualTimerAdjust = false },
                        title = { Text("Set Timer (Mins)") },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$inputMins",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Slider(
                                    value = inputMins.toFloat(),
                                    onValueChange = { inputMins = it.toInt() },
                                    valueRange = 1f..120f,
                                    steps = 119
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.setTimerDuration(inputMins)
                                showManualTimerAdjust = false
                            }) {
                                Text("Set")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showManualTimerAdjust = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.height(120.dp))
                    // Tag Selector Badge
                    val currentCategory = categories.find { it.id == selectedCategoryId }
                    Surface(
                        onClick = { showCategorySelector = true },
                        shape = RoundedCornerShape(16.dp),
                        color = currentCategory?.colorHex?.let {
                            Color(
                                android.graphics.Color.parseColor(
                                    it
                                )
                            )
                        }?.copy(alpha = 0.2f)
                            ?: MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = currentCategory?.name ?: "Select Tag",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    if (showCategorySelector) {
                        AlertDialog(
                            onDismissRequest = { showCategorySelector = false },
                            title = { Text("Select Tag") },
                            text = {
                                Column {
                                    categories.forEach { category ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedCategoryId = category.id
                                                    showCategorySelector = false
                                                }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .background(
                                                        Color(
                                                            android.graphics.Color.parseColor(
                                                                category.colorHex
                                                            )
                                                        ), CircleShape
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(category.name)
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showCategorySelector = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1.2f))

            // Main Action Buttons
            Box(
                modifier = Modifier.padding(bottom = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!isRunning) {
                    // Start Button
                    FloatingActionButton(
                        onClick = { viewModel.startTimer(selectedCategoryId ?: 0, taskId) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Start",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cancel Button
                        var showCancelConfirmation by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showCancelConfirmation = true },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        if (showCancelConfirmation) {
                            AlertDialog(
                                onDismissRequest = { showCancelConfirmation = false },
                                title = { Text("Cancel Focus Session?") },
                                text = { Text("Are you sure you want to cancel the current session? Progress will not be saved.") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.stopTimerManual()
                                        showCancelConfirmation = false
                                    }) {
                                        Text("Yes, Cancel")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showCancelConfirmation = false }) {
                                        Text("No, Keep Going")
                                    }
                                }
                            )
                        }

                        // Stop Button (Center)
                        var showStopConfirmation by remember { mutableStateOf(false) }
                        FloatingActionButton(
                            onClick = { showStopConfirmation = true },
                            containerColor = MaterialTheme.colorScheme.error,
                            shape = CircleShape,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "Stop",
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        if (showStopConfirmation) {
                            AlertDialog(
                                onDismissRequest = { showStopConfirmation = false },
                                title = { Text("Stop and Save Session?") },
                                text = { Text("Do you want to stop and record this session now?") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.stopTimerManual()
                                        showStopConfirmation = false
                                    }) {
                                        Text("Stop and Save")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showStopConfirmation = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }

                        // Pause Button
                        IconButton(
                            onClick = { viewModel.pauseTimer() },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Pause",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
            if (showReviewModal) {
                FocusReviewModal(
                    onDismiss = { viewModel.dismissReview() },
                    onSave = { rating, note -> viewModel.saveSession(rating, note) }
                )
            }
        }
    }
}

@Composable
fun TimeMachineDialog(
    sessions: List<FocusSession>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onDeleteSession: (FocusSession) -> Unit,
    onStartTimeMachine: (Long, Int, Int, FocusRating, String?) -> Unit
) {
    var durationMins by remember { mutableStateOf(25) }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 0) }
    var rating by remember { mutableStateOf(FocusRating.GREAT) }
    var note by remember { mutableStateOf("") }

    val endTimeStr = remember(durationMins) {
        val endTime = System.currentTimeMillis()
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(endTime))
    }

    var isEditing by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Add Retroactive Session" else "Recent Sessions") },
        text = {
            if (isEditing) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Duration: $durationMins mins",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = durationMins.toFloat(),
                        onValueChange = { durationMins = it.toInt() },
                        valueRange = 1f..240f
                    )

                    Text(
                        text = "Ends at: $endTimeStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Text("Category", style = MaterialTheme.typography.labelSmall)
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)) {
                        categories.forEach { category ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        Color(android.graphics.Color.parseColor(category.colorHex)),
                                        CircleShape
                                    )
                                    .clickable { selectedCategoryId = category.id }
                                    .padding(2.dp)
                            ) {
                                if (selectedCategoryId == category.id) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note (optional)") }
                    )
                }
            } else {
                if (sessions.isEmpty()) {
                    Text("No recent sessions found.")
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(sessions.size) { index ->
                            val session = sessions[index]
                            val category = categories.find { it.id == session.categoryId }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = category?.name ?: "Unknown",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${session.duration / 60000} mins",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                IconButton(onClick = { onDeleteSession(session) }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            if (index < sessions.size - 1) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isEditing) {
                Button(onClick = {
                    onStartTimeMachine(
                        System.currentTimeMillis() - (durationMins * 60 * 1000L),
                        durationMins,
                        selectedCategoryId,
                        rating,
                        note
                    )
                    onDismiss()
                }) {
                    Text("Add Session")
                }
            } else {
                Button(onClick = { isEditing = true }) {
                    Text("Add Past Session")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun AlertsDialog(onDismiss: () -> Unit, onLogEvent: (String) -> Unit) {
    val alerts = listOf("Distracted", "Stretch", "Drink", "Eye Break", "Posture")
    var selectedAlerts by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Log") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                alerts.forEach { alert ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selectedAlerts.contains(alert)) {
                                    selectedAlerts = selectedAlerts - alert
                                } else {
                                    selectedAlerts = selectedAlerts + alert
                                }
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedAlerts.contains(alert),
                            onCheckedChange = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(alert)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                selectedAlerts.forEach { onLogEvent(it) }
                onDismiss()
            }) {
                Text("Log Selected")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FocusReviewModal(
    onDismiss: () -> Unit,
    onSave: (FocusRating, String?) -> Unit
) {
    var rating by remember { mutableStateOf<FocusRating?>(null) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How was your focus?") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FocusRating.entries.forEach { r ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FilterChip(
                                selected = rating == r,
                                onClick = { rating = r },
                                label = { Text(r.name) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Add a note (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { rating?.let { onSave(it, note) } },
                enabled = rating != null
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}

@Composable
fun TimerCanvas(progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.padding(16.dp)) {
        val strokeWidth = 12.dp.toPx()
        // Gray background
        drawCircle(
            color = Color.LightGray.copy(alpha = 0.3f),
            style = Stroke(width = strokeWidth)
        )
        // Green progress
        drawArc(
            color = Color.Green,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            style = Stroke(width = strokeWidth)
        )
    }
}

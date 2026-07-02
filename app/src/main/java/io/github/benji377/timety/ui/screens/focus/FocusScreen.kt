package io.github.benji377.timety.ui.screens.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.ui.components.focus.InteractiveGauge
import io.github.benji377.timety.ui.components.focus.ModeTimeline
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.FocusViewModel

import io.github.benji377.timety.services.FocusTimerManager
import io.github.benji377.timety.services.FocusTimerService
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    onNavigateToModes: () -> Unit,
    focusViewModel: FocusViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val timerState by FocusTimerManager.timerState.collectAsState()

    var localProgress by remember { mutableStateOf(1.0f) }
    
    val isRunning = timerState.isRunning
    val isPaused = timerState.isPaused
    val activeModeName = timerState.modeName
    val gaugeProgress = if (isRunning || isPaused) timerState.progress else localProgress
    val centerText = if (isRunning || isPaused) timerState.centerText else "${(localProgress * 120).toInt()}:00"
    
    val focusMinsToday = 0
    val dailyTarget = 120

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focus", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = onNavigateToModes) {
                        Icon(Icons.Filled.DashboardCustomize, "Modes")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Focus Mode Selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = { /* Cycle backward */ },
                    enabled = !isRunning && !isPaused
                ) {
                    Icon(Icons.Filled.ArrowBackIosNew, "Previous", modifier = Modifier.size(20.dp))
                }
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .clickable(enabled = !isRunning && !isPaused, onClick = onNavigateToModes),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = activeModeName.uppercase(),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = if (isRunning || isPaused) MaterialTheme.colorScheme.onSurfaceVariant else FocusColor,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                IconButton(
                    onClick = { /* Cycle forward */ },
                    enabled = !isRunning && !isPaused
                ) {
                    Icon(Icons.Filled.ArrowForwardIos, "Next", modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Daily Goal Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(FocusColor.copy(alpha = 0.12f))
                    .border(1.dp, FocusColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .clickable { /* Go to Settings */ }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.TrackChanges, "Goal", tint = FocusColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$focusMinsToday / $dailyTarget m",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = FocusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Timer Gauge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                contentAlignment = Alignment.Center
            ) {
                InteractiveGauge(
                    progress = gaugeProgress,
                    isInteractive = !isRunning && !isPaused, // Simplified condition
                    label = "FOCUS",
                    centerText = centerText,
                    color = FocusColor,
                    centerTextColor = FocusColor,
                    labelColor = FocusColor,
                    bottomText = "No target selected",
                    bottomTextColor = FocusColor,
                    onChanged = { newProg -> 
                        localProgress = newProg
                        val totalSeconds = (newProg * 120 * 60).toInt()
                        // Note: To fully support this, FocusTimerManager should be updated with totalPhaseSeconds
                        // when the user slides the gauge and clicks Play.
                    }
                )

                // Time Machine Button
                Box(modifier = Modifier.fillMaxSize()) {
                    FilledTonalIconButton(
                        onClick = { /* Time Machine Dialog */ },
                        enabled = !isRunning && !isPaused,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Icon(Icons.Filled.History, "Time Machine", modifier = Modifier.size(28.dp))
                    }

                    // Distractions Button
                    FilledTonalIconButton(
                        onClick = { /* Distraction Dialog */ },
                        enabled = isRunning || isPaused,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Filled.WarningAmber, "Distractions", modifier = Modifier.size(28.dp))
                    }
                }
            }

            // Phase Timeline
            ModeTimeline()

            Spacer(modifier = Modifier.weight(1f))

            // Play/Pause Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Reset Button
                IconButton(
                    onClick = { 
                        val intent = Intent(context, FocusTimerService::class.java).apply { action = FocusTimerService.ACTION_STOP }
                        ContextCompat.startForegroundService(context, intent)
                    },
                    modifier = Modifier.size(64.dp),
                    enabled = isRunning || isPaused
                ) {
                    Icon(
                        Icons.Filled.RestartAlt, 
                        contentDescription = "Reset", 
                        modifier = Modifier.size(32.dp),
                        tint = if (isRunning || isPaused) Color.Gray else Color.Transparent
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Main Play/Stop Button
                Button(
                    onClick = {
                        val action = if (isRunning) FocusTimerService.ACTION_STOP else FocusTimerService.ACTION_START
                        val intent = Intent(context, FocusTimerService::class.java).apply { this.action = action }
                        ContextCompat.startForegroundService(context, intent)
                    },
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) ErrorColor else FocusColor
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        if (isRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = if (isRunning) "Stop" else "Play",
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Pause/Resume Button
                IconButton(
                    onClick = {
                        val action = if (isPaused) FocusTimerService.ACTION_START else FocusTimerService.ACTION_PAUSE
                        val intent = Intent(context, FocusTimerService::class.java).apply { this.action = action }
                        ContextCompat.startForegroundService(context, intent)
                    },
                    modifier = Modifier.size(64.dp),
                    enabled = isRunning || isPaused
                ) {
                    Icon(
                        if (isPaused) Icons.Filled.PlayCircleFilled else Icons.Filled.Pause, 
                        contentDescription = if (isPaused) "Resume" else "Pause", 
                        modifier = Modifier.size(32.dp),
                        tint = if (isRunning || isPaused) Color.Gray else Color.Transparent
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

package io.github.benji377.timety.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.UserColor
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit,
    userViewModel: UserViewModel = viewModel(factory = AppViewModelProvider.Factory),
    taskViewModel: io.github.benji377.timety.ui.viewmodel.TaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    habitViewModel: io.github.benji377.timety.ui.viewmodel.HabitViewModel = viewModel(factory = AppViewModelProvider.Factory),
    focusViewModel: io.github.benji377.timety.ui.viewmodel.FocusViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val userProfile by userViewModel.userProfile.collectAsState()
    val tasks by taskViewModel.allTasks.collectAsState()
    val habitsWithCompletions by habitViewModel.habitsWithCompletions.collectAsState()
    val sessions by focusViewModel.allSessions.collectAsState()
    val currentLevel by userViewModel.currentLevel.collectAsState()
    val levelTitle by userViewModel.levelTitle.collectAsState()
    val levelProgress by userViewModel.levelProgress.collectAsState()

    val userName = userProfile?.name ?: "User"
    val totalXp = userProfile?.totalXp ?: 0
    val levelThresholds = listOf(0, 100, 300, 600, 1000, 1500, 2100, 2800, 3600, 4500)
    val level = levelThresholds.indexOfLast { totalXp >= it }.coerceAtLeast(0) + 1
    val currentLevelXp = levelThresholds.getOrElse(level - 1) { 0 }
    val nextLevelXp = levelThresholds.getOrElse(level) { currentLevelXp + 1000 }
    val progress = if (nextLevelXp > currentLevelXp) (totalXp - currentLevelXp).toFloat() / (nextLevelXp - currentLevelXp) else 1f

    val totalTasksDone = tasks.count { it.task.isCompleted }
    val totalHabitsMet = habitsWithCompletions.sumOf { it.completions.size }
    val totalFocusMins = sessions.sumOf { it.totalSecondsFocused.toInt() } / 60
    val totalSessions = sessions.size
    val taskDates = tasks.mapNotNull { if (it.task.isCompleted) it.task.completedAt?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate() else null }
    val habitDates = habitsWithCompletions.flatMap { it.completions.map { c -> c.completionDate.atZone(java.time.ZoneId.systemDefault()).toLocalDate() } }
    val focusDates = sessions.map { java.time.LocalDateTime.ofInstant(it.startTime, java.time.ZoneId.systemDefault()).toLocalDate() }
    val allActivityDates = (taskDates + habitDates + focusDates).distinct().sorted()

    val currentStreak = io.github.benji377.timety.util.stats.StreakCalculator.calculateCurrentStreak(allActivityDates)
    val highestStreak = io.github.benji377.timety.util.stats.StreakCalculator.calculateBestStreak(allActivityDates)

    val context = LocalContext.current
    var showEditNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }
    var showShareWrapupDialog by remember { mutableStateOf(false) }

    if (showShareWrapupDialog) {
        AlertDialog(
            onDismissRequest = { showShareWrapupDialog = false },
            title = { Text("Share Wrap-Up") },
            text = {
                Column {
                    Text("Here's a summary of your achievements:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${stringResource(R.string.userStatTasksDone)}: $totalTasksDone")
                    Text("${stringResource(R.string.userStatHabitsMet)}: $totalHabitsMet")
                    Text("${stringResource(R.string.userStatFocusMins)}: $totalFocusMins")
                    Text("Highest Streak: $highestStreak")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Image generation and sharing intent coming soon!")
                }
            },
            confirmButton = {
                TextButton(onClick = { showShareWrapupDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text(stringResource(R.string.userEditNameTitle)) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    userViewModel.updateName(tempName)
                    showEditNameDialog = false
                }) {
                    Text(stringResource(R.string.commonLabelSave))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text(stringResource(R.string.commonLabelCancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.userProfileTitle), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showShareWrapupDialog = true }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share Wrap-up")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settingsTitle))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(24.dp)) }

            // Avatar Section
            item {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(UserColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Avatar",
                            modifier = Modifier.size(60.dp),
                            tint = UserColor
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(UserColor)
                            .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .clickable { Toast.makeText(context, "Image Picker coming soon", Toast.LENGTH_SHORT).show() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = "Edit Avatar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // Name Section
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = {
                        tempName = userName
                        showEditNameDialog = true
                    }) {
                        Icon(Icons.Filled.Edit, stringResource(R.string.userEditNameTitle), tint = Color.Gray)
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Streak Badge
            item {
                Surface(
                    color = WarningColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.LocalFireDepartment, contentDescription = "Streak", tint = WarningColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "$highestStreak Day Streak!", color = WarningColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // XP Card
            item {
                io.github.benji377.timety.ui.components.user.UserXpBreakdownCard(
                    currentLevel = currentLevel,
                    levelTitle = levelTitle,
                    totalXp = totalXp,
                    levelProgress = levelProgress
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
            
            // Streak Timeline Card Placeholder
            item {
                io.github.benji377.timety.ui.components.user.UserStreakTimelineCard(
                    activityDates = allActivityDates,
                    taskDates = taskDates,
                    focusDates = focusDates,
                    habitDates = habitDates,
                    currentStreak = currentStreak,
                    highestStreak = highestStreak
                )
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }

            // All-Time Statistics Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "All-Time Statistics",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatCard(title = stringResource(R.string.userStatTasksDone), value = totalTasksDone.toString(), modifier = Modifier.weight(1f))
                        StatCard(title = stringResource(R.string.userStatHabitsMet), value = totalHabitsMet.toString(), modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatCard(title = stringResource(R.string.userStatFocusMins), value = totalFocusMins.toString(), modifier = Modifier.weight(1f))
                        StatCard(title = stringResource(R.string.userStatSessions), value = totalSessions.toString(), modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatCard(title = stringResource(R.string.streakLegendStreakDay), value = highestStreak.toString(), modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


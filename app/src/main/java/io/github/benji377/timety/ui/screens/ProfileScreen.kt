package io.github.benji377.timety.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.github.benji377.timety.ui.components.common.TimetyTopBar
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.UserColor
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.UserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import io.github.benji377.timety.ui.components.common.TimetyOutlinedTextField as OutlinedTextField
import io.github.benji377.timety.ui.components.stats.StatCard
import io.github.benji377.timety.ui.components.stats.StatCardStyle
import io.github.benji377.timety.ui.components.user.StreakStatusBadge
import io.github.benji377.timety.ui.components.user.UserStreakTimelineCard
import io.github.benji377.timety.ui.components.user.UserXpBreakdownCard
import io.github.benji377.timety.ui.utils.WrapUpImageGenerator
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.ui.viewmodel.HabitViewModel
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import io.github.benji377.timety.util.stats.StreakCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit,
    userViewModel: UserViewModel = viewModel(factory = AppViewModelProvider.Factory),
    taskViewModel: TaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    habitViewModel: HabitViewModel = viewModel(factory = AppViewModelProvider.Factory),
    focusViewModel: FocusViewModel = io.github.benji377.timety.ui.viewmodel.activityScopedViewModel()
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
    val progress =
        if (nextLevelXp > currentLevelXp) (totalXp - currentLevelXp).toFloat() / (nextLevelXp - currentLevelXp) else 1f

    val totalTasksDone = tasks.count { it.task.isCompleted }
    val totalHabitsMet = habitsWithCompletions.sumOf { it.completions.size }
    val totalFocusMins = sessions.sumOf { it.totalSecondsFocused } / 60
    val totalSessions = sessions.size
    val taskDates = tasks.mapNotNull {
        if (it.task.isCompleted) it.task.completedAt?.atZone(java.time.ZoneId.systemDefault())
            ?.toLocalDate() else null
    }
    val habitDates = habitsWithCompletions.flatMap {
        it.completions.map { c ->
            c.completionDate.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        }
    }
    val focusDates = sessions.map {
        java.time.LocalDateTime.ofInstant(
            it.startTime,
            java.time.ZoneId.systemDefault()
        ).toLocalDate()
    }
    val allActivityDates = (taskDates + habitDates + focusDates).distinct().sorted()

    val currentStreak =
        StreakCalculator.calculateCurrentStreak(
            allActivityDates
        )
    val highestStreak =
        StreakCalculator.calculateBestStreak(allActivityDates)

    val context = LocalContext.current
    val shareSubjectStr = stringResource(R.string.userShareSubject)
    val shareTextStr = stringResource(R.string.userShareText)
    val shareWrapUpTooltipStr = stringResource(R.string.userTooltipShareWrapUp)
    val errorGenerateImageStr = stringResource(R.string.userErrorGenerateImage)
    var showEditNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }
    var showShareWrapupDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            userViewModel.updateProfileImage(it.toString())
        }
    }
    val coroutineScope = rememberCoroutineScope()

    if (showShareWrapupDialog) {
        AlertDialog(
            onDismissRequest = { showShareWrapupDialog = false },
            title = { Text(stringResource(R.string.userTooltipShareWrapUp)) },
            text = {
                Column {
                    Text(stringResource(R.string.userShareSummary), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${stringResource(R.string.userStatTasksDone)}: $totalTasksDone")
                    Text("${stringResource(R.string.userStatHabitsMet)}: $totalHabitsMet")
                    Text("${stringResource(R.string.userStatFocusMins)}: $totalFocusMins")
                    Text("${stringResource(R.string.userStatBestStreak)}: $highestStreak")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showShareWrapupDialog = false
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val bytes =
                                WrapUpImageGenerator.generate(
                                    context,
                                    userName,
                                    level,
                                    levelTitle,
                                    highestStreak,
                                    totalTasksDone,
                                    totalFocusMins,
                                    totalHabitsMet
                                )
                            val cachePath = File(context.cacheDir, "shared")
                            cachePath.mkdirs()
                            val file = File(cachePath, "wrapup.png")
                            file.writeBytes(bytes)
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )

                            withContext(Dispatchers.Main) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(
                                        Intent.EXTRA_SUBJECT,
                                        shareSubjectStr
                                    )
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        shareTextStr
                                    )
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(
                                        intent,
                                        shareWrapUpTooltipStr
                                    )
                                )
                            }
                        } catch (_: Exception) {
                            withContext(Dispatchers.Main) {
                                android.widget.Toast.makeText(
                                    context,
                                    errorGenerateImageStr,
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }) {
                    Text(stringResource(R.string.commonLabelShare))
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareWrapupDialog = false }) {
                    Text(stringResource(R.string.commonLabelClose))
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
                    placeholder = { Text(stringResource(R.string.userEditNameHint)) },
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
            TimetyTopBar(
                title = stringResource(R.string.userProfileTitle),
                actions = {
                    IconButton(onClick = { showShareWrapupDialog = true }) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = stringResource(R.string.userTooltipShareWrapUp)
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settingsTitle)
                        )
                    }
                }
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
                        if (userProfile?.profileImagePath != null) {
                            AsyncImage(
                                model = userProfile?.profileImagePath,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = UserColor
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(UserColor)
                            .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = stringResource(R.string.userTooltipEditAvatar),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // Name Section (streak flame sits left of the name)
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StreakStatusBadge(isActive = currentStreak > 0)
                    Spacer(modifier = Modifier.width(8.dp))
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
                        Icon(
                            Icons.Filled.Edit,
                            stringResource(R.string.userEditNameTitle),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // XP Card
            item {
                UserXpBreakdownCard(
                    currentLevel = currentLevel,
                    levelTitle = levelTitle,
                    totalXp = totalXp,
                    levelProgress = levelProgress
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // Streak Timeline Card Placeholder
            item {
                UserStreakTimelineCard(
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
                        text = stringResource(R.string.userStatsAllTimeTitle),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.userStatsAllTimeSubtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatCard(
                            title = stringResource(R.string.userStatTasksDone),
                            value = totalTasksDone.toString(),
                            icon = Icons.Outlined.CheckCircle,
                            color = TaskColor,
                            style = StatCardStyle.COMPACT_VERTICAL,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = stringResource(R.string.userStatHabitsMet),
                            value = totalHabitsMet.toString(),
                            icon = Icons.Outlined.Repeat,
                            color = HabitColor,
                            style = StatCardStyle.COMPACT_VERTICAL,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatCard(
                            title = stringResource(R.string.userStatFocusMins),
                            value = totalFocusMins.toString(),
                            icon = Icons.Outlined.Timer,
                            color = FocusColor,
                            style = StatCardStyle.COMPACT_VERTICAL,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = stringResource(R.string.userStatSessions),
                            value = totalSessions.toString(),
                            icon = Icons.Outlined.Coffee,
                            color = FocusColor,
                            style = StatCardStyle.COMPACT_VERTICAL,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatCard(
                            title = stringResource(R.string.userStatBestStreak),
                            value = highestStreak.toString(),
                            icon = Icons.Outlined.MilitaryTech,
                            color = WarningColor,
                            style = StatCardStyle.COMPACT_VERTICAL,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}


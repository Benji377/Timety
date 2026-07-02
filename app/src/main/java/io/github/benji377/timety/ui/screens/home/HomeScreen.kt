package io.github.benji377.timety.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.ui.components.common.StyledExpansionTile
import io.github.benji377.timety.ui.components.focus.InteractiveGauge
import io.github.benji377.timety.ui.components.habit.HabitListTile
import io.github.benji377.timety.ui.components.task.TaskListTile
import io.github.benji377.timety.ui.theme.*
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.ui.viewmodel.HabitViewModel
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import io.github.benji377.timety.ui.viewmodel.UserViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    taskViewModel: TaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    habitViewModel: HabitViewModel = viewModel(factory = AppViewModelProvider.Factory),
    userViewModel: UserViewModel = viewModel(factory = AppViewModelProvider.Factory),
    focusViewModel: FocusViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onNavigateToFocus: () -> Unit,
    onNavigateToTaskDetail: (String?) -> Unit,
    onNavigateToHabitDetail: (String?) -> Unit
) {
    val tasks by taskViewModel.allTasks.collectAsState()
    val habits by habitViewModel.allHabits.collectAsState()
    val userProfile by userViewModel.userProfile.collectAsState()

    val userName = userProfile?.name ?: "User"
    val dailyTarget = 120 // TODO: From Settings
    val focusMinsToday = 0 // TODO: calculate from focus sessions today
    val focusProgress = (focusMinsToday.toFloat() / dailyTarget.toFloat()).coerceIn(0f, 1f)

    val today = Instant.now().truncatedTo(ChronoUnit.DAYS)

    val urgentTasks = tasks.filter { task ->
        !task.isCompleted && task.dueDate != null && !task.dueDate.isAfter(today)
    }.sortedBy { it.dueDate }

    val upcomingTasks = tasks.filter { task ->
        !task.isCompleted && task.dueDate != null && task.dueDate.isAfter(today)
    }.sortedBy { it.dueDate }

    // Simplifying today's habits logic for UI structure
    val todaysHabits = habits

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timety", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToTaskDetail(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, "Add Task")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Greeting Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Good day, $userName!",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ready to conquer your goals?",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TaskColor
                    )
                )
            }

            // Gauge Section
            Box(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxWidth()
                    .clickable { onNavigateToFocus() },
                contentAlignment = Alignment.Center
            ) {
                InteractiveGauge(
                    progress = focusProgress,
                    label = "DAILY GOAL",
                    centerText = "${(focusProgress * 100).toInt()}%",
                    centerTextColor = FocusColor,
                    color = FocusColor,
                    bottomText = "$focusMinsToday / $dailyTarget m",
                    bottomTextColor = FocusColor
                )
            }

            Divider()

            // Lists Section
            LazyColumn(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (urgentTasks.isNotEmpty()) {
                    item {
                        StyledExpansionTile(
                            title = "${urgentTasks.size} Tasks Due",
                            titleColor = WarningColor,
                            initiallyExpanded = true
                        ) {
                            urgentTasks.forEach { task ->
                                TaskListTile(
                                    task = task,
                                    onToggleCompleted = { taskViewModel.toggleTaskCompletion(task) },
                                    onTap = { onNavigateToTaskDetail(task.id) }
                                    // Disable swipe to delete on homescreen
                                )
                            }
                        }
                    }
                }

                if (todaysHabits.isNotEmpty()) {
                    item {
                        StyledExpansionTile(
                            title = "${todaysHabits.size} Habits Today",
                            titleColor = HabitColor,
                            initiallyExpanded = true
                        ) {
                            todaysHabits.forEach { habit ->
                                HabitListTile(
                                    habit = habit,
                                    isCompleted = false,
                                    subtitleText = habit.frequency.name,
                                    onToggleCompleted = { habitViewModel.logCompletion(habit.id) },
                                    onTap = { onNavigateToHabitDetail(habit.id) }
                                )
                            }
                        }
                    }
                }

                if (upcomingTasks.isNotEmpty()) {
                    item {
                        StyledExpansionTile(
                            title = "${upcomingTasks.size} Tasks Upcoming",
                            titleColor = TaskColor,
                            initiallyExpanded = false
                        ) {
                            upcomingTasks.forEach { task ->
                                TaskListTile(
                                    task = task,
                                    onToggleCompleted = { taskViewModel.toggleTaskCompletion(task) },
                                    onTap = { onNavigateToTaskDetail(task.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

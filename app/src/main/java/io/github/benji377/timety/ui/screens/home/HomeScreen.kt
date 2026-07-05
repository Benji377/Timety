package io.github.benji377.timety.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.components.common.StyledExpansionTile
import io.github.benji377.timety.ui.components.focus.InteractiveGauge
import io.github.benji377.timety.ui.components.habit.HabitListTile
import io.github.benji377.timety.ui.components.task.TaskListTile
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.ui.viewmodel.HabitViewModel
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import io.github.benji377.timety.ui.viewmodel.UserViewModel
import io.github.benji377.timety.util.habit.HabitUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * The main dashboard screen showing today's overview, goals, and upcoming tasks.
 * Mirrors `screens/home_screen.dart`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    taskViewModel: TaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    habitViewModel: HabitViewModel = viewModel(factory = AppViewModelProvider.Factory),
    userViewModel: UserViewModel = viewModel(factory = AppViewModelProvider.Factory),
    focusViewModel: FocusViewModel = viewModel(factory = AppViewModelProvider.Factory),
    settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onNavigateToFocus: () -> Unit,
    onNavigateToTaskDetail: (String?) -> Unit,
    onNavigateToHabitDetail: (String?) -> Unit,
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToStatistics: () -> Unit = {}
) {
    val tasks by taskViewModel.allTasks.collectAsState()
    val habitsWithCompletions by habitViewModel.habitsWithCompletions.collectAsState()
    val userProfile by userViewModel.userProfile.collectAsState()
    val sessions by focusViewModel.allSessions.collectAsState()
    val dailyTarget by settingsViewModel.dailyGoalMins.collectAsState()
    val upcomingWindowDays by settingsViewModel.upcomingTasksHorizon.collectAsState()

    val userName = userProfile?.name ?: "User"

    val todayLocalDate = LocalDate.now()
    val focusMinsToday = sessions.filter {
        LocalDateTime.ofInstant(it.startTime, ZoneId.systemDefault())
            .toLocalDate() == todayLocalDate
    }.sumOf { it.totalSecondsFocused.toInt() } / 60
    val focusProgress =
        if (dailyTarget > 0) (focusMinsToday.toFloat() / dailyTarget.toFloat()).coerceIn(
            0f,
            1f
        ) else 0f

    val currentHour = LocalTime.now().hour
    val greeting = when (currentHour) {
        in 0..4 -> stringResource(R.string.greetingDeepNight, userName)
        in 5..11 -> stringResource(R.string.greetingMorning, userName)
        in 12..16 -> stringResource(R.string.greetingAfternoon, userName)
        in 17..20 -> stringResource(R.string.greetingEvening, userName)
        else -> stringResource(R.string.greetingNight, userName)
    }
    val motivation = when (currentHour) {
        in 0..4 -> stringResource(R.string.greetingDeepNightMotivation)
        in 5..11 -> stringResource(R.string.greetingMorningMotivation)
        in 12..16 -> stringResource(R.string.greetingAfternoonMotivation)
        in 17..20 -> stringResource(R.string.greetingEveningMotivation)
        else -> stringResource(R.string.greetingNightMotivation)
    }

    // Urgent tasks: incomplete tasks whose due day is today or earlier. Mirrors home_screen.dart.
    val urgentTasks = tasks.filter { t ->
        val dueDay = t.task.dueDate?.atZone(ZoneId.systemDefault())?.toLocalDate()
        !t.task.isCompleted && dueDay != null && !dueDay.isAfter(todayLocalDate)
    }.sortedBy { it.task.dueDate }

    // Upcoming tasks: incomplete tasks due strictly after today, within the configured horizon window.
    val upcomingEndDate = todayLocalDate.plusDays(upcomingWindowDays.toLong())
    val upcomingTasks = tasks.filter { t ->
        val dueDay = t.task.dueDate?.atZone(ZoneId.systemDefault())?.toLocalDate()
        !t.task.isCompleted && dueDay != null && dueDay.isAfter(todayLocalDate) && !dueDay.isAfter(
            upcomingEndDate
        )
    }.sortedBy { it.task.dueDate }

    // Today's habits: scheduled for today and not yet at their weekly target (excluding today's own completion).
    val todaysHabits = habitsWithCompletions.filter { HabitUtils.isHabitDueToday(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.appTitle), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToStatistics) {
                        Icon(
                            Icons.Filled.BarChart,
                            contentDescription = stringResource(R.string.commonTooltipStats)
                        )
                    }
                    IconButton(onClick = onNavigateToCalendar) {
                        Icon(
                            Icons.Filled.CalendarToday,
                            contentDescription = stringResource(R.string.commonTooltipCalendar)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToTaskDetail(null) },
                modifier = Modifier.border(
                    io.github.benji377.timety.ui.theme.AppTheme.neoBorderWidth,
                    androidx.compose.material3.MaterialTheme.colorScheme.outline,
                    io.github.benji377.timety.ui.theme.AppTheme.brNeo
                ),
                shape = io.github.benji377.timety.ui.theme.AppTheme.brNeo,
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                    0.dp,
                    0.dp,
                    0.dp,
                    0.dp
                ),
                containerColor = io.github.benji377.timety.ui.theme.TaskColor,
                contentColor = androidx.compose.ui.graphics.Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.commonLabelAdd))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- GREETING & MOTIVATION SECTION ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = greeting,
                    fontSize = AppTheme.fsHeadingLarge,
                    fontWeight = AppTheme.fwExtraBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = motivation,
                    fontSize = AppTheme.fsBodyLarge,
                    fontWeight = AppTheme.fwBold,
                    color = TaskColor
                )
            }

            // --- DAILY GOAL GAUGE SECTION ---
            Box(
                modifier = Modifier
                    .weight(5f)
                    .fillMaxWidth()
                    .clickable { onNavigateToFocus() },
                contentAlignment = Alignment.Center
            ) {
                InteractiveGauge(
                    progress = focusProgress,
                    isInteractive = false,
                    label = stringResource(R.string.homeDailyGoal).uppercase(),
                    centerText = "${(focusProgress * 100).toInt()}%",
                    centerTextColor = FocusColor,
                    color = FocusColor,
                    bottomText = "$focusMinsToday / $dailyTarget m",
                    bottomTextColor = FocusColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()

            // --- TASKS & HABITS LIST SECTION ---
            Box(
                modifier = Modifier
                    .weight(6f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                if (urgentTasks.isEmpty() && todaysHabits.isEmpty()) {
                    Text(
                        text = stringResource(R.string.homeDailyGoalDone),
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
                    ) {
                        // --- DUE TASKS ACCORDION ---
                        if (urgentTasks.isNotEmpty()) {
                            item {
                                StyledExpansionTile(
                                    title = stringResource(
                                        R.string.homeSectionTasksDue,
                                        urgentTasks.size
                                    ),
                                    titleColor = WarningColor,
                                    initiallyExpanded = true
                                ) {
                                    urgentTasks.forEach { task ->
                                        TaskListTile(
                                            task = task.task,
                                            isOverdue = task.task.dueDate != null &&
                                                    task.task.dueDate.atZone(ZoneId.systemDefault())
                                                        .toLocalDate().isBefore(todayLocalDate),
                                            enableDismissible = false,
                                            showDescription = false,
                                            subtasksCompleted = task.subtasks.count { it.isCompleted },
                                            subtasksTotal = task.subtasks.size,
                                            onToggleCompleted = {
                                                taskViewModel.toggleTaskCompletion(
                                                    task.task
                                                )
                                            },
                                            onTap = { onNavigateToTaskDetail(task.task.id) }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }

                        // --- TODAY'S HABITS ACCORDION ---
                        if (todaysHabits.isNotEmpty()) {
                            item {
                                StyledExpansionTile(
                                    title = stringResource(
                                        R.string.homeSectionHabitsDue,
                                        todaysHabits.size
                                    ),
                                    titleColor = HabitColor,
                                    initiallyExpanded = false
                                ) {
                                    io.github.benji377.timety.ui.components.habit.GroupedHabitsSection(
                                        habits = todaysHabits,
                                        allHabitsForStacks = habitsWithCompletions,
                                        targetDate = todayLocalDate,
                                        habitBuilder = { hc, isDone, isStacked, isLocked ->
                                            val completionsThisWeek =
                                                HabitUtils.getCompletionsThisWeek(hc)
                                            HabitListTile(
                                                habit = hc.habit,
                                                isCompleted = isDone,
                                                isStacked = isStacked,
                                                isLocked = isLocked,
                                                subtitleText = HabitUtils.buildHabitSubtitle(
                                                    hc.habit,
                                                    completionsThisWeek
                                                ),
                                                enableDismissible = false,
                                                onToggleCompleted = {
                                                    habitViewModel.toggleCompletionToday(
                                                        hc.habit.id
                                                    )
                                                },
                                                onTap = { onNavigateToHabitDetail(hc.habit.id) }
                                            )
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }

                        // --- UPCOMING TASKS ACCORDION ---
                        if (upcomingTasks.isNotEmpty()) {
                            item {
                                StyledExpansionTile(
                                    title = stringResource(
                                        R.string.homeSectionTasksUpcoming,
                                        upcomingTasks.size
                                    ),
                                    titleColor = TaskColor,
                                    initiallyExpanded = false
                                ) {
                                    upcomingTasks.forEach { task ->
                                        TaskListTile(
                                            task = task.task,
                                            enableDismissible = false,
                                            showDescription = false,
                                            subtasksCompleted = task.subtasks.count { it.isCompleted },
                                            subtasksTotal = task.subtasks.size,
                                            onToggleCompleted = {
                                                taskViewModel.toggleTaskCompletion(
                                                    task.task
                                                )
                                            },
                                            onTap = { onNavigateToTaskDetail(task.task.id) }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

package io.github.benji377.timety.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.components.common.StyledExpansionTile
import io.github.benji377.timety.ui.components.common.TimetyFab
import io.github.benji377.timety.ui.components.common.TimetyTopBar
import io.github.benji377.timety.ui.components.habit.GroupedHabitsSection
import io.github.benji377.timety.ui.components.habit.HabitListTile
import io.github.benji377.timety.ui.components.task.RecurringTaskListTile
import io.github.benji377.timety.ui.components.task.TaskListTile
import io.github.benji377.timety.ui.components.task.rememberRecurringCompleter
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.LocalSnackbarHostState
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.ui.viewmodel.HabitViewModel
import io.github.benji377.timety.ui.viewmodel.RecurringTaskViewModel
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import io.github.benji377.timety.ui.viewmodel.UserViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import io.github.benji377.timety.util.habit.HabitUtils
import io.github.benji377.timety.util.task.RecurrenceUtils
import io.github.benji377.timety.util.task.RecurringStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.roundToInt


/**
 * Home screen: greeting, a compact daily focus-goal progress card, and accordion lists of due
 * tasks, today's habits, and upcoming tasks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    taskViewModel: TaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    habitViewModel: HabitViewModel = viewModel(factory = AppViewModelProvider.Factory),
    userViewModel: UserViewModel = viewModel(factory = AppViewModelProvider.Factory),
    focusViewModel: FocusViewModel = activityScopedViewModel(),
    settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    recurringViewModel: RecurringTaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onNavigateToFocus: () -> Unit,
    onNavigateToTaskDetail: (String?) -> Unit,
    onNavigateToHabitDetail: (String?) -> Unit,
    onNavigateToRecurringDetail: (String) -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToStatistics: () -> Unit = {}
) {
    val tasks by taskViewModel.allTasks.collectAsState()
    val habitsWithCompletions by habitViewModel.habitsWithCompletions.collectAsState()
    val userProfile by userViewModel.userProfile.collectAsState()
    val sessions by focusViewModel.allSessions.collectAsState()
    val dailyTarget by settingsViewModel.dailyGoalMins.collectAsState()
    val upcomingWindowDays by settingsViewModel.upcomingTasksHorizon.collectAsState()
    val recurringItems by recurringViewModel.allRecurringTasks.collectAsState()
    val completeRecurring =
        rememberRecurringCompleter(recurringViewModel, LocalSnackbarHostState.current)

    val userName = userProfile?.name ?: "User"

    val todayLocalDate = LocalDate.now()
    val focusMinsToday = remember(sessions, todayLocalDate) {
        sessions.filter {
            LocalDateTime.ofInstant(it.startTime, ZoneId.systemDefault())
                .toLocalDate() == todayLocalDate
        }.sumOf { it.totalSecondsFocused } / 60
    }
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

    // Urgent tasks: incomplete tasks whose due day is today or earlier.
    // Memoized: these full-list scans would otherwise re-run on every recomposition.
    val urgentTasks = remember(tasks, todayLocalDate) {
        tasks.filter { t ->
            val dueDay = t.task.dueDate?.atZone(ZoneId.systemDefault())?.toLocalDate()
            !t.task.isCompleted && dueDay != null && !dueDay.isAfter(todayLocalDate)
        }.sortedBy { it.task.dueDate }
    }

    // Upcoming tasks: incomplete tasks due strictly after today, within the configured horizon window.
    val upcomingTasks = remember(tasks, todayLocalDate, upcomingWindowDays) {
        val upcomingEndDate = todayLocalDate.plusDays(upcomingWindowDays.toLong())
        tasks.filter { t ->
            val dueDay = t.task.dueDate?.atZone(ZoneId.systemDefault())?.toLocalDate()
            !t.task.isCompleted && dueDay != null && dueDay.isAfter(todayLocalDate) && !dueDay.isAfter(
                upcomingEndDate
            )
        }.sortedBy { it.task.dueDate }
    }

    // Today's habits: scheduled for today and not yet at their weekly target (excluding today's own completion).
    val todaysHabits = remember(habitsWithCompletions, todayLocalDate) {
        habitsWithCompletions.filter { HabitUtils.isHabitDueToday(it) }
    }

    // Recurring tasks join the due/upcoming accordions while actionable, keyed by status so each
    // tile gets its overdue/today border color.
    val recurringByStatus = remember(recurringItems, todayLocalDate, upcomingWindowDays) {
        val now = Instant.now()
        recurringItems.map { it.task }
            .sortedBy { it.dueDate }
            .groupBy { RecurrenceUtils.statusOf(it, now, upcomingWindowDays) }
    }
    val recurringOverdue = recurringByStatus[RecurringStatus.OVERDUE].orEmpty()
    val recurringDueToday = recurringByStatus[RecurringStatus.DUE_TODAY].orEmpty()
    val recurringUpcoming = recurringByStatus[RecurringStatus.UPCOMING].orEmpty()
    val recurringDueCount = recurringOverdue.size + recurringDueToday.size

    Scaffold(
        topBar = {
            TimetyTopBar(
                title = stringResource(R.string.appTitle),
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
                }
            )
        },
        floatingActionButton = {
            TimetyFab(onClick = { onNavigateToTaskDetail(null) }, containerColor = TaskColor)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Greeting and motivation text.
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

            // Daily focus-goal progress card.
            DailyGoalCard(
                focusMinsToday = focusMinsToday,
                dailyTarget = dailyTarget,
                onClick = onNavigateToFocus,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.spaceXLarge)
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()

            // Tasks and habits list.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                if (urgentTasks.isEmpty() && todaysHabits.isEmpty() && recurringDueCount == 0) {
                    Text(
                        text = stringResource(R.string.homeDailyGoalDone),
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
                    ) {
                        // Due tasks accordion.
                        if (urgentTasks.isNotEmpty() || recurringDueCount > 0) {
                            item {
                                StyledExpansionTile(
                                    title = stringResource(
                                        R.string.homeSectionTasksDue,
                                        urgentTasks.size + recurringDueCount
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
                                    (recurringOverdue + recurringDueToday).forEach { recurringTask ->
                                        RecurringTaskListTile(
                                            task = recurringTask,
                                            status = if (recurringTask in recurringOverdue) {
                                                RecurringStatus.OVERDUE
                                            } else RecurringStatus.DUE_TODAY,
                                            onComplete = { completeRecurring(recurringTask) },
                                            onTap = { onNavigateToRecurringDetail(recurringTask.id) },
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }

                        // Today's habits accordion.
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
                                    GroupedHabitsSection(
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

                        // Upcoming tasks accordion.
                        if (upcomingTasks.isNotEmpty() || recurringUpcoming.isNotEmpty()) {
                            item {
                                StyledExpansionTile(
                                    title = stringResource(
                                        R.string.homeSectionTasksUpcoming,
                                        upcomingTasks.size + recurringUpcoming.size
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
                                    recurringUpcoming.forEach { recurringTask ->
                                        RecurringTaskListTile(
                                            task = recurringTask,
                                            status = RecurringStatus.UPCOMING,
                                            onComplete = { completeRecurring(recurringTask) },
                                            onTap = { onNavigateToRecurringDetail(recurringTask.id) },
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


/**
 * Compact daily focus-goal card: label, minutes focused vs target, and a chunky progress bar
 * with the percentage beside it. Tapping navigates to the Focus screen.
 */
@Composable
private fun DailyGoalCard(
    focusMinsToday: Int,
    dailyTarget: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (dailyTarget > 0) focusMinsToday.toFloat() / dailyTarget else 0f
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = AppTheme.brNeo,
        border = BorderStroke(AppTheme.neoBorderWidth, FocusColor),
        colors = CardDefaults.cardColors(containerColor = FocusColor.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(AppTheme.spaceLarge)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.homeDailyGoal).uppercase(),
                    fontSize = AppTheme.fsLabel,
                    fontWeight = AppTheme.fwBold,
                    letterSpacing = AppTheme.lsWide,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$focusMinsToday / $dailyTarget m",
                    fontWeight = AppTheme.fwBold,
                    color = FocusColor
                )
            }
            Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DailyGoalBar(progress = progress, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(AppTheme.spaceMedium))
                Text(
                    text = "${(progress * 100).roundToInt()}%",
                    fontWeight = AppTheme.fwExtraBold,
                    color = FocusColor
                )
            }
        }
    }
}


/**
 * Bordered progress bar for the daily goal. The first lap fills solid; past 100% a second lap
 * of darker diagonal stripes paints over the fill to showcase over-achievement.
 */
@Composable
private fun DailyGoalBar(progress: Float, modifier: Modifier = Modifier) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Canvas(
        modifier = modifier
            .height(16.dp)
            .border(AppTheme.listTileBorderWidth, FocusColor)
    ) {
        drawRect(trackColor)
        drawRect(FocusColor, size = Size(size.width * progress.coerceIn(0f, 1f), size.height))
        val overflow = (progress - 1f).coerceIn(0f, 1f)
        if (overflow > 0f) {
            clipRect(right = size.width * overflow) {
                val step = size.height
                var x = -size.height
                while (x < size.width) {
                    drawLine(
                        color = Color.Black.copy(alpha = 0.35f),
                        start = Offset(x, size.height),
                        end = Offset(x + size.height, 0f),
                        strokeWidth = step / 2f
                    )
                    x += step * 1.5f
                }
            }
        }
    }
}

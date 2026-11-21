package io.github.benji377.timety.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.TaskWithSubtasks
import io.github.benji377.timety.data.repository.AccordionKey
import io.github.benji377.timety.ui.components.common.NeoCard
import io.github.benji377.timety.ui.components.common.NeoFab
import io.github.benji377.timety.ui.components.common.NeoIconButton
import io.github.benji377.timety.ui.components.common.NeoTopBar
import io.github.benji377.timety.ui.components.common.StyledExpansionTile
import io.github.benji377.timety.ui.components.habit.GroupedHabitsSection
import io.github.benji377.timety.ui.components.habit.HabitListTile
import io.github.benji377.timety.ui.components.task.RecurringTaskListTile
import io.github.benji377.timety.ui.components.task.TaskListTile
import io.github.benji377.timety.ui.components.task.rememberRecurringCompleter
import io.github.benji377.timety.ui.components.task.rememberTaskCompletionToggle
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.LocalSnackbarHostState
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.ui.theme.forText
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.ui.viewmodel.HabitViewModel
import io.github.benji377.timety.ui.viewmodel.RecurringTaskViewModel
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import io.github.benji377.timety.ui.viewmodel.UserViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import io.github.benji377.timety.ui.components.habit.rememberHabitCheckIn
import io.github.benji377.timety.util.habit.HabitUtils
import io.github.benji377.timety.util.task.RecurrenceUtils
import io.github.benji377.timety.util.task.RecurringStatus
import io.github.benji377.timety.util.task.due
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * Home screen: an optional greeting, a compact daily focus-goal progress card, and accordion lists
 * of due tasks, today's habits, and upcoming tasks.
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
    val unorderedStacks by habitViewModel.unorderedStacks.collectAsState()
    val userProfile by userViewModel.userProfile.collectAsState()
    val sessions by focusViewModel.allSessions.collectAsState()
    val dailyFocusTarget by settingsViewModel.dailyFocusTarget.collectAsState()
    val showGreeting by settingsViewModel.showHomeGreeting.collectAsState()
    val upcomingWindowDays by settingsViewModel.upcomingTasksHorizon.collectAsState()
    val recurringItems by recurringViewModel.allRecurringTasks.collectAsState()
    val recurringSubtasks = remember(recurringItems) { recurringItems.associate { it.task.id to it.subtasks } }
    val completeRecurring =
        rememberRecurringCompleter(recurringViewModel, LocalSnackbarHostState.current)
    val toggleTaskCompletion = rememberTaskCompletionToggle(taskViewModel, focusViewModel)

    // One tile for a row of the pinned, due or upcoming section.
    @Composable
    fun HomeTaskRowTile(row: HomeTaskRow) {
        when (row) {
            is HomeTaskRow.OneOff -> TaskListTile(
                task = row.task.task,
                isOverdue = row.task.task.due?.isOverdue(Instant.now()) == true,
                enableDismissible = false,
                showDescription = false,
                subtasksCompleted = row.task.subtasks.count { it.isCompleted },
                subtasksTotal = row.task.subtasks.size,
                onToggleCompleted = { toggleTaskCompletion(row.task.task) },
                onTap = { onNavigateToTaskDetail(row.task.task.id) }
            )
            is HomeTaskRow.Recurring -> RecurringTaskListTile(
                task = row.task,
                status = row.status,
                subtasksCompleted = recurringSubtasks[row.task.id].orEmpty().count { it.isCompleted },
                subtasksTotal = recurringSubtasks[row.task.id].orEmpty().size,
                onComplete = { completeRecurring(row.task) },
                onTap = { onNavigateToRecurringDetail(row.task.id) },
            )
        }
    }

    val isPinnedExpanded by settingsViewModel.accordionExpanded(AccordionKey.HOME_PINNED)
        .collectAsState()
    val isTasksDueExpanded by settingsViewModel.accordionExpanded(AccordionKey.HOME_TASKS_DUE)
        .collectAsState()
    val isHabitsDueExpanded by settingsViewModel.accordionExpanded(AccordionKey.HOME_HABITS_DUE)
        .collectAsState()
    val isTasksUpcomingExpanded by settingsViewModel.accordionExpanded(AccordionKey.HOME_TASKS_UPCOMING)
        .collectAsState()

    val userName = userProfile?.name ?: "User"

    val todayLocalDate = LocalDate.now()
    val habitCheckIn = rememberHabitCheckIn(habitViewModel)
    val focusTargetToday = dailyFocusTarget.minutesOn(todayLocalDate)
    // Same count as the Focus screen: a session crossing midnight is split between the days.
    val focusMinsToday = remember(sessions, todayLocalDate) {
        focusViewModel.getMinutesFocusedOnDay(todayLocalDate)
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

    // Pinned tasks go in their own section whatever their due date, and only there.
    val unpinnedTasks = remember(tasks) { tasks.filter { it.task.pinnedAt == null } }

    // Urgent tasks: incomplete tasks whose due day is today or earlier.
    // Memoized: these full-list scans would otherwise re-run on every recomposition.
    val urgentTasks = remember(unpinnedTasks, todayLocalDate) {
        unpinnedTasks.filter { t ->
            val dueDay = t.task.dueDate?.atZone(ZoneId.systemDefault())?.toLocalDate()
            !t.task.isCompleted && dueDay != null && !dueDay.isAfter(todayLocalDate)
        }.sortedBy { it.task.dueDate }
    }

    // Upcoming tasks: incomplete tasks due strictly after today, within the configured horizon window.
    val upcomingTasks = remember(unpinnedTasks, todayLocalDate, upcomingWindowDays) {
        val upcomingEndDate = todayLocalDate.plusDays(upcomingWindowDays.toLong())
        unpinnedTasks.filter { t ->
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
            .filter { it.pinnedAt == null }
            .sortedBy { it.dueDate }
            .groupBy { RecurrenceUtils.statusOf(it, now, upcomingWindowDays) }
    }
    val recurringOverdue = recurringByStatus[RecurringStatus.OVERDUE].orEmpty()
    val recurringDueToday = recurringByStatus[RecurringStatus.DUE_TODAY].orEmpty()
    val recurringUpcoming = recurringByStatus[RecurringStatus.UPCOMING].orEmpty()
    val recurringDueCount = recurringOverdue.size + recurringDueToday.size

    // One-off and recurring tasks in one due-date order, so an overdue recurring task isn't
    // listed after a one-off task that is only due later today.
    val dueRows = remember(urgentTasks, recurringOverdue, recurringDueToday) {
        homeTaskRows(
            urgentTasks,
            recurringOverdue.map { it to RecurringStatus.OVERDUE } +
                    recurringDueToday.map { it to RecurringStatus.DUE_TODAY },
        )
    }
    val upcomingRows = remember(upcomingTasks, recurringUpcoming) {
        homeTaskRows(upcomingTasks, recurringUpcoming.map { it to RecurringStatus.UPCOMING })
    }
    // Open pinned tasks of both kinds, oldest pin first; a done one-off task is never pinned.
    val pinnedRows = remember(tasks, recurringItems, upcomingWindowDays) {
        val now = Instant.now()
        val oneOff = tasks.filter { it.task.pinnedAt != null && !it.task.isCompleted }
            .map { HomeTaskRow.OneOff(it) to it.task.pinnedAt!! }
        val recurring = recurringItems.map { it.task }.filter { it.pinnedAt != null }
            .map { HomeTaskRow.Recurring(it, RecurrenceUtils.statusOf(it, now, upcomingWindowDays)) to it.pinnedAt!! }
        (oneOff + recurring).sortedBy { it.second }.map { it.first }
    }

    Scaffold(
        topBar = {
            NeoTopBar(
                title = stringResource(R.string.appTitle),
                actions = {
                    NeoIconButton(
                        onClick = onNavigateToStatistics,
                        icon = Icons.Filled.BarChart,
                        contentDescription = stringResource(R.string.commonTooltipStats),
                        modifier = Modifier.padding(end = AppTheme.spaceSmall),
                    )
                    NeoIconButton(
                        onClick = onNavigateToCalendar,
                        icon = Icons.Filled.CalendarToday,
                        contentDescription = stringResource(R.string.commonTooltipCalendar),
                        modifier = Modifier.padding(end = AppTheme.spaceSmall),
                    )
                }
            )
        },
        floatingActionButton = {
            NeoFab(onClick = { onNavigateToTaskDetail(null) }, containerColor = TaskColor)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Greeting and motivation text, hidden when turned off in settings.
            if (showGreeting) {
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
                        color = TaskColor.forText()
                    )
                }
            }

            // Daily focus-goal progress card, hidden when today has no target.
            if (focusTargetToday != null) {
                // Without the greeting's padding the card would sit flush against the top bar.
                if (!showGreeting) Spacer(modifier = Modifier.height(16.dp))
                DailyGoalCard(
                    focusMinsToday = focusMinsToday,
                    dailyTarget = focusTargetToday,
                    onClick = onNavigateToFocus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTheme.spaceXLarge)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // With nothing above it, the divider would double the top bar's own border.
            if (showGreeting || focusTargetToday != null) HorizontalDivider()

            // Tasks and habits list.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val nothingDueToday =
                    urgentTasks.isEmpty() && todaysHabits.isEmpty() && recurringDueCount == 0
                val nothingUpcoming = upcomingTasks.isEmpty() && recurringUpcoming.isEmpty()
                if (nothingDueToday && nothingUpcoming && pinnedRows.isEmpty()) {
                    Text(
                        text = stringResource(R.string.homeDailyGoalDone),
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
                    ) {
                        // Pinned tasks accordion, above everything else.
                        if (pinnedRows.isNotEmpty()) {
                            item {
                                StyledExpansionTile(
                                    title = stringResource(R.string.homeSectionPinned, pinnedRows.size),
                                    titleColor = TaskColor,
                                    expanded = isPinnedExpanded,
                                    onExpandedChange = {
                                        settingsViewModel.setAccordionExpanded(AccordionKey.HOME_PINNED, it)
                                    },
                                ) {
                                    pinnedRows.forEach { row -> HomeTaskRowTile(row) }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                        if (nothingDueToday) {
                            item {
                                Text(
                                    text = stringResource(R.string.homeDailyGoalDone),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = AppTheme.spaceXLarge)
                                )
                            }
                        }
                        // Due tasks accordion.
                        if (urgentTasks.isNotEmpty() || recurringDueCount > 0) {
                            item {
                                StyledExpansionTile(
                                    title = stringResource(
                                        R.string.homeSectionTasksDue,
                                        urgentTasks.size + recurringDueCount
                                    ),
                                    titleColor = WarningColor,
                                    expanded = isTasksDueExpanded,
                                    onExpandedChange = {
                                        settingsViewModel.setAccordionExpanded(
                                            AccordionKey.HOME_TASKS_DUE, it
                                        )
                                    },
                                ) {
                                    dueRows.forEach { row ->
                                        HomeTaskRowTile(row)
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
                                    expanded = isHabitsDueExpanded,
                                    onExpandedChange = {
                                        settingsViewModel.setAccordionExpanded(
                                            AccordionKey.HOME_HABITS_DUE, it
                                        )
                                    },
                                ) {
                                    GroupedHabitsSection(
                                        habits = todaysHabits,
                                        allHabitsForStacks = habitsWithCompletions,
                                        targetDate = todayLocalDate,
                                        pinnedFirst = true,
                                        unorderedStacks = unorderedStacks,
                                        habitBuilder = { hc, isDone, isStacked, lock, _ ->
                                            val completionsThisPeriod =
                                                HabitUtils.getCompletionsThisPeriod(hc)
                                            HabitListTile(
                                                habit = hc.habit,
                                                isCompleted = isDone,
                                                checkInsToday = HabitUtils.countOn(hc, todayLocalDate),
                                                isStacked = isStacked,
                                                lock = lock,
                                                subtitleText = HabitUtils.buildHabitSubtitle(
                                                    hc.habit,
                                                    completionsThisPeriod
                                                ),
                                                enableDismissible = false,
                                                onToggleCompleted = { habitCheckIn(hc.habit) },
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
                                    expanded = isTasksUpcomingExpanded,
                                    onExpandedChange = {
                                        settingsViewModel.setAccordionExpanded(
                                            AccordionKey.HOME_TASKS_UPCOMING, it
                                        )
                                    },
                                ) {
                                    upcomingRows.forEach { row ->
                                        HomeTaskRowTile(row)
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

/** A row in one of Home's task accordions: a one-off task or a recurring task with its status. */
private sealed interface HomeTaskRow {
    val dueDate: Instant

    data class OneOff(val task: TaskWithSubtasks) : HomeTaskRow {
        // Rows only come from tasks filtered on having a due date.
        override val dueDate: Instant get() = task.task.dueDate ?: Instant.MAX
    }

    data class Recurring(val task: RecurringTaskEntity, val status: RecurringStatus) : HomeTaskRow {
        override val dueDate: Instant get() = task.dueDate
    }
}

/** [oneOff] and [recurring] tasks merged into one list, earliest due first. */
private fun homeTaskRows(
    oneOff: List<TaskWithSubtasks>,
    recurring: List<Pair<RecurringTaskEntity, RecurringStatus>>,
): List<HomeTaskRow> =
    (oneOff.map { HomeTaskRow.OneOff(it) } + recurring.map { (task, status) -> HomeTaskRow.Recurring(task, status) })
        .sortedBy { it.dueDate }

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
    NeoCard(
        onClick = onClick,
        modifier = modifier,
        borderColor = FocusColor,
        // Neutral fill sets it apart from the plain surface cards without fighting the accent text.
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(AppTheme.spaceLarge)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.homeFocusToday).uppercase(),
                    fontSize = AppTheme.fsLabel,
                    fontWeight = AppTheme.fwBold,
                    letterSpacing = AppTheme.lsWide,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$focusMinsToday / $dailyTarget m",
                    fontWeight = AppTheme.fwBold,
                    color = FocusColor.forText()
                )
            }
            Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DailyGoalBar(progress = progress, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(AppTheme.spaceMedium))
                Text(
                    text = "${(progress * 100).roundToInt()}%",
                    fontWeight = AppTheme.fwExtraBold,
                    color = FocusColor.forText()
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
            .border(AppTheme.borderHairline, FocusColor)
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

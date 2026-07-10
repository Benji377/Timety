package io.github.benji377.timety.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.data.model.task.RecurringOccurrenceEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.RecurringTaskWithOccurrences
import io.github.benji377.timety.data.model.task.TaskWithSubtasks
import io.github.benji377.timety.ui.components.common.StyledExpansionTile
import io.github.benji377.timety.ui.components.common.TimetyTopBar
import io.github.benji377.timety.ui.components.focus.localizedFocusModeName
import io.github.benji377.timety.ui.components.task.rememberRecurringCompleter
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.LocalSnackbarHostState
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.utils.AppUtils
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.ui.viewmodel.HabitViewModel
import io.github.benji377.timety.ui.viewmodel.RecurringTaskViewModel
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.datetime.AppDateUtils
import io.github.benji377.timety.util.datetime.CalendarUtils
import io.github.benji377.timety.util.habit.HabitUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


/**
 * Calendar screen showing a month grid plus, for the selected day, accordion lists of due tasks,
 * scheduled habits, and focus sessions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    taskViewModel: TaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    habitViewModel: HabitViewModel = viewModel(factory = AppViewModelProvider.Factory),
    focusViewModel: FocusViewModel = activityScopedViewModel(),
    recurringViewModel: RecurringTaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onNavigateToTask: (String) -> Unit = {},
    onNavigateToHabit: (String) -> Unit = {},
    onNavigateToRecurring: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val tasks by taskViewModel.allTasks.collectAsState()
    val habitsWithCompletions by habitViewModel.habitsWithCompletions.collectAsState()
    val sessions by focusViewModel.allSessions.collectAsState()
    val recurringItems by recurringViewModel.allRecurringTasks.collectAsState()
    val completeRecurring =
        rememberRecurringCompleter(recurringViewModel, LocalSnackbarHostState.current)
    val zone = ZoneId.systemDefault()

    var focusedMonth by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }

    // Filter and sort the task/session/habit data for the selected day's accordion lists.
    val selectedDayTasks = remember(tasks, selectedDate) {
        selectedDate?.let { d ->
            tasks.filter { t -> t.task.dueDate?.atZone(zone)?.toLocalDate() == d }
                .sortedByDescending { it.task.priority.ordinal }
        } ?: emptyList()
    }

    val selectedDaySessions = remember(sessions, selectedDate) {
        selectedDate?.let { d ->
            sessions.filter { s -> s.startTime.atZone(zone).toLocalDate() == d }
                .sortedBy { it.startTime }
        } ?: emptyList()
    }

    // Recurring tasks due on the selected day plus occurrences completed on it. Completing one
    // advances its due date, so the upcoming entry moves to the next cycle on its own while the
    // logged occurrence keeps the completion day marked.
    val selectedDayRecurringDue = remember(recurringItems, selectedDate) {
        selectedDate?.let { d ->
            recurringItems.map { it.task }
                .filter { it.dueDate.atZone(zone).toLocalDate() == d }
                .sortedBy { it.dueDate }
        } ?: emptyList()
    }
    val selectedDayRecurringDone = remember(recurringItems, selectedDate) {
        selectedDate?.let { d ->
            recurringItems.flatMap { item ->
                item.occurrences
                    .filter { it.completedAt.atZone(zone).toLocalDate() == d }
                    .map { item.task to it }
            }.sortedBy { it.second.completedAt }
        } ?: emptyList()
    }

    // Combine habits scheduled for this day + habits actually completed on this day.
    val selectedDayHabits = remember(habitsWithCompletions, selectedDate) {
        selectedDate?.let { d ->
            val scheduled = habitsWithCompletions.filter { isHabitScheduledOn(it, d) }
            val completed = habitsWithCompletions.filter { HabitUtils.isCompletedOn(it, d) }
            (scheduled + completed).distinctBy { it.habit.id }
        } ?: emptyList()
    }

    Scaffold(
        topBar = {
            TimetyTopBar(
                title = stringResource(R.string.calendarTitle),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.commonBack)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        focusedMonth = LocalDate.now().withDayOfMonth(1)
                        selectedDate = LocalDate.now()
                    }) {
                        Icon(
                            Icons.Filled.Today,
                            contentDescription = stringResource(R.string.calendarTooltipToday)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top half: the month calendar grid.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Column {
                    MonthNavigator(
                        focusedMonth = focusedMonth,
                        onPrevious = { focusedMonth = focusedMonth.minusMonths(1) },
                        onNext = { focusedMonth = focusedMonth.plusMonths(1) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CalendarGrid(
                        focusedMonth = focusedMonth,
                        selectedDate = selectedDate,
                        tasks = tasks,
                        sessions = sessions,
                        habitsWithCompletions = habitsWithCompletions,
                        recurringItems = recurringItems,
                        onDaySelected = { selectedDate = it }
                    )
                }
            }

            HorizontalDivider(thickness = 1.dp)

            // Bottom half: accordion lists for the selected day.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                if (selectedDate == null) {
                    Text(
                        text = stringResource(R.string.calendarLabelSelect),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    val d = selectedDate!!
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        item {
                            HabitsAccordion(
                                habits = selectedDayHabits,
                                selectedDate = d,
                                onToggle = { habit, isCompleted ->
                                    if (isCompleted) {
                                        habitViewModel.unmarkCompletionOnDate(habit.habit.id, d)
                                    } else {
                                        val now = java.time.LocalTime.now()
                                        val retroInstant = d.atTime(now).atZone(zone).toInstant()
                                        habitViewModel.markCompletionOnDate(
                                            habit.habit.id,
                                            retroInstant
                                        )
                                    }
                                },
                                onHabitClick = { habit -> onNavigateToHabit(habit.habit.id) }
                            )
                        }
                        item {
                            TasksAccordion(
                                tasks = selectedDayTasks,
                                recurringDue = selectedDayRecurringDue,
                                recurringDone = selectedDayRecurringDone,
                                onToggle = { task -> taskViewModel.toggleTaskCompletion(task.task) },
                                onTaskClick = { task -> onNavigateToTask(task.task.id) },
                                onCompleteRecurring = completeRecurring,
                                onUncheckRecurring = { occurrence ->
                                    recurringViewModel.deleteOccurrence(occurrence)
                                },
                                onRecurringClick = { task -> onNavigateToRecurring(task.id) },
                            )
                        }
                        item {
                            FocusSessionsAccordion(
                                sessions = selectedDaySessions,
                                focusViewModel = focusViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}


private fun isHabitScheduledOn(hwc: HabitWithCompletions, date: LocalDate): Boolean {
    return when (hwc.habit.frequency) {
        HabitFrequency.DAILY -> true
        HabitFrequency.WEEKLY_FLEXIBLE -> true
        HabitFrequency.WEEKLY_EXACT -> HabitUtils.parseWeekdays(hwc.habit.targetWeekdays)
            .contains(date.dayOfWeek.value)
    }
}

@Composable
private fun MonthNavigator(focusedMonth: LocalDate, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = null)
        }
        Text(
            text = AppDateFormatUtils.formatMonthYear(
                focusedMonth
            ),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun CalendarGrid(
    focusedMonth: LocalDate,
    selectedDate: LocalDate?,
    tasks: List<TaskWithSubtasks>,
    sessions: List<FocusSessionEntity>,
    habitsWithCompletions: List<HabitWithCompletions>,
    recurringItems: List<RecurringTaskWithOccurrences>,
    onDaySelected: (LocalDate) -> Unit
) {
    val zone = ZoneId.systemDefault()
    val weeks = remember(focusedMonth) { CalendarUtils.generateWeeks(focusedMonth) }
    val today = LocalDate.now()

    // Pre-compute sets of dates with activity for O(1) lookup during grid generation.
    // Recurring tasks mark their next due day and every completed occurrence's day.
    val taskDateKeys = remember(tasks, recurringItems) {
        val taskDays = tasks.mapNotNull { it.task.dueDate }
            .map { AppDateUtils.dayKey(it.atZone(zone).toLocalDate()) }
        val recurringDays = recurringItems.flatMap { item ->
            item.occurrences.map { it.completedAt } + item.task.dueDate
        }.map { AppDateUtils.dayKey(it.atZone(zone).toLocalDate()) }
        (taskDays + recurringDays).toSet()
    }
    val sessionDateKeys = remember(sessions) {
        sessions.map { AppDateUtils.dayKey(it.startTime.atZone(zone).toLocalDate()) }.toSet()
    }
    val habitDateKeys = remember(habitsWithCompletions) {
        habitsWithCompletions.flatMap { it.completions }
            .map { AppDateUtils.dayKey(it.completionDate.atZone(zone).toLocalDate()) }
            .toSet()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header row: Mon..Sun + Weekly summary column.
        Row(modifier = Modifier.fillMaxWidth()) {
            val headers = listOf(
                stringResource(R.string.calendarHeaderMon),
                stringResource(R.string.calendarHeaderTue),
                stringResource(R.string.calendarHeaderWed),
                stringResource(R.string.calendarHeaderThu),
                stringResource(R.string.calendarHeaderFri),
                stringResource(R.string.calendarHeaderSat),
                stringResource(R.string.calendarHeaderSun)
            )
            headers.forEach { header ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        header,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.calendarHeaderWeekly),
                    fontWeight = FontWeight.Bold,
                    color = TaskColor,
                    maxLines = 1
                )
            }
        }

        // Memoized: these scans touch every task/completion/session for all ~6 week rows and
        // would otherwise re-run on every recomposition (e.g. each day tap).
        val weeklyCounts = remember(weeks, tasks, sessions, habitsWithCompletions, recurringItems) {
            weeks.associateWith { week ->
                val weekStart = week.first()
                val weekEnd = week.last()
                fun inWeek(instant: Instant): Boolean {
                    val d = instant.atZone(zone).toLocalDate()
                    return !d.isBefore(weekStart) && !d.isAfter(weekEnd)
                }
                val taskCount = tasks.count { t ->
                    t.task.dueDate != null && inWeek(t.task.dueDate)
                } + recurringItems.count { inWeek(it.task.dueDate) } +
                    recurringItems.sumOf { item ->
                        item.occurrences.count { inWeek(it.completedAt) }
                    }
                val habitCount = habitsWithCompletions.sumOf { h ->
                    h.completions.count { c ->
                        val d = c.completionDate.atZone(zone).toLocalDate()
                        !d.isBefore(weekStart) && !d.isAfter(weekEnd)
                    }
                }
                val focusCount = sessions.count { s ->
                    val d = s.startTime.atZone(zone).toLocalDate()
                    !d.isBefore(weekStart) && !d.isAfter(weekEnd)
                }
                Triple(taskCount, habitCount, focusCount)
            }
        }

        weeks.forEach { week ->
            val (weeklyTaskCount, weeklyHabitCount, weeklyFocusCount) =
                weeklyCounts[week] ?: Triple(0, 0, 0)

            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    val isCurrentMonth = day.month == focusedMonth.month
                    val isSelected = day == selectedDate
                    val isToday = day == today
                    val dateKey = AppDateUtils.dayKey(day)
                    val hasTasks = taskDateKeys.contains(dateKey)
                    val hasFocus = sessionDateKeys.contains(dateKey)
                    val hasHabits = habitDateKeys.contains(dateKey)

                    // Padding sits outside the fixed height so the selection outline keeps clear of the dots.
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .height(45.dp)
                            .background(
                                color = if (isSelected) TaskColor.copy(alpha = 0.2f) else Color.Transparent,
                                shape = AppTheme.brMedium
                            )
                            .border(
                                width = if (isToday) 2.dp else 0.dp,
                                color = if (isToday) TaskColor else Color.Transparent,
                                shape = AppTheme.brMedium
                            )
                            .clickable { onDaySelected(day) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = day.dayOfMonth.toString(),
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrentMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.5f
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (hasTasks) Dot(TaskColor)
                                if (hasTasks && (hasFocus || hasHabits)) Spacer(
                                    modifier = Modifier.width(
                                        2.dp
                                    )
                                )
                                if (hasFocus) Dot(SuccessColor)
                                if (hasFocus && hasHabits) Spacer(modifier = Modifier.width(2.dp))
                                if (hasHabits) Dot(HabitColor)
                                if (!hasTasks && !hasFocus && !hasHabits) Spacer(
                                    modifier = Modifier.height(
                                        5.dp
                                    )
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .height(53.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = TaskColor,
                                    fontWeight = FontWeight.Bold
                                )
                            ) { append("$weeklyTaskCount") }
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                append(
                                    " | "
                                )
                            }
                            withStyle(
                                SpanStyle(
                                    color = HabitColor,
                                    fontWeight = FontWeight.Bold
                                )
                            ) { append("$weeklyHabitCount") }
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                append(
                                    " | "
                                )
                            }
                            withStyle(
                                SpanStyle(
                                    color = SuccessColor,
                                    fontWeight = FontWeight.Bold
                                )
                            ) { append("$weeklyFocusCount") }
                        },
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(5.dp)
            .background(color, shape = CircleShape)
    )
}

// Accordion widgets for the day-detail lists below.

@Composable
private fun HabitsAccordion(
    habits: List<HabitWithCompletions>,
    selectedDate: LocalDate,
    onToggle: (HabitWithCompletions, Boolean) -> Unit,
    onHabitClick: (HabitWithCompletions) -> Unit
) {
    StyledExpansionTile(
        title = stringResource(R.string.calendarSectionHabits, habits.size),
        titleColor = HabitColor,
        initiallyExpanded = false
    ) {
        if (habits.isEmpty()) {
            Text(
                text = stringResource(R.string.calendarSectionHabitsEmpty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            habits.forEach { hwc ->
                val isCompleted = HabitUtils.isCompletedOn(hwc, selectedDate)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clickable { onHabitClick(hwc) },
                    shape = AppTheme.brMedium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    border = BorderStroke(
                        1.dp,
                        if (isCompleted) HabitColor.copy(alpha = 0.3f) else HabitColor
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isCompleted,
                            onCheckedChange = { onToggle(hwc, isCompleted) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = SuccessColor,
                                uncheckedColor = HabitColor,
                                checkmarkColor = Color.White
                            )
                        )
                        Text(
                            text = hwc.habit.name,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                            color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
        }
    }
}

@Composable
private fun TasksAccordion(
    tasks: List<TaskWithSubtasks>,
    recurringDue: List<RecurringTaskEntity>,
    recurringDone: List<Pair<RecurringTaskEntity, RecurringOccurrenceEntity>>,
    onToggle: (TaskWithSubtasks) -> Unit,
    onTaskClick: (TaskWithSubtasks) -> Unit,
    onCompleteRecurring: (RecurringTaskEntity) -> Unit,
    onUncheckRecurring: (RecurringOccurrenceEntity) -> Unit,
    onRecurringClick: (RecurringTaskEntity) -> Unit,
) {
    val totalCount = tasks.size + recurringDue.size + recurringDone.size
    StyledExpansionTile(
        title = stringResource(R.string.calendarSectionTasks, totalCount),
        titleColor = TaskColor,
        initiallyExpanded = false
    ) {
        if (totalCount == 0) {
            Text(
                text = stringResource(R.string.calendarSectionTasksEmpty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            tasks.forEach { taskWithSubtasks ->
                val task = taskWithSubtasks.task
                DayTaskRow(
                    title = task.title,
                    isCompleted = task.isCompleted,
                    onToggle = { onToggle(taskWithSubtasks) },
                    onClick = { onTaskClick(taskWithSubtasks) },
                    trailing = { AppUtils.PriorityIcon(priority = task.priority) },
                )
            }
            recurringDue.forEach { task ->
                DayTaskRow(
                    title = task.title,
                    isCompleted = false,
                    onToggle = { onCompleteRecurring(task) },
                    onClick = { onRecurringClick(task) },
                    trailing = { RecurringMarkerIcon() },
                )
            }
            recurringDone.forEach { (task, occurrence) ->
                DayTaskRow(
                    title = task.title,
                    isCompleted = true,
                    onToggle = { onUncheckRecurring(occurrence) },
                    onClick = { onRecurringClick(task) },
                    trailing = { RecurringMarkerIcon() },
                )
            }
            Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
        }
    }
}

/** Checkbox row card shared by normal and recurring tasks in the day-detail task accordion. */
@Composable
private fun DayTaskRow(
    title: String,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = AppTheme.brMedium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        border = BorderStroke(
            1.dp,
            if (isCompleted) SuccessColor else TaskColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = SuccessColor,
                    uncheckedColor = TaskColor,
                    checkmarkColor = Color.White
                )
            )
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                textDecoration = if (isCompleted) TextDecoration.LineThrough else null
            )
            trailing()
        }
    }
}

@Composable
private fun RecurringMarkerIcon() {
    Icon(
        imageVector = Icons.Filled.Repeat,
        contentDescription = null,
        tint = TaskColor,
        modifier = Modifier.size(AppTheme.iconSizeSmall),
    )
}

@Composable
private fun FocusSessionsAccordion(
    sessions: List<FocusSessionEntity>,
    focusViewModel: FocusViewModel
) {
    val modes by focusViewModel.allModes.collectAsState()
    val tags by focusViewModel.allTags.collectAsState()
    val zone = ZoneId.systemDefault()
    val use24Hour =
        LocalDateFormatSettings.current.use24HourFormat
    val ongoingLabel = stringResource(R.string.calendarLabelFocusOngoing)
    val untaggedLabel = stringResource(R.string.focusTargetUntagged)

    StyledExpansionTile(
        title = stringResource(R.string.calendarSectionFocus, sessions.size),
        titleColor = SuccessColor,
        initiallyExpanded = false
    ) {
        if (sessions.isEmpty()) {
            Text(
                text = stringResource(R.string.calendarSectionFocusEmpty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            sessions.forEach { session ->
                val mode = modes.firstOrNull { it.id == session.modeId } ?: modes.firstOrNull()
                val tag = session.tagId?.let { tagId -> tags.firstOrNull { it.id == tagId } }

                var timeString =
                    AppDateFormatUtils.formatTime(
                        session.startTime,
                        use24Hour,
                        zone
                    )
                timeString += if (session.endTime != null) {
                    " - ${
                        AppDateFormatUtils.formatTime(
                            session.endTime,
                            use24Hour,
                            zone
                        )
                    }"
                } else {
                    " - $ongoingLabel"
                }

                val focusMins = session.totalSecondsFocused / 60

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    shape = AppTheme.brMedium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Circle,
                            contentDescription = null,
                            tint = tag?.let { Color(it.colorValue) }
                                ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = tag?.name ?: untaggedLabel, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            if (mode != null) {
                                Text(text = localizedFocusModeName(mode), fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            Text(
                                text = timeString,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = stringResource(R.string.focusMinutes, focusMins),
                            color = SuccessColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
        }
    }
}

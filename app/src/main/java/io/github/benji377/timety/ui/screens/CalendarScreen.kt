package io.github.benji377.timety.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.color
import io.github.benji377.timety.ui.components.calendar.PeriodReportDialog
import io.github.benji377.timety.ui.components.common.BackNavigationIcon
import io.github.benji377.timety.ui.components.common.MonthNavigator
import io.github.benji377.timety.ui.components.common.NeoIconButton
import io.github.benji377.timety.ui.components.common.NeoTopBar
import io.github.benji377.timety.ui.components.task.rememberRecurringCompleter
import io.github.benji377.timety.ui.components.task.rememberTaskCompletionToggle
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.LocalSnackbarHostState
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.ui.viewmodel.HabitViewModel
import io.github.benji377.timety.ui.viewmodel.RecurringTaskViewModel
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import io.github.benji377.timety.util.calendar.CalendarDayItems
import io.github.benji377.timety.util.stats.PeriodSummary
import io.github.benji377.timety.util.stats.ReportPeriod
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

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
    val toggleTaskCompletion = rememberTaskCompletionToggle(taskViewModel, focusViewModel)
    val zone = ZoneId.systemDefault()

    var focusedMonth by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    // The report being shown, as (period, first day); a Pair of Serializables survives rotation.
    var openReport by rememberSaveable { mutableStateOf<Pair<ReportPeriod, LocalDate>?>(null) }

    val dayItems = remember(selectedDate, tasks, recurringItems, habitsWithCompletions, sessions) {
        selectedDate?.let { CalendarDayItems.of(it, tasks, recurringItems, habitsWithCompletions, sessions, zone) }
    }

    openReport?.let { (period, start) ->
        val plainTasks = tasks.map { it.task }
        val habitCompletions = habitsWithCompletions.flatMap { h -> h.completions.map { it.completionDate } }
        fun summaryOf(range: ClosedRange<LocalDate>, until: Instant? = null) =
            PeriodSummary.of(range, plainTasks, recurringItems, habitCompletions, sessions, zone, until)
        // The current period's figures cover all of it (time still ahead adds nothing done, while
        // tasks due later stay counted); only the comparison is cut to the time elapsed so far.
        val now = ZonedDateTime.now(zone)
        val comparedWith = period.comparisonRange(start, now.toLocalDate())
        PeriodReportDialog(
            period = period,
            start = start,
            comparedWith = comparedWith,
            summary = summaryOf(start..period.endOf(start)),
            previous = summaryOf(comparedWith, period.comparisonCutoff(start, now)),
            onDismiss = { openReport = null }
        )
    }

    Scaffold(
        topBar = {
            NeoTopBar(
                title = stringResource(R.string.calendarTitle),
                navigationIcon = {
                    BackNavigationIcon(onClick = onNavigateBack)
                },
                actions = {
                    NeoIconButton(
                        onClick = {
                            focusedMonth = LocalDate.now().withDayOfMonth(1)
                            selectedDate = LocalDate.now()
                        },
                        icon = Icons.Filled.Today,
                        contentDescription = stringResource(R.string.calendarTooltipToday)
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top half: the month calendar grid. Cream background (matches the app-wide surface,
            // not a pure-white Material `surface`) inside a hairline border, so the grid reads as
            // a distinct component instead of floating edge-to-edge.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // Both the outer margin and the inner padding are kept tight: a seven-column
                    // grid has to fit inside whatever is left, and stacking two generous paddings
                    // squeezed the day cells down to a size that was awkward to tap.
                    .padding(horizontal = AppTheme.spaceSmall, vertical = AppTheme.spaceSmall)
                    .background(MaterialTheme.colorScheme.background, AppTheme.brNeo)
                    .border(AppTheme.borderCard, MaterialTheme.colorScheme.outline, AppTheme.brNeo)
                    .padding(AppTheme.spaceSmall)
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
                        onDaySelected = { selectedDate = it },
                        onOpenWeeklyReport = { openReport = ReportPeriod.WEEK to it },
                        onOpenMonthlyReport = { openReport = ReportPeriod.MONTH to focusedMonth }
                    )
                }
            }

            // Bottom half: accordion lists for the selected day. Solid theme color instead of an
            // alpha-blended tint (fills are never soft/translucent) - the top border
            // replaces the old standalone HorizontalDivider, giving the same seam as a real edge
            // rather than a soft color transition.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = AppTheme.borderHairline,
                        color = MaterialTheme.colorScheme.outline
                    )
            ) {
                if (selectedDate == null || dayItems == null) {
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
                                habits = dayItems.habits,
                                selectedDate = d,
                                onToggle = { habit, isCompleted ->
                                    if (isCompleted) {
                                        habitViewModel.unmarkCompletionOnDate(habit.habit.id, d)
                                    } else {
                                        val retroInstant = d.atTime(LocalTime.now()).atZone(zone).toInstant()
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
                                tasks = dayItems.tasks,
                                recurringDue = dayItems.recurringDue,
                                recurringDone = dayItems.recurringDone,
                                onToggle = { task -> toggleTaskCompletion(task.task) },
                                onTaskClick = { task -> onNavigateToTask(task.task.id) },
                                onCompleteRecurring = completeRecurring,
                                onUncheckRecurring = { occurrence ->
                                    recurringViewModel.uncheckOccurrence(occurrence)
                                },
                                onRecurringClick = { task -> onNavigateToRecurring(task.id) },
                            )
                        }
                        item {
                            FocusSessionsAccordion(
                                sessions = dayItems.sessions,
                                focusViewModel = focusViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

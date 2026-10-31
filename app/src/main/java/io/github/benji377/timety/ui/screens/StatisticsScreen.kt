package io.github.benji377.timety.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.components.common.BackNavigationIcon
import io.github.benji377.timety.ui.components.common.NeoCard
import io.github.benji377.timety.ui.components.common.NeoTopBar
import io.github.benji377.timety.ui.components.stats.SectionHeader
import io.github.benji377.timety.ui.components.stats.StatCard
import io.github.benji377.timety.ui.components.stats.StatCardStyle
import io.github.benji377.timety.ui.components.stats.SynergyChart
import io.github.benji377.timety.ui.screens.stats.StatsPage
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.ui.utils.rememberToday
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.ui.viewmodel.HabitViewModel
import io.github.benji377.timety.ui.viewmodel.RecurringTaskViewModel
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.habit.HabitRates
import io.github.benji377.timety.util.stats.FocusPeriodStats
import io.github.benji377.timety.util.stats.FocusSessionDayStats
import io.github.benji377.timety.util.stats.PeriodSummary
import io.github.benji377.timety.util.stats.ReportPeriod
import io.github.benji377.timety.util.stats.TaskTiming
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs

private const val DAYS_PER_WEEK = 7

/**
 * The statistics overview: today's numbers, the synergy graph and one line per page for this week.
 * Tiles and lines open the page with the details.
 */
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    onOpenPage: (StatsPage) -> Unit,
    taskViewModel: TaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    recurringViewModel: RecurringTaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    focusViewModel: FocusViewModel = activityScopedViewModel(),
    settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    habitViewModel: HabitViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val tasks by taskViewModel.allTasks.collectAsState()
    val recurring by recurringViewModel.allRecurringTasks.collectAsState()
    val dailyFocusTarget by settingsViewModel.dailyFocusTarget.collectAsState()
    val sessions by focusViewModel.allSessions.collectAsState()
    val habits by habitViewModel.habitsWithCompletions.collectAsState()
    val zone = remember { ZoneId.systemDefault() }
    val today by rememberToday()

    // Completed recurring occurrences count as tasks done on their completion day.
    val tasksPerDay = remember(tasks, recurring) {
        val completions = tasks.mapNotNull { item ->
            item.task.completedAt?.takeIf { item.task.isCompleted }
        } + recurring.flatMap { item -> item.occurrences.map { it.completedAt } }
        completions.groupingBy { it.atZone(zone).toLocalDate() }.eachCount()
    }
    val focusMinutesByDay = remember(sessions) { FocusSessionDayStats.minutesByDay(sessions, zone) }
    val tasksToday = tasksPerDay[today] ?: 0
    val focusToday = focusMinutesByDay[today] ?: 0
    // Goes past 100% like the Home card. Null on days without a target, shown as "-".
    val goalPercent = dailyFocusTarget.minutesOn(today)?.let { target ->
        focusToday * 100 / target
    }

    val last7Days = remember(today) { (6L downTo 0L).map { today.minusDays(it) } }
    val weekStart = ReportPeriod.WEEK.startOf(today)
    val weekDays = remember(weekStart, today) {
        (0L until DAYS_PER_WEEK).map { weekStart.plusDays(it) }.filterNot { it.isAfter(today) }
    }
    val week = weekStart..ReportPeriod.WEEK.endOf(weekStart)

    val habitRate = remember(habits, week, today) {
        HabitRates.total(
            habits.map { item ->
                val dates = item.completions.map { it.completionDate.atZone(zone).toLocalDate() }
                HabitRates.of(item.habit, dates, week, today, zone)
            }
        )
    }
    val checkInsPerDay = remember(habits) {
        habits.flatMap { it.completions }
            .groupingBy { it.completionDate.atZone(zone).toLocalDate() }
            .eachCount()
    }

    val taskEntities = remember(tasks) { tasks.map { it.task } }
    val thisWeek = remember(taskEntities, recurring, sessions, week) {
        PeriodSummary.of(week, taskEntities, recurring, emptyList(), sessions, zone)
    }
    val lastWeek = remember(taskEntities, recurring, sessions, week, today) {
        PeriodSummary.of(
            range = ReportPeriod.WEEK.comparisonRange(weekStart, today),
            tasks = taskEntities,
            recurring = recurring,
            habitCompletions = emptyList(),
            sessions = sessions,
            zone = zone,
            until = ReportPeriod.WEEK.comparisonCutoff(weekStart, ZonedDateTime.now(zone)),
        )
    }
    val dueRate = remember(taskEntities, recurring, week) {
        TaskTiming.dueDateRate(taskEntities, recurring, week, zone)
    }

    Scaffold(
        topBar = {
            NeoTopBar(
                title = stringResource(R.string.statsTitle),
                navigationIcon = { BackNavigationIcon(onClick = onNavigateBack) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(AppTheme.spaceLarge),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spaceXLarge),
        ) {
            item(key = "today") {
                Column {
                    SectionHeader(stringResource(R.string.statsSectionToday))
                    Spacer(Modifier.height(AppTheme.spaceMedium))
                    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall)) {
                        StatCard(
                            title = stringResource(R.string.statsTileTasksDone),
                            value = "$tasksToday",
                            icon = Icons.Filled.TaskAlt,
                            color = TaskColor,
                            style = StatCardStyle.COMPACT_VERTICAL,
                            onClick = { onOpenPage(StatsPage.TASKS) },
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            title = stringResource(R.string.statsTileFocus),
                            value = AppDateFormatUtils.formatMinutesCompact(focusToday),
                            icon = Icons.Filled.Timer,
                            color = FocusColor,
                            style = StatCardStyle.COMPACT_VERTICAL,
                            onClick = { onOpenPage(StatsPage.TIME) },
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            title = stringResource(R.string.statsTileFocusGoal),
                            value = goalPercent?.let { "$it%" } ?: "-",
                            icon = Icons.Filled.TrackChanges,
                            color = WarningColor,
                            style = StatCardStyle.COMPACT_VERTICAL,
                            onClick = { onOpenPage(StatsPage.TIME) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            item(key = "synergy") {
                NeoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(AppTheme.spaceLarge)) {
                        SectionHeader(
                            stringResource(R.string.statsLabelProductivity),
                            stringResource(R.string.statsSynergyHint),
                        )
                        Spacer(Modifier.height(AppTheme.spaceMedium))
                        SynergyChart(
                            days = last7Days,
                            dailyFocus = last7Days.map { focusMinutesByDay[it] ?: 0 },
                            dailyTasks = last7Days.map { tasksPerDay[it] ?: 0 },
                        )
                    }
                }
            }
            item(key = "week") {
                Column {
                    SectionHeader(stringResource(R.string.statsSectionThisWeek))
                    Spacer(Modifier.height(AppTheme.spaceSmall))
                    OverviewRow(
                        title = stringResource(StatsPage.TIME.titleRes),
                        summary = timeSummary(thisWeek.focusMinutes, lastWeek.focusMinutes),
                        sparkline = weekDays.map { focusMinutesByDay[it] ?: 0 },
                        sparklineColor = FocusColor,
                        onClick = { onOpenPage(StatsPage.TIME) },
                    )
                    HorizontalDivider()
                    OverviewRow(
                        title = stringResource(StatsPage.TASKS.titleRes),
                        summary = tasksSummary(thisWeek.tasksCompleted, dueRate.percent),
                        sparkline = weekDays.map { tasksPerDay[it] ?: 0 },
                        sparklineColor = TaskColor,
                        onClick = { onOpenPage(StatsPage.TASKS) },
                    )
                    HorizontalDivider()
                    OverviewRow(
                        title = stringResource(StatsPage.HABITS.titleRes),
                        summary = habitRate.percent?.let { stringResource(R.string.statsRowHabits, it) },
                        sparkline = weekDays.map { checkInsPerDay[it] ?: 0 },
                        sparklineColor = HabitColor,
                        onClick = { onOpenPage(StatsPage.HABITS) },
                    )
                }
            }
        }
    }
}

@Composable
private fun timeSummary(minutes: Int, previousMinutes: Int): String {
    val time = AppDateFormatUtils.formatMinutesCompact(minutes)
    val change = FocusPeriodStats.percentChange(minutes, previousMinutes)
    return when {
        change == null || change == 0 -> stringResource(R.string.statsRowTime, time)
        change > 0 -> stringResource(R.string.statsRowTimeMore, time, change)
        else -> stringResource(R.string.statsRowTimeLess, time, abs(change))
    }
}

@Composable
private fun tasksSummary(done: Int, dueDatePercent: Int?): String {
    val doneText = pluralStringResource(R.plurals.statsTasksDone, done, done)
    return dueDatePercent?.let { stringResource(R.string.statsRowTasksDue, doneText, it) } ?: doneText
}

@Composable
private fun OverviewRow(
    title: String,
    onClick: () -> Unit,
    summary: String? = null,
    sparkline: List<Int>? = null,
    sparklineColor: Color = Color.Unspecified,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(vertical = AppTheme.spaceSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            if (summary != null) {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // One day is a single point and draws no line.
        if (sparkline != null && sparkline.size > 1) {
            Spacer(Modifier.width(AppTheme.spaceMedium))
            Sparkline(sparkline, sparklineColor)
        }
        Spacer(Modifier.width(AppTheme.spaceSmall))
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A small unlabelled line of this week's [values] so far, on a whole week's width so that days
 * still to come aren't drawn as a drop to zero. The text beside it has the numbers.
 */
@Composable
private fun Sparkline(values: List<Int>, color: Color) {
    Canvas(modifier = Modifier.size(width = 56.dp, height = 24.dp)) {
        val max = (values.maxOrNull() ?: 0).coerceAtLeast(1).toFloat()
        val stroke = 2.dp.toPx()
        val points = values.mapIndexed { index, value ->
            Offset(
                index.toFloat() / (DAYS_PER_WEEK - 1) * size.width,
                size.height - stroke - value / max * (size.height - 2 * stroke),
            )
        }
        points.zipWithNext { from, to ->
            drawLine(color, from, to, strokeWidth = stroke, cap = StrokeCap.Round)
        }
    }
}

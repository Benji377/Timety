package io.github.benji377.timety.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.focus.color
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.data.model.task.RecurringTaskWithOccurrences
import io.github.benji377.timety.data.model.task.TaskWithSubtasks
import io.github.benji377.timety.ui.components.common.NeoProgressBar
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.datetime.CalendarUtils
import io.github.benji377.timety.util.stats.PeriodSummary
import io.github.benji377.timety.util.stats.ReportPeriod
import java.time.LocalDate
import java.time.ZoneId

@Composable
internal fun CalendarGrid(
    focusedMonth: LocalDate,
    selectedDate: LocalDate?,
    tasks: List<TaskWithSubtasks>,
    sessions: List<FocusSessionEntity>,
    habitsWithCompletions: List<HabitWithCompletions>,
    recurringItems: List<RecurringTaskWithOccurrences>,
    onDaySelected: (LocalDate) -> Unit,
    onOpenWeeklyReport: (weekStart: LocalDate) -> Unit,
    onOpenMonthlyReport: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val weeks = remember(focusedMonth) { CalendarUtils.generateWeeks(focusedMonth) }
    val today = LocalDate.now()

    // Pre-compute sets of dates with activity for O(1) lookup during grid generation.
    // Recurring tasks mark their next due day and every completed occurrence's day.
    val taskDates = remember(tasks, recurringItems) {
        val taskDays = tasks.mapNotNull { it.task.dueDate }
            .map { it.atZone(zone).toLocalDate() }
        val recurringDays = recurringItems.flatMap { item ->
            item.occurrences.map { it.completedAt } + item.task.dueDate
        }.map { it.atZone(zone).toLocalDate() }
        (taskDays + recurringDays).toSet()
    }
    val sessionDates = remember(sessions) {
        sessions.map { it.startTime.atZone(zone).toLocalDate() }.toSet()
    }
    val habitDates = remember(habitsWithCompletions) {
        habitsWithCompletions.flatMap { it.completions }
            .map { it.completionDate.atZone(zone).toLocalDate() }
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
            // An icon rather than a "Weekly" label: no translation of that word fits the column on a
            // small phone, and the icon also reads as tappable. Heading the whole column, it opens
            // the shown month's report, while each week chip below opens its own week.
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .padding(bottom = 8.dp)
                    .clip(AppTheme.brMedium)
                    .clickable(onClick = onOpenMonthlyReport),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Insights,
                    contentDescription = stringResource(R.string.calendarMonthlyReportTitle),
                    tint = TaskColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Memoized: these scans touch every task/completion/session for all ~6 week rows and
        // would otherwise re-run on every recomposition (e.g. each day tap).
        val weeklySummaries = remember(weeks, tasks, sessions, habitsWithCompletions, recurringItems) {
            val plainTasks = tasks.map { it.task }
            val habitCompletions = habitsWithCompletions.flatMap { h -> h.completions.map { it.completionDate } }
            weeks.associateWith { week ->
                PeriodSummary.of(
                    week.first()..ReportPeriod.WEEK.endOf(week.first()),
                    plainTasks, recurringItems, habitCompletions, sessions, zone
                )
            }
        }
        // Bars compare the weeks on screen with each other, so they scale to the busiest one.
        val maxTasksCompleted = weeklySummaries.values.maxOfOrNull { it.tasksCompleted } ?: 0
        val maxHabitCheckIns = weeklySummaries.values.maxOfOrNull { it.habitCheckIns } ?: 0
        // Three rows of text would overflow the fixed-height chip at large font sizes, so the
        // number gives way there and the bars carry the week on their own.
        val showFocusTime = LocalDensity.current.fontScale <= MAX_FONT_SCALE_FOR_WEEK_NUMBER

        weeks.forEach { week ->
            val summary = weeklySummaries.getValue(week)

            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    val isCurrentMonth = day.month == focusedMonth.month
                    val isSelected = day == selectedDate
                    val isToday = day == today
                    val hasTasks = day in taskDates
                    val hasFocus = day in sessionDates
                    val hasHabits = day in habitDates

                    // Only the selected day and today get a border; plain cells stay borderless,
                    // because outlining all ~35-42 cells reads as a cage rather than a grid.
                    val cellBorderColor = when {
                        // The selected cell's own fill is neutral, so its border is what actually
                        // marks it: full-contrast `onSurface` ink, a step up from the muted
                        // hairline used elsewhere.
                        isSelected -> MaterialTheme.colorScheme.onSurface
                        isToday -> TaskColor
                        else -> Color.Transparent
                    }
                    // Padding sits outside the fixed height so the selection outline keeps clear of the dots.
                    val dayInteractionSource = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .height(45.dp)
                            .background(
                                // Selection is a *neutral* fill, not the saturated TaskColor block
                                // it used to be: a blue cell swallows the blue task dot, which
                                // forced every dot on the selected day to go white and threw away
                                // the whole point of the color coding. surfaceVariant plus the ink
                                // border reads as selected while leaving the dots legible.
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    Color.Transparent
                                },
                                shape = AppTheme.brMedium
                            )
                            .border(
                                width = AppTheme.borderHairline,
                                color = cellBorderColor,
                                shape = AppTheme.brMedium
                            )
                            .clickable(
                                interactionSource = dayInteractionSource,
                                indication = ripple(),
                            ) { onDaySelected(day) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = day.dayOfMonth.toString(),
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isSelected || isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            // The dots keep their per-category colors on every day, selected
                            // included - the color coding is the only place in the grid that says
                            // *what* a day contains, so it must survive selection.
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

                // Filled chip, deliberately borderless: these stack flush in a column of six, one
                // per week row, and an outline on each turned the column into a visible ladder
                // running down the side of the grid. The fill alone is enough to separate the
                // summary from the day cells beside it.
                WeekSummaryChip(
                    summary = summary,
                    maxTasksCompleted = maxTasksCompleted,
                    maxHabitCheckIns = maxHabitCheckIns,
                    showFocusTime = showFocusTime,
                    onClick = { onOpenWeeklyReport(week.first()) },
                    modifier = Modifier.weight(1.5f)
                )
            }
        }
    }
}

/**
 * One week row's summary: focus time as the only number (its green dots count the same sessions,
 * so it can't disagree with them), then bars for completed tasks and habit check-ins. Exact
 * values live in the weekly report and in the chip's content description.
 */
@Composable
private fun WeekSummaryChip(
    summary: PeriodSummary,
    maxTasksCompleted: Int,
    maxHabitCheckIns: Int,
    showFocusTime: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(
        R.string.calendarWeekSummaryDescription,
        summary.tasksCompleted,
        summary.habitCheckIns,
        AppDateFormatUtils.formatMinutesCompact(summary.focusMinutes)
    )
    Box(
        modifier = modifier
            .height(53.dp)
            .padding(horizontal = AppTheme.spaceXSmall)
            .clip(AppTheme.brMedium)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = AppTheme.spaceSmall)
        ) {
            if (showFocusTime) {
                Text(
                    // A dash keeps empty and future weeks quiet instead of a column of "0m".
                    text = if (summary.focusMinutes > 0) {
                        AppDateFormatUtils.formatMinutesTiny(summary.focusMinutes)
                    } else {
                        "-"
                    },
                    color = if (summary.focusMinutes > 0) {
                        SuccessColor
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(3.dp))
            }
            WeekBar(summary.tasksCompleted, maxTasksCompleted, TaskColor)
            Spacer(modifier = Modifier.height(3.dp))
            WeekBar(summary.habitCheckIns, maxHabitCheckIns, HabitColor)
        }
    }
}

@Composable
private fun WeekBar(value: Int, max: Int, color: Color) {
    NeoProgressBar(
        progress = { if (max > 0) value.toFloat() / max else 0f },
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
    )
}

/** Above this system font scale the week chip drops its number; see [WeekSummaryChip]. */
private const val MAX_FONT_SCALE_FOR_WEEK_NUMBER = 1.3f

@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(5.dp)
            .background(color, shape = CircleShape)
    )
}

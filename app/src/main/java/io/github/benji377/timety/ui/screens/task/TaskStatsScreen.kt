package io.github.benji377.timety.ui.screens.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.task.TaskWithSubtasks
import io.github.benji377.timety.ui.components.common.AccentBadge
import io.github.benji377.timety.ui.components.common.WeekNavigator
import io.github.benji377.timety.ui.components.stats.LegendDot
import io.github.benji377.timety.ui.components.stats.SectionHeader
import io.github.benji377.timety.ui.components.stats.TagBreakdownBars
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningAccent
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.ui.utils.AppUtils
import io.github.benji377.timety.ui.utils.quantityString
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.ui.viewmodel.RecurringTaskViewModel
import io.github.benji377.timety.ui.viewmodel.TagViewModel
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.datetime.AppDateUtils
import io.github.benji377.timety.util.stats.CalibrationBucket
import io.github.benji377.timety.util.stats.CalibrationInsight
import io.github.benji377.timety.util.stats.EstimationCalibrator
import io.github.benji377.timety.util.stats.StatsUtils
import io.github.benji377.timety.util.stats.TagBreakdown
import io.github.benji377.timety.util.stats.TagStats
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Shows weekly task velocity and productivity charts for the selected week, plus all-time counts
 * per tag.
 */
@Composable
fun TaskStatsScreen(
    taskViewModel: TaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    recurringViewModel: RecurringTaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    focusViewModel: FocusViewModel = activityScopedViewModel(),
    tagViewModel: TagViewModel = activityScopedViewModel(),
) {
    val tasks by taskViewModel.allTasks.collectAsState()
    val recurringItems by recurringViewModel.allRecurringTasks.collectAsState()
    val sessions by focusViewModel.allSessions.collectAsState()
    val taskTags by tagViewModel.taskTags.collectAsState()
    val recurringTags by tagViewModel.recurringTaskTags.collectAsState()
    var focusedDate by remember { mutableStateOf(LocalDate.now()) }

    // Recurring tasks feed the charts too: templates count as created once, each logged
    // occurrence as one completion.
    val recurringCreated = remember(recurringItems) { recurringItems.map { it.task.createdAt } }
    val recurringCompleted = remember(recurringItems) {
        recurringItems.flatMap { item -> item.occurrences.map { it.completedAt } }
    }

    val zone = ZoneId.systemDefault()
    val startOfWeek = AppDateUtils.startOfWeekMonday(focusedDate)
    val endOfWeekLocal = startOfWeek.plusDays(6)
    val startOfWeekInstant = startOfWeek.atStartOfDay(zone).toInstant()
    val endOfWeekInstant = endOfWeekLocal.atTime(23, 59, 59).atZone(zone).toInstant()

    val isCurrentRealWeek =
        AppDateUtils.isWithinInclusive(LocalDate.now(), startOfWeek, endOfWeekLocal)

    if (tasks.isEmpty() && recurringItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.taskStatsEmpty))
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                WeekNavigator(
                    focusedDate = focusedDate,
                    onShiftWeek = { days -> focusedDate = focusedDate.plusDays(days.toLong()) }
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Task velocity chart.
            item {
                SectionHeader(stringResource(R.string.taskStatsVelocity))
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LegendDot(WarningColor)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.taskStatsCreated),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendDot(SuccessColor)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.taskStatsCompleted),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.height(200.dp)) {
                    VelocityChart(
                        tasks,
                        recurringCreated,
                        recurringCompleted,
                        startOfWeekInstant,
                        endOfWeekInstant,
                        isCurrentRealWeek,
                        zone
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(40.dp)) }

            // Productivity bar chart.
            item {
                SectionHeader(
                    stringResource(R.string.taskStatsProductivity),
                    stringResource(R.string.taskStatsCompletedDaily)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.height(200.dp)) {
                    ProductivityChart(
                        tasks,
                        recurringCompleted,
                        startOfWeekInstant,
                        endOfWeekInstant,
                        isCurrentRealWeek,
                        zone
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(40.dp)) }

            // Tag counts, all time.
            item {
                val breakdown = remember(tasks, recurringItems, taskTags, recurringTags) {
                    TagStats.countsByTag(
                        tasks.map { it.task.id } + recurringItems.map { it.task.id },
                        taskTags + recurringTags,
                    )
                }
                TagBreakdownCard(breakdown)
            }
            item { Spacer(modifier = Modifier.height(40.dp)) }

            // Estimation calibration, all time.
            item { EstimationCalibrationCard(tasks, sessions) }
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun VelocityChart(
    tasks: List<TaskWithSubtasks>,
    recurringCreated: List<Instant>,
    recurringCompleted: List<Instant>,
    startOfWeek: Instant,
    endOfWeek: Instant,
    isCurrentRealWeek: Boolean,
    zone: ZoneId
) {
    val dailyCreated = IntArray(7)
    val dailyCompleted = IntArray(7)
    fun IntArray.countInWeek(instant: Instant) {
        if (!instant.isBefore(startOfWeek) && instant.isBefore(endOfWeek)) {
            this[instant.atZone(zone).dayOfWeek.value - 1]++
        }
    }

    tasks.forEach {
        dailyCreated.countInWeek(it.task.createdAt)
        if (it.task.isCompleted) {
            it.task.completedAt?.let { completedAt -> dailyCompleted.countInWeek(completedAt) }
        }
    }
    recurringCreated.forEach { dailyCreated.countInWeek(it) }
    recurringCompleted.forEach { dailyCompleted.countInWeek(it) }

    val maxY = StatsUtils.maxValue((dailyCreated.toList() + dailyCompleted.toList()), minimum = 5.0)

    SimpleBarChart(
        data1 = dailyCreated,
        color1 = WarningColor,
        data2 = dailyCompleted,
        color2 = SuccessColor,
        maxVal = maxY,
        isCurrentRealWeek = isCurrentRealWeek,
        zone = zone
    )
}

@Composable
private fun ProductivityChart(
    tasks: List<TaskWithSubtasks>,
    recurringCompleted: List<Instant>,
    startOfWeek: Instant,
    endOfWeek: Instant,
    isCurrentRealWeek: Boolean,
    zone: ZoneId
) {
    val dailyCompleted = IntArray(7)
    val completions = tasks.mapNotNull { if (it.task.isCompleted) it.task.completedAt else null } +
            recurringCompleted

    completions.forEach { completedAt ->
        if (!completedAt.isBefore(startOfWeek) && completedAt.isBefore(endOfWeek)) {
            dailyCompleted[completedAt.atZone(zone).dayOfWeek.value - 1]++
        }
    }

    val maxY = StatsUtils.maxValue(dailyCompleted.toList(), minimum = 5.0)

    SimpleBarChart(
        data1 = dailyCompleted,
        color1 = TaskColor,
        data2 = null,
        color2 = Color.Transparent,
        maxVal = maxY,
        isCurrentRealWeek = isCurrentRealWeek,
        zone = zone
    )
}

@Composable
private fun SimpleBarChart(
    data1: IntArray,
    color1: Color,
    data2: IntArray?,
    color2: Color,
    maxVal: Double,
    isCurrentRealWeek: Boolean,
    zone: ZoneId
) {
    val days = listOf(
        stringResource(R.string.commonWeekdayMon),
        stringResource(R.string.commonWeekdayTue),
        stringResource(R.string.commonWeekdayWed),
        stringResource(R.string.commonWeekdayThu),
        stringResource(R.string.commonWeekdayFri),
        stringResource(R.string.commonWeekdaySat),
        stringResource(R.string.commonWeekdaySun)
    )
    val todayIndex = if (isCurrentRealWeek) Instant.now().atZone(zone).dayOfWeek.value - 1 else -1
    // Add one unit of headroom above the tallest bar so it doesn't touch the top edge.
    val denom = (maxVal + 1).toFloat()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0 until 7) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                val isToday = i == todayIndex
                val barShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                // Today's bars carry a solid outline border. The bold weekday label alone is a weak
                // signal against a full-height column, and the sibling charts on the Focus and
                // Habit tabs mark today the same way - a solid border, never an opacity change.
                val todayBorder = if (isToday) {
                    Modifier.border(
                        AppTheme.borderHairline,
                        MaterialTheme.colorScheme.outline,
                        barShape
                    )
                } else {
                    Modifier
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val h1 = (data1[i].toFloat() / denom).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(h1)
                            .width(12.dp)
                            .clip(barShape)
                            .background(color1)
                            .then(todayBorder)
                    )

                    if (data2 != null) {
                        Spacer(modifier = Modifier.width(2.dp))
                        val h2 = (data2[i].toFloat() / denom).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(h2)
                                .width(12.dp)
                                .clip(barShape)
                                .background(color2)
                                .then(todayBorder)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = days[i],
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal),
                    color = if (isToday) TaskColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TagBreakdownCard(breakdown: TagBreakdown) {
    Column {
        SectionHeader(
            stringResource(R.string.taskStatsTagsTitle),
            stringResource(R.string.taskStatsTagsSubtitle),
        )
        Spacer(modifier = Modifier.height(18.dp))
        if (breakdown.totals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.taskStatsTagsEmpty))
            }
        } else {
            TagBreakdownBars(breakdown) { count ->
                quantityString(
                    R.plurals.nTasksCount,
                    count,
                    zeroRes = R.string.nTasksCountZero,
                    count
                )
            }
        }
    }
}

@Composable
private fun EstimationCalibrationCard(
    tasks: List<TaskWithSubtasks>,
    sessions: List<FocusSessionEntity>,
) {
    val buckets = remember(tasks, sessions) {
        EstimationCalibrator.buckets(
            tasks.filter { it.task.isCompleted }.map { it.task },
            sessions
        )
    }

    Column {
        SectionHeader(
            stringResource(R.string.taskStatsCalibrationTitle),
            stringResource(R.string.taskStatsCalibrationSubtitle)
        )
        Spacer(modifier = Modifier.height(18.dp))

        if (buckets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.taskStatsCalibrationEmpty))
            }
            return
        }

        buckets.forEach { bucket ->
            CalibrationBucketRow(bucket)
            Spacer(modifier = Modifier.height(12.dp))
        }

        val insight = remember(buckets) { EstimationCalibrator.insight(buckets) }
        if (insight != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = WarningAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    calibrationInsightText(insight),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WarningAccent,
                )
            }
        }
    }
}

@Composable
private fun CalibrationBucketRow(bucket: CalibrationBucket) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AccentBadge(color = TaskColor, size = 32.dp) {
            Text(
                AppUtils.getSizeEmoji(bucket.size),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TaskColor,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            "~${AppDateFormatUtils.formatMinutesCompact(bucket.avgMinutes)}",
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            quantityString(
                R.plurals.nTasksCount,
                bucket.sampleCount,
                zeroRes = R.string.nTasksCountZero,
                bucket.sampleCount
            ),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
@ReadOnlyComposable
private fun calibrationInsightText(insight: CalibrationInsight): String = when (insight) {
    is CalibrationInsight.OrderingViolation -> stringResource(
        R.string.taskStatsCalibrationOrderingViolation,
        stringResource(AppUtils.sizeLabel(insight.largerSize)),
        stringResource(AppUtils.sizeLabel(insight.smallerSize)),
    )

    is CalibrationInsight.HighSpread -> stringResource(
        R.string.taskStatsCalibrationHighSpread,
        stringResource(AppUtils.sizeLabel(insight.size)),
    )
}

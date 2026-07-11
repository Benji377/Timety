package io.github.benji377.timety.ui.screens.task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.task.TaskWithSubtasks
import io.github.benji377.timety.ui.components.common.WeekNavigator
import io.github.benji377.timety.ui.components.stats.LegendDot
import io.github.benji377.timety.ui.components.stats.SectionHeader
import io.github.benji377.timety.ui.theme.ChartDeepOrange
import io.github.benji377.timety.ui.theme.ChartTeal
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.ui.utils.quantityString
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.RecurringTaskViewModel
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import io.github.benji377.timety.util.datetime.AppDateUtils
import io.github.benji377.timety.util.stats.StatsUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt


/**
 * Shows weekly task velocity and productivity charts plus an all-time category breakdown for the
 * selected week.
 */
@Composable
fun TaskStatsScreen(
    taskViewModel: TaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    recurringViewModel: RecurringTaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val tasks by taskViewModel.allTasks.collectAsState()
    val recurringItems by recurringViewModel.allRecurringTasks.collectAsState()
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

            // Category breakdown, all time.
            item { CategoryBreakdownCard(tasks, recurringItems.map { it.task.category }) }
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
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(color1)
                    )

                    if (data2 != null) {
                        Spacer(modifier = Modifier.width(2.dp))
                        val h2 = (data2[i].toFloat() / denom).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(h2)
                                .width(12.dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(color2)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                val isToday = i == todayIndex
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
private fun CategoryBreakdownCard(
    tasks: List<TaskWithSubtasks>,
    recurringCategories: List<String>,
) {
    val uncategorized = stringResource(R.string.taskStatsCategoryUncategorized)
    val categoryCounts = remember(tasks, recurringCategories) {
        val counts = LinkedHashMap<String, Int>()
        (tasks.map { it.task.category } + recurringCategories).forEach {
            val cat = it.ifBlank { uncategorized }
            counts[cat] = (counts[cat] ?: 0) + 1
        }
        counts
    }

    if (categoryCounts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.taskStatsCategoryUnused))
        }
        return
    }

    val totalTasks = categoryCounts.values.sum()
    val entries = categoryCounts.entries.sortedByDescending { it.value }
    val colors = listOf(
        TaskColor,
        ErrorColor,
        SuccessColor,
        WarningColor,
        HabitColor,
        MaterialTheme.colorScheme.primary,
        ChartTeal,
        ChartDeepOrange
    )

    Column {
        SectionHeader(
            stringResource(R.string.taskStatsDistribution),
            stringResource(R.string.taskStatsDistributionSubtitle)
        )
        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            entries.forEachIndexed { index, entry ->
                Box(
                    modifier = Modifier
                        .weight(entry.value.toFloat())
                        .fillMaxHeight()
                        .background(colors[index % colors.size])
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        entries.forEachIndexed { index, entry ->
            val color = colors[index % colors.size]
            val percent = ((entry.value.toFloat() / totalTasks) * 100).roundToInt()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(color, CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    entry.key,
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    quantityString(
                        R.plurals.nTasksCount,
                        entry.value,
                        zeroRes = R.string.nTasksCountZero,
                        entry.value
                    ),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "$percent%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

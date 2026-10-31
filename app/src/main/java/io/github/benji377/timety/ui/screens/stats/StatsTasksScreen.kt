package io.github.benji377.timety.ui.screens.stats

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.ui.components.common.AccentBadge
import io.github.benji377.timety.ui.components.stats.LegendDot
import io.github.benji377.timety.ui.components.stats.PeriodBar
import io.github.benji377.timety.ui.components.stats.PeriodBarChart
import io.github.benji377.timety.ui.components.stats.SectionHeader
import io.github.benji377.timety.ui.components.stats.StatCard
import io.github.benji377.timety.ui.components.stats.StatsPeriodControls
import io.github.benji377.timety.ui.components.stats.TagBreakdownBars
import io.github.benji377.timety.ui.components.stats.bucketAxisLabel
import io.github.benji377.timety.ui.components.stats.comparisonText
import io.github.benji377.timety.ui.components.stats.currentBucketIndex
import io.github.benji377.timety.ui.components.stats.rememberStatsPeriodState
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningAccent
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.ui.utils.AppUtils
import io.github.benji377.timety.ui.utils.quantityString
import io.github.benji377.timety.ui.utils.rememberToday
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.ui.viewmodel.RecurringTaskViewModel
import io.github.benji377.timety.ui.viewmodel.TagViewModel
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.stats.CalibrationBucket
import io.github.benji377.timety.util.stats.CalibrationInsight
import io.github.benji377.timety.util.stats.EstimationCalibrator
import io.github.benji377.timety.util.stats.PeriodBuckets
import io.github.benji377.timety.util.stats.PeriodSummary
import io.github.benji377.timety.util.stats.TagStats
import io.github.benji377.timety.util.stats.TaskTiming
import io.github.benji377.timety.util.tag.TagFilter
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * What got done in the chosen period: completions against the period before, completed and created
 * over time, due date and finish time cards, and completions by tag. Size estimates stay all-time.
 */
@Composable
fun StatsTasksScreen(
    taskViewModel: TaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    recurringViewModel: RecurringTaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    focusViewModel: FocusViewModel = activityScopedViewModel(),
    tagViewModel: TagViewModel = activityScopedViewModel(),
) {
    val allTasks by taskViewModel.allTasks.collectAsState()
    val allRecurring by recurringViewModel.allRecurringTasks.collectAsState()
    val sessions by focusViewModel.allSessions.collectAsState()
    val taskTags by tagViewModel.taskTags.collectAsState()
    val recurringTags by tagViewModel.recurringTaskTags.collectAsState()
    val today by rememberToday()
    val state = rememberStatsPeriodState(today)
    val zone = remember { ZoneId.systemDefault() }

    if (allTasks.isEmpty() && allRecurring.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppTheme.spaceLarge),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.taskStatsEmpty), textAlign = TextAlign.Center)
        }
        return
    }

    val tagsById = remember(taskTags, recurringTags) { taskTags + recurringTags }
    val usedTags = remember(allTasks, allRecurring, tagsById) {
        TagFilter.usedTags(allTasks.map { it.task.id } + allRecurring.map { it.task.id }, tagsById)
    }
    val activeTagIds = TagFilter.activeIds(state.selectedTagIds, usedTags)
    val tasks = remember(allTasks, activeTagIds, tagsById) {
        TagFilter.filter(allTasks.map { it.task }, activeTagIds, tagsById) { it.id }
    }
    val recurring = remember(allRecurring, activeTagIds, tagsById) {
        TagFilter.filter(allRecurring, activeTagIds, tagsById) { it.task.id }
    }
    val range = state.range
    val completions = remember(tasks, recurring) {
        tasks.mapNotNull { if (it.isCompleted) it.completedAt else null } +
            recurring.flatMap { item -> item.occurrences.map { it.completedAt } }
    }
    val created = remember(tasks, recurring) {
        tasks.map { it.createdAt } + recurring.map { it.task.createdAt }
    }
    val buckets = remember(state.period, state.start) { PeriodBuckets.of(state.period, state.start) }
    val bucketsCompleted = remember(buckets, completions) { PeriodBuckets.count(buckets, completions, zone) }
    val bucketsCreated = remember(buckets, created) { PeriodBuckets.count(buckets, created, zone) }
    val done = bucketsCompleted.sum()

    val previousDone = remember(tasks, recurring, state.period, state.start, today) {
        PeriodSummary.of(
            range = state.period.comparisonRange(state.start, today),
            tasks = tasks,
            recurring = recurring,
            habitCompletions = emptyList(),
            sessions = emptyList(),
            zone = zone,
            until = state.period.comparisonCutoff(state.start, ZonedDateTime.now(zone)),
        ).tasksCompleted
    }
    val dueRate = remember(tasks, recurring, range) { TaskTiming.dueDateRate(tasks, recurring, range, zone) }
    val medianDays = remember(tasks, range) { TaskTiming.medianDaysToFinish(tasks, range, zone) }
    val tagCounts = remember(tasks, recurring, tagsById, range) {
        val completedIds = tasks
            .filter { task ->
                task.isCompleted && task.completedAt?.let { it.atZone(zone).toLocalDate() in range } == true
            }
            .map { it.id } +
            recurring.flatMap { item ->
                item.occurrences
                    .filter { it.completedAt.atZone(zone).toLocalDate() in range }
                    .map { item.task.id }
            }
        TagStats.countsByTag(completedIds, tagsById)
    }
    val bars = buckets.indices.map { index ->
        val bucket = buckets[index]
        PeriodBar(
            axisLabel = bucketAxisLabel(state.period, bucket),
            readout = stringResource(
                R.string.statsTasksBarReadout,
                bucketTitle(state.period, bucket),
                bucketsCompleted[index],
                bucketsCreated[index],
            ),
            values = listOf(bucketsCompleted[index], bucketsCreated[index]),
        )
    }
    val deltaDone = done - previousDone

    Column(modifier = Modifier.fillMaxSize()) {
        StatsPeriodControls(
            state = state,
            today = today,
            tags = usedTags,
            activeTagIds = activeTagIds,
            accentColor = TaskColor,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AppTheme.spaceLarge),
            verticalArrangement = Arrangement.spacedBy(AppTheme.space2XLarge),
        ) {
            item(key = "total") {
                Column {
                    Text(
                        pluralStringResource(R.plurals.statsTasksDone, done, done),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        comparisonText(
                            state.period,
                            state.start,
                            today,
                            (if (deltaDone >= 0) "+" else "") + "$deltaDone",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(AppTheme.spaceLarge))
                    PeriodBarChart(
                        bars = bars,
                        seriesColors = listOf(SuccessColor, WarningColor),
                        currentIndex = currentBucketIndex(buckets, today),
                        resetKey = state.period to state.start,
                    )
                    Spacer(Modifier.height(AppTheme.spaceSmall))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LegendDot(SuccessColor)
                        Spacer(Modifier.width(AppTheme.spaceXSmall))
                        Text(
                            stringResource(R.string.taskStatsCompleted),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(AppTheme.spaceLarge))
                        LegendDot(WarningColor)
                        Spacer(Modifier.width(AppTheme.spaceXSmall))
                        Text(
                            stringResource(R.string.taskStatsCreated),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item(key = "cards") {
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceMedium)) {
                    StatCard(
                        title = stringResource(R.string.statsDoneByDue),
                        value = dueRate.percent?.let { "$it%" } ?: "-",
                        icon = Icons.Filled.EventAvailable,
                        color = SuccessColor,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        title = stringResource(R.string.statsTimeToFinish),
                        value = medianDays?.let {
                            stringResource(R.string.statsDaysValue, String.format(LocalLocale.current.platformLocale, "%.1f", it))
                        } ?: "-",
                        icon = Icons.Filled.HourglassBottom,
                        color = TaskColor,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (!tagCounts.isEmpty) {
                item(key = "tags") {
                    Column {
                        SectionHeader(
                            stringResource(R.string.statsByTag),
                            stringResource(R.string.statsByTagTasksHint),
                        )
                        Spacer(Modifier.height(AppTheme.spaceLarge))
                        TagBreakdownBars(tagCounts) { count ->
                            quantityString(
                                R.plurals.nTasksCount,
                                count,
                                zeroRes = R.string.nTasksCountZero,
                                count,
                            )
                        }
                    }
                }
            }
            item(key = "calibration") { EstimationCalibrationCard(tasks, sessions) }
        }
    }
}

@Composable
private fun EstimationCalibrationCard(
    tasks: List<TaskEntity>,
    sessions: List<FocusSessionEntity>,
) {
    val buckets = remember(tasks, sessions) {
        EstimationCalibrator.buckets(tasks.filter { it.isCompleted }, sessions)
    }

    Column {
        SectionHeader(
            stringResource(R.string.taskStatsCalibrationTitle),
            stringResource(R.string.statsCalibrationHint),
        )
        Spacer(modifier = Modifier.height(18.dp))

        if (buckets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
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
                    modifier = Modifier.size(18.dp),
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
        verticalAlignment = Alignment.CenterVertically,
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
            fontWeight = FontWeight.Bold,
        )
        Text(
            quantityString(
                R.plurals.nTasksCount,
                bucket.sampleCount,
                zeroRes = R.string.nTasksCountZero,
                bucket.sampleCount,
            ),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

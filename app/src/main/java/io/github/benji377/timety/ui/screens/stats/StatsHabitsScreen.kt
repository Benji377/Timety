package io.github.benji377.timety.ui.screens.stats

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.ui.components.common.NeoProgressBar
import io.github.benji377.timety.ui.components.stats.PeriodBar
import io.github.benji377.timety.ui.components.stats.PeriodBarChart
import io.github.benji377.timety.ui.components.stats.SectionHeader
import io.github.benji377.timety.ui.components.stats.StatsPeriodControls
import io.github.benji377.timety.ui.components.stats.bucketAxisLabel
import io.github.benji377.timety.ui.components.stats.comparisonText
import io.github.benji377.timety.ui.components.stats.currentBucketIndex
import io.github.benji377.timety.ui.components.stats.rememberStatsPeriodState
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.utils.rememberToday
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.HabitViewModel
import io.github.benji377.timety.ui.viewmodel.TagViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import io.github.benji377.timety.util.habit.HabitRate
import io.github.benji377.timety.util.habit.HabitRates
import io.github.benji377.timety.util.habit.HabitStreaks
import io.github.benji377.timety.util.stats.PeriodBuckets
import io.github.benji377.timety.util.tag.TagFilter
import java.time.ZoneId

/** One habit in the period: its rate there, and its streaks, which are always all-time. */
private data class HabitRow(
    val habit: HabitEntity,
    val rate: HabitRate,
    val currentStreak: Int,
    val bestStreak: Int,
)

/**
 * How habits went in the chosen period: the share of expected check-ins against the period before,
 * check-ins over time, and each habit's rate and streaks.
 */
@Composable
fun StatsHabitsScreen(
    onOpenHabit: (String) -> Unit,
    habitViewModel: HabitViewModel = viewModel(factory = AppViewModelProvider.Factory),
    tagViewModel: TagViewModel = activityScopedViewModel(),
) {
    val allHabits by habitViewModel.habitsWithCompletions.collectAsState()
    val habitTags by tagViewModel.habitTags.collectAsState()
    val today by rememberToday()
    val state = rememberStatsPeriodState(today)
    val zone = remember { ZoneId.systemDefault() }

    if (allHabits.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppTheme.spaceLarge),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.habitStatsLabelEmpty), textAlign = TextAlign.Center)
        }
        return
    }

    val usedTags = remember(allHabits, habitTags) {
        TagFilter.usedTags(allHabits.map { it.habit.id }, habitTags)
    }
    val activeTagIds = TagFilter.activeIds(state.selectedTagIds, usedTags)
    val habits = remember(allHabits, activeTagIds, habitTags) {
        TagFilter.filter(allHabits, activeTagIds, habitTags) { it.habit.id }
    }
    val datesByHabit = remember(habits) {
        habits.associate { item ->
            item.habit.id to item.completions.map { it.completionDate.atZone(zone).toLocalDate() }
        }
    }
    val range = state.range
    val rows = remember(habits, datesByHabit, range, today) {
        habits.map { item ->
            val dates = datesByHabit.getValue(item.habit.id)
            val (current, best) = HabitStreaks.streaks(item.habit, dates, today)
            HabitRow(item.habit, HabitRates.of(item.habit, dates, range, today, zone), current, best)
        }.sortedWith(compareByDescending<HabitRow> { it.rate.percent ?: -1 }.thenBy { it.habit.name.lowercase() })
    }
    val rate = HabitRates.total(rows.map { it.rate })
    val previousRate = remember(habits, datesByHabit, state.period, state.start, today) {
        val previous = state.period.comparisonRange(state.start, today)
        HabitRates.total(
            habits.map { item ->
                HabitRates.of(
                    item.habit,
                    datesByHabit.getValue(item.habit.id),
                    previous,
                    previous.endInclusive,
                    zone,
                )
            }
        )
    }
    val buckets = remember(state.period, state.start) { PeriodBuckets.of(state.period, state.start) }
    val bucketCheckIns = remember(buckets, habits) {
        PeriodBuckets.count(buckets, habits.flatMap { h -> h.completions.map { it.completionDate } }, zone)
    }
    val bars = buckets.indices.map { index ->
        val count = bucketCheckIns[index]
        PeriodBar(
            axisLabel = bucketAxisLabel(state.period, buckets[index]),
            readout = stringResource(
                R.string.statsBarReadout,
                bucketTitle(state.period, buckets[index]),
                pluralStringResource(R.plurals.statsCheckIns, count, count),
            ),
            values = listOf(count),
        )
    }
    val currentPercent = rate.percent
    val previousPercent = previousRate.percent

    Column(modifier = Modifier.fillMaxSize()) {
        StatsPeriodControls(
            state = state,
            today = today,
            tags = usedTags,
            activeTagIds = activeTagIds,
            accentColor = HabitColor,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AppTheme.spaceLarge),
        ) {
            item(key = "total") {
                Column(modifier = Modifier.padding(bottom = AppTheme.space2XLarge)) {
                    Text(
                        currentPercent?.let { "$it%" } ?: "-",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        stringResource(R.string.statsHabitsRateCaption),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (currentPercent != null && previousPercent != null) {
                        val delta = currentPercent - previousPercent
                        Text(
                            comparisonText(
                                state.period,
                                state.start,
                                today,
                                (if (delta >= 0) "+" else "") + "$delta%",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(AppTheme.spaceLarge))
                    PeriodBarChart(
                        bars = bars,
                        seriesColors = listOf(HabitColor),
                        currentIndex = currentBucketIndex(buckets, today),
                        resetKey = state.period to state.start,
                    )
                }
            }
            item(key = "header") {
                SectionHeader(stringResource(R.string.statsYourHabits))
                Spacer(Modifier.height(AppTheme.spaceSmall))
            }
            items(rows, key = { it.habit.id }) { row ->
                HabitStatsRow(row, onClick = { onOpenHabit(row.habit.id) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun HabitStatsRow(row: HabitRow, onClick: () -> Unit) {
    val percent = row.rate.percent
    val streak = streakText(row.habit, row.currentStreak)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = AppTheme.spaceMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    row.habit.name,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(AppTheme.spaceSmall))
                Text(percent?.let { "$it%" } ?: "-", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(AppTheme.spaceXSmall))
            NeoProgressBar(
                progress = { (percent ?: 0) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Color(row.habit.colorValue),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Spacer(Modifier.height(AppTheme.spaceXSmall))
            Text(
                if (percent != null) {
                    stringResource(R.string.statsHabitRow, row.rate.done, row.rate.expected, streak, row.bestStreak)
                } else {
                    stringResource(R.string.statsHabitRowNoRate, streak, row.bestStreak)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A streak in the steps [habit] is scheduled in: days, weeks or months. */
@Composable
@ReadOnlyComposable
private fun streakText(habit: HabitEntity, streak: Int): String = pluralStringResource(
    when (habit.frequency) {
        HabitFrequency.DAILY, HabitFrequency.WEEKLY_EXACT -> R.plurals.statsStreakDays
        HabitFrequency.WEEKLY_FLEXIBLE -> R.plurals.statsStreakWeeks
        HabitFrequency.MONTHLY_FLEXIBLE -> R.plurals.statsStreakMonths
    },
    streak,
    streak,
)

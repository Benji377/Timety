package io.github.benji377.timety.ui.screens.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.components.stats.PeriodBar
import io.github.benji377.timety.ui.components.stats.PeriodBarChart
import io.github.benji377.timety.ui.components.stats.SectionHeader
import io.github.benji377.timety.ui.components.stats.StatCard
import io.github.benji377.timety.ui.components.stats.StatCardStyle
import io.github.benji377.timety.ui.components.stats.StatsPeriodControls
import io.github.benji377.timety.ui.components.stats.TagBreakdownBars
import io.github.benji377.timety.ui.components.stats.bucketAxisLabel
import io.github.benji377.timety.ui.components.stats.comparisonText
import io.github.benji377.timety.ui.components.stats.currentBucketIndex
import io.github.benji377.timety.ui.components.stats.rememberStatsPeriodState
import io.github.benji377.timety.ui.screens.focus.FocusHeatmap
import io.github.benji377.timety.ui.screens.focus.TargetBreakdownSection
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.ui.utils.rememberToday
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import io.github.benji377.timety.ui.viewmodel.TagViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.stats.FocusPeriodStats
import io.github.benji377.timety.util.stats.FocusSessionDayStats
import io.github.benji377.timety.util.stats.PeriodBuckets
import io.github.benji377.timety.util.stats.PeriodSummary
import io.github.benji377.timety.util.stats.ReportPeriod
import io.github.benji377.timety.util.stats.TagStats
import io.github.benji377.timety.util.tag.TagFilter
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle

/**
 * How much was focused in the chosen period: the total against the period before, bars over time,
 * cards, and breakdowns by tag and target, plus the year's heatmap. The tag filter applies to all.
 */
@Composable
fun StatsTimeScreen(
    focusViewModel: FocusViewModel = activityScopedViewModel(),
    tagViewModel: TagViewModel = activityScopedViewModel(),
    settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val sessions by focusViewModel.allSessions.collectAsState()
    val distractions by focusViewModel.allDistractions.collectAsState()
    val sessionTags by tagViewModel.sessionTags.collectAsState()
    val dailyTarget by settingsViewModel.dailyFocusTarget.collectAsState()
    val today by rememberToday()
    val state = rememberStatsPeriodState(today)
    val zone = remember { ZoneId.systemDefault() }

    if (sessions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.focusStatsEmpty))
        }
        return
    }

    val usedTags = remember(sessions, sessionTags) {
        TagFilter.usedTags(sessions.map { it.id }, sessionTags)
    }
    val activeTagIds = TagFilter.activeIds(state.selectedTagIds, usedTags)
    val filtered = remember(sessions, activeTagIds, sessionTags) {
        TagFilter.filter(sessions, activeTagIds, sessionTags) { it.id }
    }
    val range = state.range
    val minutesByDay = remember(filtered) { FocusSessionDayStats.minutesByDay(filtered, zone) }
    val buckets = remember(state.period, state.start) { PeriodBuckets.of(state.period, state.start) }
    val bucketMinutes = remember(buckets, minutesByDay) { PeriodBuckets.sum(buckets, minutesByDay) }
    val total = bucketMinutes.sum()

    val previousMinutes = remember(filtered, state.period, state.start, today) {
        PeriodSummary.of(
            range = state.period.comparisonRange(state.start, today),
            tasks = emptyList(),
            recurring = emptyList(),
            habitCompletions = emptyList(),
            sessions = filtered,
            zone = zone,
            until = state.period.comparisonCutoff(state.start, ZonedDateTime.now(zone)),
        ).focusMinutes
    }
    val change = FocusPeriodStats.percentChange(total, previousMinutes)
    val averageSession = remember(filtered, range) {
        FocusPeriodStats.averageSessionMinutes(filtered, range, zone)
    }
    // The daily target counts all focus, so a tag filter doesn't change whether a day reached it.
    val goalDays = remember(sessions, dailyTarget, range, today) {
        FocusPeriodStats.goalDays(
            FocusSessionDayStats.minutesByDay(sessions, zone),
            dailyTarget::minutesOn,
            range,
            today,
        )
    }
    val distractionCount = remember(distractions, activeTagIds, sessionTags, range) {
        distractions.count { entry ->
            entry.distraction.time.atZone(zone).toLocalDate() in range &&
                TagFilter.matches(sessionTags[entry.session.id].orEmpty(), activeTagIds)
        }
    }
    val tagMinutes = remember(filtered, sessionTags, range) {
        TagStats.minutesByTag(filtered, sessionTags, range.start, range.endInclusive, zone)
    }
    val bars = buckets.indices.map { index ->
        val bucket = buckets[index]
        PeriodBar(
            axisLabel = bucketAxisLabel(state.period, bucket),
            readout = stringResource(
                R.string.statsBarReadout,
                bucketTitle(state.period, bucket),
                AppDateFormatUtils.formatMinutesCompact(bucketMinutes[index]),
            ),
            values = listOf(bucketMinutes[index]),
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        StatsPeriodControls(
            state = state,
            today = today,
            tags = usedTags,
            activeTagIds = activeTagIds,
            accentColor = FocusColor,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AppTheme.spaceLarge),
            verticalArrangement = Arrangement.spacedBy(AppTheme.space2XLarge),
        ) {
            item(key = "total") {
                Column {
                    Text(
                        AppDateFormatUtils.formatMinutesCompact(total),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                    )
                    if (change != null) {
                        Text(
                            comparisonText(
                                state.period,
                                state.start,
                                today,
                                (if (change >= 0) "+" else "") + "$change%",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(AppTheme.spaceLarge))
                    PeriodBarChart(
                        bars = bars,
                        seriesColors = listOf(FocusColor),
                        currentIndex = currentBucketIndex(buckets, today),
                        resetKey = state.period to state.start,
                    )
                }
            }
            item(key = "cards") {
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall)) {
                    StatCard(
                        title = stringResource(R.string.statsAvgSession),
                        value = averageSession?.let(AppDateFormatUtils::formatMinutesCompact) ?: "-",
                        icon = Icons.Filled.Timer,
                        color = FocusColor,
                        style = StatCardStyle.COMPACT_VERTICAL,
                        modifier = Modifier.weight(1f),
                    )
                    if (goalDays.total > 0) {
                        StatCard(
                            title = stringResource(R.string.statsDaysGoalMet),
                            value = "${goalDays.met} / ${goalDays.total}",
                            icon = Icons.Filled.TrackChanges,
                            color = WarningColor,
                            style = StatCardStyle.COMPACT_VERTICAL,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    StatCard(
                        title = stringResource(R.string.statsDistractions),
                        value = "$distractionCount",
                        icon = Icons.Filled.WarningAmber,
                        color = WarningColor,
                        style = StatCardStyle.COMPACT_VERTICAL,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (!tagMinutes.isEmpty) {
                item(key = "tags") {
                    Column {
                        SectionHeader(
                            stringResource(R.string.statsByTag),
                            stringResource(R.string.statsByTagSessionsHint),
                        )
                        Spacer(Modifier.height(AppTheme.spaceLarge))
                        TagBreakdownBars(tagMinutes) { AppDateFormatUtils.formatMinutesCompact(it) }
                    }
                }
            }
            item(key = "targets") { TargetBreakdownSection(filtered, sessionTags, range, zone) }
            item(key = "heatmap") { FocusHeatmap(filtered) }
        }
    }
}

/** What a bar of [period] covers, for the line above the chart. */
@Composable
internal fun bucketTitle(period: ReportPeriod, bucket: ClosedRange<LocalDate>): String =
    when (period) {
        ReportPeriod.WEEK -> AppDateFormatUtils.formatWeekdayDay(bucket.start)
        ReportPeriod.MONTH -> stringResource(
            R.string.focusHistoryDateRange,
            AppDateFormatUtils.formatShortDate(bucket.start),
            AppDateFormatUtils.formatShortDate(bucket.endInclusive),
        )

        ReportPeriod.YEAR -> bucket.start.month.getDisplayName(TextStyle.FULL, LocalLocale.current.platformLocale)
    }

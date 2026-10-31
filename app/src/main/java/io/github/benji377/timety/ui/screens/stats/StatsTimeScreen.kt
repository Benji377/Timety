package io.github.benji377.timety.ui.screens.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.components.stats.PeriodBar
import io.github.benji377.timety.ui.components.stats.PeriodBarChart
import io.github.benji377.timety.ui.components.stats.SectionHeader
import io.github.benji377.timety.ui.components.stats.StatTile
import io.github.benji377.timety.ui.components.stats.StatTileRow
import io.github.benji377.timety.ui.components.stats.StatsEmptyState
import io.github.benji377.timety.ui.components.stats.StatsPeriodPage
import io.github.benji377.timety.ui.components.stats.StatsTotal
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
import io.github.benji377.timety.util.stats.FocusHeatmapBucketer
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
        StatsEmptyState(stringResource(R.string.focusStatsEmpty))
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
        PeriodSummary.comparison(state.period, state.start, ZonedDateTime.now(zone), sessions = filtered)
            .focusMinutes
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
    // Held here because a lazy item forgets what it remembers when scrolled out of view.
    val heatmap = remember(filtered, today) { FocusHeatmapBucketer.grid(filtered, today, zone) }
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

    StatsPeriodPage(
        state = state,
        today = today,
        tags = usedTags,
        activeTagIds = activeTagIds,
        accentColor = FocusColor,
    ) {
        item(key = "total") {
            Column {
                StatsTotal(
                    AppDateFormatUtils.formatMinutesCompact(total),
                    comparison = change?.let { comparisonText(state.period, state.start, today, it, "%") },
                )
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
            StatTileRow(
                listOfNotNull(
                    StatTile(
                        stringResource(R.string.statsAvgSession),
                        averageSession?.let(AppDateFormatUtils::formatMinutesCompact) ?: "-",
                        Icons.Filled.Timer,
                        FocusColor,
                    ),
                    if (goalDays.total > 0) {
                        StatTile(
                            stringResource(R.string.statsDaysGoalMet),
                            "${goalDays.met} / ${goalDays.total}",
                            Icons.Filled.TrackChanges,
                            WarningColor,
                        )
                    } else {
                        null
                    },
                    StatTile(
                        stringResource(R.string.statsDistractions),
                        "$distractionCount",
                        Icons.Filled.WarningAmber,
                        WarningColor,
                    ),
                )
            )
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
        item(key = "heatmap") { FocusHeatmap(heatmap) }
    }
}

/** What a bar of [period] covers, for the line above the chart. */
@Composable
@ReadOnlyComposable
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

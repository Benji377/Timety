package io.github.benji377.timety.ui.screens.stats

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.components.common.NeoCard
import io.github.benji377.timety.ui.components.mood.MoodsBestFirst
import io.github.benji377.timety.ui.components.mood.color
import io.github.benji377.timety.ui.components.mood.labelRes
import io.github.benji377.timety.ui.components.stats.BreakdownBar
import io.github.benji377.timety.ui.components.stats.LegendLabel
import io.github.benji377.timety.ui.components.stats.SectionHeader
import io.github.benji377.timety.ui.components.stats.StatsEmptyState
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.UserColor
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.ui.utils.rememberToday
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.DayRatingViewModel
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.ui.viewmodel.HabitViewModel
import io.github.benji377.timety.ui.viewmodel.RecurringTaskViewModel
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.stats.DayPart
import io.github.benji377.timety.util.stats.FocusPatterns
import io.github.benji377.timety.util.stats.FocusPeak
import io.github.benji377.timety.util.stats.FocusSessionDayStats
import io.github.benji377.timety.util.stats.TaskTiming
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.format.TextStyle
import kotlin.math.roundToInt

/**
 * Recurring patterns over a fixed window, led by labelled highlights. Each part hides until there is
 * enough data for it, so a few days are never presented as a habit.
 */
@Composable
fun StatsPatternsScreen(
    focusViewModel: FocusViewModel = activityScopedViewModel(),
    taskViewModel: TaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    recurringViewModel: RecurringTaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    habitViewModel: HabitViewModel = viewModel(factory = AppViewModelProvider.Factory),
    dayRatingViewModel: DayRatingViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val sessions by focusViewModel.allSessions.collectAsState()
    val distractions by focusViewModel.allDistractions.collectAsState()
    val tasks by taskViewModel.allTasks.collectAsState()
    val recurring by recurringViewModel.allRecurringTasks.collectAsState()
    val habits by habitViewModel.habitsWithCompletions.collectAsState()
    val ratings by dayRatingViewModel.ratingsByDay.collectAsState()
    val today by rememberToday()
    val zone = remember { ZoneId.systemDefault() }
    val locale = LocalLocale.current.platformLocale
    val window = remember(today) { FocusPatterns.window(today) }

    val peak = remember(sessions, window) { FocusPatterns.focusPeak(sessions, window, zone) }
    val slots = remember(sessions, window) { FocusPatterns.weekdaySlots(sessions, window, zone) }
    val minutesByDay = remember(sessions) { FocusSessionDayStats.minutesByDay(sessions, zone) }
    val averages = remember(minutesByDay, window) {
        FocusPatterns.averageMinutesByWeekday(minutesByDay, window)
    }
    val busiestDay = remember(tasks, recurring, window) {
        val completions = TaskTiming.completions(tasks.map { it.task }, recurring)
        FocusPatterns.busiestTaskWeekday(completions, window, zone)
    }
    val distractionMinute = remember(distractions, window) {
        FocusPatterns.medianDistractionMinute(
            distractions.map { it.session to it.distraction },
            window,
            zone,
        )
    }?.takeIf { it > 0 }
    val checkInParts = remember(habits, window) {
        FocusPatterns.completionsByDayPart(
            habits.flatMap { h -> h.completions.map { it.completionDate } },
            window,
            zone,
        )
    }.takeIf { parts -> parts.values.sum() >= FocusPatterns.MIN_CHECK_INS }
    val moodFocus = remember(ratings, minutesByDay, window) {
        ratings?.let { FocusPatterns.focusByMood(it, minutesByDay, window) }
    }
    val ratedDays = ratings?.keys?.count { it in window } ?: 0

    val highlights = listOfNotNull(
        peak?.let { stringResource(R.string.statsPatternPeakLabel) to focusPeakValue(it, TextStyle.FULL) },
        busiestDay?.let {
            stringResource(R.string.statsPatternBusiestLabel) to it.getDisplayName(TextStyle.FULL, locale)
        },
        distractionMinute?.let {
            stringResource(R.string.statsPatternDistractedLabel) to AppDateFormatUtils.formatMinutesCompact(it)
        },
    )

    if (highlights.isEmpty() && checkInParts == null && moodFocus == null) {
        StatsEmptyState(stringResource(R.string.statsPatternsEmpty, FocusPatterns.WINDOW_WEEKS.toInt()))
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppTheme.spaceLarge),
        verticalArrangement = Arrangement.spacedBy(AppTheme.space2XLarge),
    ) {
        item(key = "highlights") {
            Column {
                Text(
                    stringResource(R.string.statsPatternsWindow, FocusPatterns.WINDOW_WEEKS.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (highlights.isNotEmpty()) {
                    Spacer(Modifier.height(AppTheme.spaceSmall))
                    NeoCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(horizontal = AppTheme.spaceLarge)) {
                            highlights.forEachIndexed { index, (label, value) ->
                                if (index > 0) HorizontalDivider()
                                HighlightRow(label, value)
                            }
                        }
                    }
                }
            }
        }
        // The same minimum as the highlight about it: a grid of a few sessions shows no pattern.
        if (peak != null) {
            item(key = "grid") { WeekdaySlotGrid(slots) }
            item(key = "weekdays") {
                Column {
                    SectionHeader(
                        stringResource(R.string.statsPatternsByWeekday),
                        stringResource(R.string.statsPatternsByWeekdayHint),
                    )
                    Spacer(Modifier.height(AppTheme.spaceLarge))
                    val max = (averages.values.maxOrNull() ?: 0).coerceAtLeast(1).toFloat()
                    DayOfWeek.entries.forEach { weekday ->
                        val minutes = averages[weekday] ?: 0
                        BreakdownBar(
                            label = weekday.getDisplayName(TextStyle.SHORT, locale),
                            color = FocusColor,
                            value = AppDateFormatUtils.formatMinutesCompact(minutes),
                            fraction = minutes / max,
                        )
                    }
                }
            }
        }
        if (checkInParts != null) {
            item(key = "habits") { HabitTiming(checkInParts) }
        }
        if (moodFocus != null) {
            item(key = "mood") {
                Column {
                    SectionHeader(
                        stringResource(R.string.statsPatternsByMood),
                        stringResource(R.string.statsPatternsByMoodHint, ratedDays),
                    )
                    Spacer(Modifier.height(AppTheme.spaceLarge))
                    val max = (moodFocus.values.maxOrNull() ?: 0).coerceAtLeast(1).toFloat()
                    MoodsBestFirst.forEach { mood ->
                        moodFocus[mood]?.let { minutes ->
                            BreakdownBar(
                                label = stringResource(mood.labelRes),
                                color = mood.color,
                                value = AppDateFormatUtils.formatMinutesCompact(minutes),
                                fraction = minutes / max,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppTheme.spaceMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(AppTheme.spaceMedium))
        Text(value, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

/** When focus peaks, as a weekday in [weekdayStyle] and a part of the day; also on the overview. */
@Composable
@ReadOnlyComposable
internal fun focusPeakValue(peak: FocusPeak, weekdayStyle: TextStyle): String = stringResource(
    R.string.statsPatternPeakValue,
    peak.weekday.getDisplayName(weekdayStyle, LocalLocale.current.platformLocale),
    dayPartLabel(peak.dayPart),
)

@Composable
private fun WeekdaySlotGrid(slots: List<List<Int>>) {
    val locale = LocalLocale.current.platformLocale
    val max = slots.flatten().maxOrNull()?.coerceAtLeast(1) ?: 1
    val empty = MaterialTheme.colorScheme.surfaceVariant
    val slotCount = slots.first().size
    val labelWidth = 48.dp

    Column {
        SectionHeader(
            stringResource(R.string.statsPatternsGrid),
            stringResource(R.string.statsPatternsGridHint),
        )
        Spacer(Modifier.height(AppTheme.spaceMedium))
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(labelWidth))
            DayOfWeek.entries.forEach { weekday ->
                Text(
                    weekday.getDisplayName(TextStyle.NARROW, locale),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(AppTheme.spaceXSmall))
        (0 until slotCount).forEach { slot ->
            val from = slot * FocusPatterns.HOURS_PER_SLOT
            val slotLabel = "$from-${from + FocusPatterns.HOURS_PER_SLOT}"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    slotLabel,
                    modifier = Modifier.width(labelWidth),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DayOfWeek.entries.forEach { weekday ->
                    val minutes = slots[weekday.value - 1][slot]
                    val description = "${weekday.getDisplayName(TextStyle.FULL, locale)} $slotLabel: " +
                        AppDateFormatUtils.formatMinutesCompact(minutes)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(1.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (minutes == 0) empty else FocusColor.copy(alpha = 0.2f + 0.8f * minutes / max)
                            )
                            .clearAndSetSemantics { contentDescription = description },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitTiming(parts: Map<DayPart, Int>) {
    val total = parts.values.sum().coerceAtLeast(1)
    Column {
        SectionHeader(stringResource(R.string.statsPatternsHabitTiming))
        Spacer(Modifier.height(AppTheme.spaceMedium))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(999.dp)),
        ) {
            DayPart.entries.forEach { part ->
                val count = parts[part] ?: 0
                if (count > 0) {
                    Box(
                        modifier = Modifier
                            .weight(count.toFloat())
                            .fillMaxHeight()
                            .background(part.chartColor),
                    )
                }
            }
        }
        Spacer(Modifier.height(AppTheme.spaceMedium))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceMedium),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spaceXSmall),
        ) {
            DayPart.entries.forEach { part ->
                LegendLabel(
                    part.chartColor,
                    "${dayPartLabel(part)} ${((parts[part] ?: 0) * 100f / total).roundToInt()}%",
                )
            }
        }
    }
}

private val DayPart.chartColor: Color
    get() = when (this) {
        DayPart.MORNING -> WarningColor
        DayPart.AFTERNOON -> TaskColor
        DayPart.EVENING -> HabitColor
        DayPart.NIGHT -> UserColor
    }

@Composable
@ReadOnlyComposable
private fun dayPartLabel(part: DayPart): String {
    @StringRes val res = when (part) {
        DayPart.MORNING -> R.string.commonDaytimeMorning
        DayPart.AFTERNOON -> R.string.commonDaytimeAfternoon
        DayPart.EVENING -> R.string.commonDaytimeEvening
        DayPart.NIGHT -> R.string.commonDaytimeNight
    }
    return stringResource(res)
}

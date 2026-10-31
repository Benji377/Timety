package io.github.benji377.timety.ui.components.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.tag.TagEntity
import io.github.benji377.timety.ui.components.common.NeoIconButton
import io.github.benji377.timety.ui.components.common.NeoSegmentedSelector
import io.github.benji377.timety.ui.components.common.TagFilterRow
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.stats.ReportPeriod
import java.time.LocalDate

/**
 * The period and tags a stats page shows; each page keeps its own while it is open. The shown period
 * is the one around a remembered day rather than a stored start, because a period's first day can
 * belong to an earlier week: switching from it would otherwise walk back a little on every switch.
 */
@Stable
class StatsPeriodState(period: ReportPeriod, anchor: LocalDate, tagIds: Set<String>) {
    var period by mutableStateOf(period)
        private set
    private var anchor by mutableStateOf(anchor)
    var selectedTagIds by mutableStateOf(tagIds)

    val start: LocalDate get() = period.startOf(anchor)

    val range: ClosedRange<LocalDate> get() = start..period.endOf(start)

    /** Switches to [newPeriod], staying around the shown dates. */
    fun selectPeriod(newPeriod: ReportPeriod) {
        period = newPeriod
    }

    fun showPrevious() {
        anchor = period.previousStart(anchor)
    }

    /** Moves forward one period, never remembering a day after [today]. */
    fun showNext(today: LocalDate) {
        anchor = minOf(period.nextStart(anchor), today)
    }

    companion object {
        val Saver: Saver<StatsPeriodState, Any> = listSaver(
            save = { state ->
                listOf(state.period.name, state.anchor.toEpochDay(), ArrayList(state.selectedTagIds))
            },
            restore = { saved ->
                StatsPeriodState(
                    period = ReportPeriod.valueOf(saved[0] as String),
                    anchor = LocalDate.ofEpochDay(saved[1] as Long),
                    tagIds = (saved[2] as List<*>).filterIsInstance<String>().toSet(),
                )
            },
        )
    }
}

/** A [StatsPeriodState] starting on the week of [today]. */
@Composable
fun rememberStatsPeriodState(today: LocalDate): StatsPeriodState =
    rememberSaveable(saver = StatsPeriodState.Saver) {
        StatsPeriodState(ReportPeriod.WEEK, today, emptySet())
    }

/** Week, month or year, the arrows through them, and the [tags] to narrow a page to. */
@Composable
fun StatsPeriodControls(
    state: StatsPeriodState,
    today: LocalDate,
    tags: List<TagEntity>,
    activeTagIds: Set<String>,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val periodLabels = ReportPeriod.entries.associateWith { periodLabel(it) }
    Column(modifier = modifier.fillMaxWidth()) {
        NeoSegmentedSelector(
            values = ReportPeriod.entries,
            selectedValue = state.period,
            isEditing = true,
            onSelected = state::selectPeriod,
            labelBuilder = periodLabels::getValue,
            activeBgColor = accentColor,
            modifier = Modifier.padding(
                start = AppTheme.spaceLarge,
                end = AppTheme.spaceLarge,
                top = AppTheme.spaceMedium,
            ),
        )
        PeriodNavigator(
            state = state,
            today = today,
            modifier = Modifier.padding(
                horizontal = AppTheme.spaceLarge,
                vertical = AppTheme.spaceSmall,
            ),
        )
        TagFilterRow(
            tags = tags,
            selectedIds = activeTagIds,
            onSelectionChange = { state.selectedTagIds = it },
            contentPadding = PaddingValues(horizontal = AppTheme.spaceLarge),
            modifier = Modifier.padding(bottom = AppTheme.spaceSmall),
        )
        HorizontalDivider()
    }
}

/** The shown period between arrows; the forward arrow stops at the current one. */
@Composable
fun PeriodNavigator(
    state: StatsPeriodState,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NeoIconButton(
            onClick = state::showPrevious,
            icon = Icons.Filled.ChevronLeft,
            contentDescription = stringResource(R.string.commonLabelPrevious),
        )
        Text(periodTitle(state.period, state.start, today), fontWeight = FontWeight.Bold)
        NeoIconButton(
            onClick = { state.showNext(today) },
            icon = Icons.Filled.ChevronRight,
            contentDescription = stringResource(R.string.commonLabelNext),
            enabled = state.range.endInclusive.isBefore(today),
        )
    }
}

/**
 * A line saying what [delta] compares against: the same days of the previous period while this one
 * is running, otherwise the whole previous period. [suffix] follows the signed number.
 */
@Composable
@ReadOnlyComposable
fun comparisonText(
    period: ReportPeriod,
    start: LocalDate,
    today: LocalDate,
    delta: Int,
    suffix: String = "",
): String {
    val running = period.isRunning(start, today)
    val res = when (period) {
        ReportPeriod.WEEK -> if (running) R.string.statsCompareRunningWeek else R.string.statsCompareFinishedWeek
        ReportPeriod.MONTH -> if (running) R.string.statsCompareRunningMonth else R.string.statsCompareFinishedMonth
        ReportPeriod.YEAR -> if (running) R.string.statsCompareRunningYear else R.string.statsCompareFinishedYear
    }
    val sign = if (delta >= 0) "+" else ""
    return stringResource(res, "$sign$delta$suffix")
}

@Composable
@ReadOnlyComposable
private fun periodLabel(period: ReportPeriod): String = stringResource(
    when (period) {
        ReportPeriod.WEEK -> R.string.statsPeriodWeek
        ReportPeriod.MONTH -> R.string.statsPeriodMonth
        ReportPeriod.YEAR -> R.string.statsPeriodYear
    }
)

@Composable
@ReadOnlyComposable
private fun periodTitle(period: ReportPeriod, start: LocalDate, today: LocalDate): String =
    when (period) {
        ReportPeriod.WEEK -> {
            val end = period.endOf(start)
            val dateFormat = LocalDateFormatSettings.current.dateFormatCode
            // Short dates leave out the year, so older weeks use the full format to stay clear.
            fun format(day: LocalDate) = if (day.year == today.year) {
                AppDateFormatUtils.formatShortDate(day)
            } else {
                AppDateFormatUtils.formatDate(day, dateFormat)
            }
            stringResource(R.string.focusHistoryDateRange, format(start), format(end))
        }

        ReportPeriod.MONTH -> AppDateFormatUtils.formatMonthYear(start)
        ReportPeriod.YEAR -> start.year.toString()
    }

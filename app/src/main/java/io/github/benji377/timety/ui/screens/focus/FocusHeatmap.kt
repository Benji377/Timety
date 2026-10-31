package io.github.benji377.timety.ui.screens.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.color
import io.github.benji377.timety.ui.components.stats.SectionHeader
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.LocalSnackbarHostState
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.stats.HeatmapGrid
import io.github.benji377.timety.util.stats.HeatmapDay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.util.Locale

// Trailing-365-day contribution grid, GitHub-style: 53 Monday-start week columns of 7 day cells,
// a fixed weekday gutter on the left, 3-letter month labels on top, and a Less→More legend below.
@Composable
internal fun FocusHeatmap(grid: HeatmapGrid) {
    val locale = remember { Locale.getDefault() }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current
    val dateFormatCode = LocalDateFormatSettings.current.dateFormatCode

    // Anchor on the current week (rightmost column) by default, same as GitHub's own graph:
    // scrolling right reveals nothing (there's no future data), only scrolling left digs into
    // history. `reverseScrolling` looked like the natural fit for this but proved unreliable
    // once the grid gained a sibling (the weekday gutter) in the same Row - maxValue ended up
    // stale on first layout. Scrolling explicitly once it's known is more robust.
    LaunchedEffect(grid) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    // Labels the week column containing the 1st of a month, skipping a repeat if the previous
    // label already named that month (a short month can otherwise land two labels back to back).
    val monthLabels = remember(grid, locale) {
        var lastLabeledMonth: Int? = null
        grid.weeks.map { week ->
            val firstOfMonth = week.days.firstOrNull { it.date.dayOfMonth == 1 }
            if (firstOfMonth != null && firstOfMonth.date.monthValue != lastLabeledMonth) {
                lastLabeledMonth = firstOfMonth.date.monthValue
                firstOfMonth.date.month.getDisplayName(java.time.format.TextStyle.SHORT, locale)
            } else {
                null
            }
        }
    }

    // Monday-start week rows (0=Mon..6=Sun); only every other row gets a label, matching
    // GitHub's own graph, so the gutter doesn't turn into a solid wall of text.
    val weekdayLabels = remember(locale) {
        mapOf(
            0 to DayOfWeek.MONDAY,
            2 to DayOfWeek.WEDNESDAY,
            4 to DayOfWeek.FRIDAY,
        ).mapValues { (_, day) -> day.getDisplayName(java.time.format.TextStyle.SHORT, locale) }
    }

    Column {
        SectionHeader(
            stringResource(R.string.focusStatsSectionHeatmapTitle),
            stringResource(R.string.focusStatsSectionHeatmapSubtitle),
        )
        Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
        Row {
            // Fixed gutter - stays put while only the grid below scrolls horizontally. Each slot
            // is exactly cell-sized (11.dp) so the rows line up with the grid regardless of font
            // metrics; the label itself is allowed to render past that box (see HeatmapAxisLabel)
            // instead of being vertically clipped or - worse - widening the row and desyncing it
            // from the grid.
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Spacer(
                    modifier = Modifier
                        .width(28.dp)
                        .height(16.dp)
                )
                for (row in 0..6) {
                    HeatmapAxisLabel(
                        text = weekdayLabels[row] ?: "",
                        fontSize = 10.sp,
                        modifier = Modifier
                            .width(28.dp)
                            .height(11.dp),
                    )
                }
            }
            Row(
                // horizontalScroll doesn't clip: without this, cells scrolled past the left
                // edge draw their rounded borders on top of the weekday gutter.
                modifier = Modifier
                    .clipToBounds()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                grid.weeks.forEachIndexed { index, week ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        HeatmapAxisLabel(
                            text = monthLabels[index] ?: "",
                            fontSize = 10.sp,
                            modifier = Modifier
                                .width(11.dp)
                                .height(16.dp),
                        )
                        week.days.forEach { day ->
                            HeatmapCell(
                                day = day,
                                level = grid.intensityLevel(day.minutes),
                                onTap = {
                                    val dateStr = AppDateFormatUtils.formatDate(
                                        day.date,
                                        dateFormatCode,
                                        locale
                                    )
                                    val minutesStr =
                                        AppDateFormatUtils.formatMinutesCompact(day.minutes)
                                    scope.launch { snackbarHostState.showSnackbar("$dateStr: $minutesStr") }
                                },
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(AppTheme.spaceMedium))
        HeatmapLegend()
    }
}

// A label that occupies exactly [modifier]'s size for layout purposes (so it never widens its
// row/column or desyncs the grid) while rendering at its natural, unclipped size - matching how
// GitHub's own SVG month/weekday labels overflow their anchor cell instead of being constrained
// to it.
@Composable
private fun HeatmapAxisLabel(text: String, fontSize: TextUnit, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        if (text.isNotEmpty()) {
            Text(
                text,
                fontSize = fontSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.wrapContentSize(
                    unbounded = true,
                    align = Alignment.CenterStart
                ),
            )
        }
    }
}

@Composable
private fun HeatmapLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.focusStatsHeatmapLess),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (level in 0..4) {
                HeatmapSwatch(level = level, showBorder = level == 0)
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            stringResource(R.string.focusStatsHeatmapMore),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HeatmapCell(day: HeatmapDay, level: Int, onTap: () -> Unit) {
    HeatmapSwatch(
        level = level,
        showBorder = day.inRange && level == 0,
        modifier = Modifier.clickable(enabled = day.inRange, onClick = onTap),
    )
}

@Composable
private fun HeatmapSwatch(level: Int, showBorder: Boolean, modifier: Modifier = Modifier) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val shape = RoundedCornerShape(2.dp)
    Box(
        modifier = modifier
            .size(11.dp)
            .clip(shape)
            // Data viz, not chrome: this GitHub-style contribution cell's alpha step *is* the
            // encoded value (0..4 intensity level), so the no-alpha-fills rule doesn't apply -
            // an alpha step that encodes data is data, not chrome. Only the zero-level
            // cell's outline is chrome, so that border stays solid rather than alpha-faded below.
            .background(if (level > 0) FocusColor.copy(alpha = 0.2f + level * 0.2f) else Color.Transparent)
            .then(
                if (showBorder) Modifier.border(
                    AppTheme.borderHairline,
                    outlineColor,
                    shape
                ) else Modifier
            )
    )
}

package io.github.benji377.timety.ui.components.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.util.stats.ReportPeriod
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private const val MANY_BARS = 7
private const val VERY_MANY_BARS = 14

/**
 * One bucket of a [PeriodBarChart]: its [axisLabel], one of [values] per series, and the sentence
 * shown above the chart while it is selected.
 */
data class PeriodBar(val axisLabel: String, val readout: String, val values: List<Int>)

/**
 * Bars for a period, with the selected bar's numbers on a fixed line above them. Tapping a bar only
 * selects it. The selection starts on [currentIndex], or else on the last bar, whenever [resetKey]
 * changes. The bar at [currentIndex] is outlined.
 */
@Composable
fun PeriodBarChart(
    bars: List<PeriodBar>,
    seriesColors: List<Color>,
    currentIndex: Int?,
    resetKey: Any,
    modifier: Modifier = Modifier,
) {
    var selected by remember(resetKey, bars.size) {
        mutableIntStateOf(currentIndex ?: bars.lastIndex)
    }
    val maxValue = bars.maxOfOrNull { bar -> bar.values.maxOrNull() ?: 0 }?.coerceAtLeast(1) ?: 1
    val barCount = bars.size * seriesColors.size
    val barWidth = when {
        barCount <= MANY_BARS -> 16.dp
        barCount <= VERY_MANY_BARS -> 11.dp
        else -> 8.dp
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = bars.getOrNull(selected)?.readout.orEmpty(),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(AppTheme.spaceSmall))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        ) {
            bars.forEachIndexed { index, bar ->
                BarColumn(
                    bar = bar,
                    seriesColors = seriesColors,
                    maxValue = maxValue,
                    barWidth = barWidth,
                    isSelected = index == selected,
                    isCurrent = index == currentIndex,
                    onClick = { selected = index },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BarColumn(
    bar: PeriodBar,
    seriesColors: List<Color>,
    maxValue: Int,
    barWidth: Dp,
    isSelected: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
    val outline = MaterialTheme.colorScheme.outline
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(AppTheme.brMedium)
            .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = bar.readout
                role = Role.Button
                onClick { onClick(); true }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(top = AppTheme.spaceSmall),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            bar.values.forEachIndexed { series, value ->
                Box(
                    modifier = Modifier
                        .width(barWidth)
                        .fillMaxHeight(value.toFloat() / maxValue)
                        .clip(shape)
                        .background(seriesColors[series])
                        .then(
                            if (isCurrent && value > 0) {
                                Modifier.border(AppTheme.borderHairline, outline, shape)
                            } else {
                                Modifier
                            }
                        ),
                )
            }
        }
        Text(
            text = bar.axisLabel,
            fontSize = AppTheme.fsCaption,
            fontWeight = if (isCurrent || isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrent || isSelected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(vertical = AppTheme.spaceXSmall),
        )
    }
}

/** The short label under a bar: a weekday, a week's first day number or a month. */
fun bucketAxisLabel(
    period: ReportPeriod,
    bucket: ClosedRange<LocalDate>,
    locale: Locale = Locale.getDefault(),
): String = when (period) {
    ReportPeriod.WEEK -> bucket.start.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
    ReportPeriod.MONTH -> bucket.start.dayOfMonth.toString()
    ReportPeriod.YEAR -> bucket.start.month.getDisplayName(TextStyle.SHORT, locale)
}

/** The index of the bucket holding [today], if any. */
fun currentBucketIndex(buckets: List<ClosedRange<LocalDate>>, today: LocalDate): Int? =
    buckets.indexOfFirst { today in it }.takeIf { it >= 0 }

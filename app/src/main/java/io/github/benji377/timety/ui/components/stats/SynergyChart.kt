package io.github.benji377.timety.ui.components.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.utils.quantityString
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import java.time.LocalDate
import kotlin.math.roundToInt
import java.time.format.TextStyle as JavaTextStyle

/**
 * Focus minutes and completed tasks over [days] as two smoothed lines. Each line has its own scale,
 * so they show how the two move together; the exact numbers are on the line above the chart, for
 * the last day until another is tapped or dragged to.
 */
@Composable
fun SynergyChart(
    days: List<LocalDate>,
    dailyFocus: List<Int>,
    dailyTasks: List<Int>,
    modifier: Modifier = Modifier,
) {
    val locale = LocalLocale.current.platformLocale
    val textMeasurer = rememberTextMeasurer()
    var selected by remember(days.last()) { mutableIntStateOf(days.lastIndex) }
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selectedAxisColor = MaterialTheme.colorScheme.onSurface
    val guideColor = MaterialTheme.colorScheme.outline
    val sidePadding = 16.dp
    val labelHeight = 28.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                days[selected].dayOfWeek.getDisplayName(JavaTextStyle.SHORT, locale),
                fontWeight = FontWeight.Bold,
            )
            ReadoutValue(FocusColor, AppDateFormatUtils.formatMinutesCompact(dailyFocus[selected]))
            ReadoutValue(
                TaskColor,
                quantityString(
                    R.plurals.nTasksCount,
                    dailyTasks[selected],
                    zeroRes = R.string.nTasksCountZero,
                    dailyTasks[selected],
                ),
            )
        }
        Spacer(Modifier.height(AppTheme.spaceSmall))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .pointerInput(days.size) {
                    val padding = sidePadding.toPx()
                    fun select(x: Float) {
                        val fraction = (x - padding) / (size.width - 2 * padding)
                        selected = (fraction * days.lastIndex).roundToInt().coerceIn(days.indices)
                    }
                    detectTapGestures { select(it.x) }
                }
                // Only sideways drags, so a vertical swipe over the chart still scrolls the page.
                .pointerInput(days.size) {
                    val padding = sidePadding.toPx()
                    fun select(x: Float) {
                        val fraction = (x - padding) / (size.width - 2 * padding)
                        selected = (fraction * days.lastIndex).roundToInt().coerceIn(days.indices)
                    }
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        select(change.position.x)
                    }
                },
        ) {
            val padding = sidePadding.toPx()
            val plotWidth = size.width - 2 * padding
            val plotHeight = size.height - labelHeight.toPx()
            if (plotWidth <= 0f || plotHeight <= 0f) return@Canvas
            fun xOf(index: Int) = padding + index.toFloat() / days.lastIndex * plotWidth

            drawLine(
                color = guideColor,
                start = Offset(xOf(selected), 0f),
                end = Offset(xOf(selected), plotHeight),
                strokeWidth = 1.dp.toPx(),
            )
            days.forEachIndexed { index, day ->
                val layout = textMeasurer.measure(
                    day.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, locale),
                    TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (index == selected) selectedAxisColor else axisColor,
                    ),
                )
                drawText(
                    layout,
                    topLeft = Offset(xOf(index) - layout.size.width / 2f, plotHeight + 8.dp.toPx()),
                )
            }
            drawSeries(dailyFocus, FocusColor, selected, plotHeight, ::xOf)
            drawSeries(dailyTasks, TaskColor, selected, plotHeight, ::xOf)
        }
    }
}

private fun DrawScope.drawSeries(
    values: List<Int>,
    color: Color,
    selected: Int,
    plotHeight: Float,
    xOf: (Int) -> Float,
) {
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1).toFloat()
    val top = 8.dp.toPx()
    val points = values.mapIndexed { index, value ->
        Offset(xOf(index), plotHeight - value / max * (plotHeight - top))
    }
    val line = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            val midX = (points[i - 1].x + points[i].x) / 2f
            cubicTo(midX, points[i - 1].y, midX, points[i].y, points[i].x, points[i].y)
        }
    }
    val area = Path().apply {
        addPath(line)
        lineTo(points.last().x, plotHeight)
        lineTo(points.first().x, plotHeight)
        close()
    }
    drawPath(area, color = color.copy(alpha = 0.1f))
    drawPath(line, color = color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    points.forEachIndexed { index, point ->
        drawCircle(color, radius = (if (index == selected) 6 else 4).dp.toPx(), center = point)
    }
}

@Composable
private fun ReadoutValue(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LegendDot(color, size = AppTheme.spaceSmall)
        Spacer(Modifier.width(AppTheme.spaceXSmall))
        Text(text, fontWeight = FontWeight.Bold, fontSize = AppTheme.fsBodySmall)
    }
}

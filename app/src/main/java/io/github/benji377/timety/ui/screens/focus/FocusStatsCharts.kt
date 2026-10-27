package io.github.benji377.timety.ui.screens.focus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.focus.color
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.util.stats.FocusSessionDayStats
import io.github.benji377.timety.util.stats.StatsUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// Draws a 24-hour clock face for [day] with an arc per session, starting at 00:00 at the top and
// sweeping clockwise. Each session is clipped to [day], so one crossing midnight draws its evening
// part on the day it started and its early-morning part on the next.
@Composable
internal fun FocusClockChart(
    sessions: List<FocusSessionEntity>,
    day: LocalDate,
    color: Color,
    modifier: Modifier = Modifier
) {
    val zone = remember { ZoneId.systemDefault() }
    val textMeasurer = rememberTextMeasurer()
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val markerColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = minOf(size.width, size.height) / 2f
        val strokeWidth = 24.dp.toPx()

        drawCircle(
            color = trackColor,
            radius = radius - strokeWidth / 2f,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        val markers = listOf("0" to -90.0, "6" to 0.0, "12" to 90.0, "18" to 180.0)
        markers.forEach { (label, deg) ->
            val rad = Math.toRadians(deg)
            val r = radius - strokeWidth - 10.dp.toPx()
            val x = center.x + (r * kotlin.math.cos(rad)).toFloat()
            val y = center.y + (r * kotlin.math.sin(rad)).toFloat()
            val layout =
                textMeasurer.measure(
                    label,
                    style = TextStyle(color = markerColor, fontSize = 10.sp)
                )
            drawText(
                layout,
                topLeft = Offset(x - layout.size.width / 2f, y - layout.size.height / 2f)
            )
        }

        val dayStart = day.atStartOfDay(zone).toInstant()
        val nextDayStart = day.plusDays(1).atStartOfDay(zone).toInstant()
        // Wall-clock time, so the 23- and 25-hour DST days still line up with the 24-hour face.
        fun hourOfDay(instant: Instant): Double =
            if (!instant.isBefore(nextDayStart)) 24.0
            else instant.atZone(zone).toLocalTime().toSecondOfDay() / 3600.0

        sessions.forEach { session ->
            val end = session.endTime ?: Instant.now()
            val startHour = hourOfDay(maxOf(session.startTime, dayStart))
            val endHour = hourOfDay(minOf(end, nextDayStart))

            val sweepHours = endHour - startHour
            if (sweepHours <= 0) return@forEach

            val startAngleDeg = (startHour / 24.0) * 360.0 - 90.0
            val sweepAngleDeg = (sweepHours / 24.0) * 360.0

            // Data viz, not chrome: a Canvas-drawn arc segment on the 24-hour clock face, not a
            // container fill, so the app's flat-fill rule doesn't apply to it.
            drawArc(
                color = color.copy(alpha = 0.8f),
                startAngle = startAngleDeg.toFloat(),
                sweepAngle = sweepAngleDeg.toFloat(),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size((radius - strokeWidth / 2f) * 2, (radius - strokeWidth / 2f) * 2),
                topLeft = Offset(
                    center.x - (radius - strokeWidth / 2f),
                    center.y - (radius - strokeWidth / 2f)
                ),
            )
        }
    }
}

@Composable
internal fun VolumeBarChart(
    sessions: List<FocusSessionEntity>,
    startOfWeek: LocalDate,
    isCurrentRealWeek: Boolean,
    zone: ZoneId
) {
    // A midnight-crossing session is credited to each day it spanned, so its bar height matches the
    // heatmap and the daily goal instead of stacking the whole session onto its start day.
    val dailyMins = IntArray(7)
    FocusSessionDayStats.minutesByDay(sessions, zone).forEach { (day, minutes) ->
        val offset = java.time.temporal.ChronoUnit.DAYS.between(startOfWeek, day)
        if (offset in 0..6) dailyMins[offset.toInt()] += minutes
    }
    val maxY = StatsUtils.maxValue(dailyMins.toList(), minimum = 60.0)
    val denom = (maxY * 1.2).toFloat().coerceAtLeast(1f)
    val todayIndex = if (isCurrentRealWeek) LocalDate.now().dayOfWeek.value - 1 else -1

    val days = listOf(
        stringResource(R.string.commonWeekdayMon),
        stringResource(R.string.commonWeekdayTue),
        stringResource(R.string.commonWeekdayWed),
        stringResource(R.string.commonWeekdayThu),
        stringResource(R.string.commonWeekdayFri),
        stringResource(R.string.commonWeekdaySat),
        stringResource(R.string.commonWeekdaySun),
    )

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0 until 7) {
            val isToday = i == todayIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val h = (dailyMins[i] / denom).coerceIn(0f, 1f)
                    // Flat, solid bar for every day - fading the non-today bars was a soft alpha
                    // fill standing in for a boolean emphasis, not an encoded data value, so it's
                    // gone. Today still needs a solid affordance beyond the label (bold text alone
                    // is too weak a signal at this bar size), so today's bar gets a solid outline
                    // border instead - a solid differentiator, not restored alpha.
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(h)
                            .width(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(FocusColor)
                            .then(
                                if (isToday) {
                                    Modifier.border(
                                        AppTheme.borderHairline,
                                        MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(4.dp),
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                    )
                }
                Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
                Text(
                    days[i],
                    fontSize = AppTheme.fsBodySmall,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) FocusColor else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

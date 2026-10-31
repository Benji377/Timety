package io.github.benji377.timety.ui.screens.focus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.focus.color
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

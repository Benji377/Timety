package io.github.benji377.timety.ui.components.focus

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.GaugeBgDark
import io.github.benji377.timety.ui.theme.GaugeBgLight
import io.github.benji377.timety.ui.theme.GaugeBorderDark
import io.github.benji377.timety.ui.theme.GaugeBorderLight
import io.github.benji377.timety.ui.theme.GaugeLabelDark
import io.github.benji377.timety.ui.theme.GaugeTrackDark
import io.github.benji377.timety.ui.theme.GaugeTrackLight
import io.github.benji377.timety.ui.theme.GaugeWhite
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.unit.IntSize
import io.github.benji377.timety.ui.theme.LocalIsDarkTheme


@Composable
fun InteractiveGauge(
    progress: Float,
    label: String,
    centerText: String,
    bottomText: String,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    isStopwatch: Boolean = false,
    onChanged: ((Float) -> Unit)? = null,
    centerTextColor: Color? = null,
    labelColor: Color? = null,
    bottomTextColor: Color = Color.Unspecified,
    bottomTextIcon: ImageVector? = null,
    onBottomTextTapped: (() -> Unit)? = null,
    color: Color? = null,
) {
    var currentProgress by remember { mutableFloatStateOf(progress) }
    LaunchedEffect(progress, isInteractive) { currentProgress = progress }

    val isDark = LocalIsDarkTheme.current
    val gaugeColor = color ?: MaterialTheme.colorScheme.primary

    val infiniteTransition = rememberInfiniteTransition(label = "gaugePulse")
    val pulseValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(AppTheme.pulseDurationMs, easing = LinearEasing),
        ),
        label = "gaugePulseValue",
    )

    val paintProgress = if (isStopwatch) pulseValue else currentProgress
    val trackOpacity = if (isStopwatch) (1f - pulseValue).coerceIn(0.2f, 1f) else 1f

    fun handlePointer(offset: Offset, size: IntSize) {
        if (!isInteractive) return
        val center = Offset(size.width / 2f, size.height / 2f)
        val dx = offset.x - center.x
        val dy = offset.y - center.y
        var angle = atan2(dy.toDouble(), dx.toDouble())
        angle += PI / 2
        if (angle < 0) angle += 2 * PI
        var newProgress = (angle / (2 * PI)).toFloat()
        if (newProgress < 0.02f) newProgress = 0f
        if (newProgress > 0.98f) newProgress = 1f
        currentProgress = newProgress
        onChanged?.invoke(newProgress)
    }

    Box(
        modifier = modifier
            .size(AppTheme.gaugeSize)
            .pointerInput(isInteractive) {
                if (!isInteractive) return@pointerInput
                detectDragGestures { change, _ -> handlePointer(change.position, size) }
            }
            .pointerInput(isInteractive) {
                if (!isInteractive) return@pointerInput
                detectTapGestures { offset -> handlePointer(offset, size) }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = minOf(size.width, size.height) / 2f
            val strokeWidth = AppTheme.gaugeStrokeWidth.toPx()
            val innerRadius = radius - strokeWidth - 14.dp.toPx()

            // Soft shadow approximation behind the inner disc.
            drawCircle(
                color = (if (isDark) GaugeBorderDark else GaugeBorderLight).copy(alpha = 0.15f),
                radius = innerRadius + 3.dp.toPx(),
                center = center,
            )

            // Inner disc.
            drawCircle(
                color = if (isDark) GaugeBgDark else GaugeWhite,
                radius = innerRadius,
                center = center
            )
            drawCircle(
                color = if (isDark) GaugeBorderDark else GaugeBorderLight,
                radius = innerRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx()),
            )

            // Outer track.
            drawCircle(
                color = if (isDark) GaugeTrackDark else GaugeBorderLight,
                radius = radius - strokeWidth / 2f,
                center = center,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // Progress arc.
            drawArc(
                color = gaugeColor.copy(alpha = trackOpacity),
                startAngle = -90f,
                sweepAngle = 360f * paintProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size((radius - strokeWidth / 2f) * 2, (radius - strokeWidth / 2f) * 2),
                topLeft = Offset(
                    center.x - (radius - strokeWidth / 2f),
                    center.y - (radius - strokeWidth / 2f)
                ),
            )

            // Draggable thumb.
            if (isInteractive) {
                val thumbAngle = (-PI / 2) + (2 * PI * paintProgress)
                val thumbCenter = Offset(
                    (center.x + (radius - strokeWidth / 2f) * cos(thumbAngle)).toFloat(),
                    (center.y + (radius - strokeWidth / 2f) * sin(thumbAngle)).toFloat(),
                )
                drawCircle(
                    color = gaugeColor.copy(alpha = AppTheme.opacityLight),
                    radius = 20.dp.toPx(),
                    center = thumbCenter
                )
                drawCircle(color = GaugeWhite, radius = 14.dp.toPx(), center = thumbCenter)
                drawCircle(
                    color = gaugeColor,
                    radius = 14.dp.toPx(),
                    center = thumbCenter,
                    style = Stroke(width = 4.dp.toPx())
                )
            }
        }

        // Label, time and tag pill spread evenly over the inner disc's full height,
        // centered both ways (Flutter achieves this with a FittedBox inside the disc).
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
        ) {
            val actualLabelColor = labelColor ?: (if (isDark) GaugeLabelDark else GaugeTrackLight)
            Text(
                text = if (label.length > 12) label.replace(" ", "\n") else label,
                textAlign = TextAlign.Center,
                fontSize = AppTheme.fsGaugeLabel,
                fontWeight = AppTheme.fwBold,
                letterSpacing = AppTheme.lsExtraWide,
                color = actualLabelColor,
                lineHeight = if (label.length > 12) AppTheme.fsGaugeLabel * 1.1f else AppTheme.fsGaugeLabel,
            )
            val bodyLargeColor = MaterialTheme.colorScheme.onSurface
            val actualCenterColor = if (centerTextColor != null) {
                if (isDark) GaugeWhite else centerTextColor
            } else {
                bodyLargeColor
            }
            // Auto-shrinks for long stopwatch values (e.g. "120:00"), like Flutter's
            // FittedBox(scaleDown).
            BasicText(
                text = centerText,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 32.sp,
                    maxFontSize = AppTheme.fsGaugeDisplay,
                    stepSize = 2.sp,
                ),
                style = TextStyle(
                    fontSize = AppTheme.fsGaugeDisplay,
                    fontWeight = AppTheme.fwLight,
                    color = actualCenterColor,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isDark) GaugeBgDark else GaugeBgLight, CircleShape)
                    .border(1.dp, if (isDark) GaugeBorderDark else GaugeBorderLight, CircleShape)
                    .clickable(enabled = onBottomTextTapped != null) { onBottomTextTapped?.invoke() }
                    .padding(horizontal = AppTheme.spaceLarge, vertical = AppTheme.spaceMedium),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (bottomTextIcon != null) {
                        Icon(
                            bottomTextIcon,
                            contentDescription = null,
                            tint = bottomTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = bottomText,
                        fontSize = AppTheme.fsBodyLarge,
                        fontWeight = AppTheme.fwBold,
                        color = bottomTextColor,
                    )
                }
            }
        }
    }
}

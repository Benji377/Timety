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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.ripple
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.GaugeBgDark
import io.github.benji377.timety.ui.theme.GaugeBgLight
import io.github.benji377.timety.ui.theme.GaugeLabelDark
import io.github.benji377.timety.ui.theme.GaugeWhite
import io.github.benji377.timety.ui.theme.LocalIsDarkTheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Progress gauge used for focus sessions, with a label, a large center value, and a bottom pill.
 * The track/progress ring is drawn as a circle inside a hairline frame. When [isInteractive] is
 * true, the progress can be changed by dragging or tapping the ring. When [isStopwatch] is true,
 * the ring pulses instead of showing a fixed progress amount.
 */
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
    // One outline around the whole dial, exactly like every other bordered element in the app.
    // The dial used to carry two competing rings - a thick colored band and a neutral gray donut -
    // which read as a heavy frame rather than as a progress indicator.
    val frameColor = MaterialTheme.colorScheme.outline
    // The un-progressed part of the ring, in the app's paper-alt tone rather than a gray that
    // appears nowhere else in the palette.
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    val infiniteTransition = rememberInfiniteTransition(label = "gaugePulse")
    val pulseValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(AppTheme.PULSE_DURATION_MS, easing = LinearEasing),
        ),
        label = "gaugePulseValue",
    )

    val paintProgress = if (isStopwatch) pulseValue else currentProgress
    val trackOpacity = if (isStopwatch) (1f - pulseValue).coerceIn(0.2f, 1f) else 1f

    fun handlePointer(offset: Offset, size: IntSize) {
        if (!isInteractive) return
        val center = Offset(size.width / 2f, size.height / 2f)
        val newProgress = progressFromCircleAngle(offset, center)
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
            val frameWidth = AppTheme.borderHairline.toPx()

            val faceFillColor = if (isDark) GaugeBgDark else GaugeWhite

            // The dial is one solid card that happens to be round: a flat face inside a single
            // hairline outline, the same two ingredients as every card in the app, so it reads as
            // part of the set rather than as a piece of instrumentation.
            drawCircle(color = faceFillColor, radius = radius, center = center)
            drawCircle(
                color = frameColor,
                radius = radius - frameWidth / 2f,
                center = center,
                style = Stroke(width = frameWidth),
            )

            // The ring sits inside the frame rather than doubling as it, which leaves the outline
            // free to stay the same weight and color as every other border on the screen.
            val ringRadius = radius - frameWidth - AppTheme.spaceSmall.toPx() - strokeWidth / 2f

            drawCircle(
                color = trackColor,
                radius = ringRadius,
                center = center,
                style = Stroke(width = strokeWidth),
            )
            drawArc(
                color = gaugeColor.copy(alpha = trackOpacity),
                startAngle = -90f,
                sweepAngle = 360f * paintProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(ringRadius * 2, ringRadius * 2),
                topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
            )
            val thumbAngle = (-PI / 2) + (2 * PI * paintProgress)
            val thumbCenter = Offset(
                (center.x + ringRadius * cos(thumbAngle)).toFloat(),
                (center.y + ringRadius * sin(thumbAngle)).toFloat(),
            )

            // Draggable thumb: a solid disc with the same bold outline as the dial, so the grab
            // handle is built from the app's own vocabulary instead of a soft glow.
            if (isInteractive) {
                val thumbRadius = strokeWidth * 0.9f
                drawCircle(color = gaugeColor, radius = thumbRadius, center = thumbCenter)
                drawCircle(
                    color = frameColor,
                    radius = thumbRadius - frameWidth / 2f,
                    center = thumbCenter,
                    style = Stroke(width = frameWidth),
                )
            }
        }

        // Label, time and tag pill spread evenly over the inner disc's full height, centered both
        // ways, with a generous inset to keep text off the curve.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
        ) {
            val actualLabelColor =
                labelColor ?: (if (isDark) GaugeLabelDark else MaterialTheme.colorScheme.onSurfaceVariant)
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
            // Auto-shrinks for long stopwatch values (e.g. "120:00") so they still fit on one line.
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
            val bottomTextInteractionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isDark) GaugeBgDark else GaugeBgLight, CircleShape)
                    .border(AppTheme.borderHairline, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable(
                        enabled = onBottomTextTapped != null,
                        interactionSource = bottomTextInteractionSource,
                        indication = ripple(),
                    ) { onBottomTextTapped?.invoke() }
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

/** Snaps near the top-center seam so it's easy to drag to exactly 0 or 1. */
private fun snapProgress(raw: Float): Float = when {
    raw < 0.02f -> 0f
    raw > 0.98f -> 1f
    else -> raw
}

private fun progressFromCircleAngle(offset: Offset, center: Offset): Float {
    val dx = offset.x - center.x
    val dy = offset.y - center.y
    var angle = atan2(dy.toDouble(), dx.toDouble()) + PI / 2
    if (angle < 0) angle += 2 * PI
    return snapProgress((angle / (2 * PI)).toFloat())
}

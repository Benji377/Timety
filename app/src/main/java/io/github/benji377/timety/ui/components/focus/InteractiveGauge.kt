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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
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
import io.github.benji377.timety.ui.theme.LocalIsDarkTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Outer shape the gauge's track/progress ring is drawn as. */
enum class GaugeShape { Circle, Square }


/**
 * Progress gauge used for focus sessions, with a label, a large center value, and a bottom pill.
 * The track/progress ring is drawn as a [gaugeShape]. When [isInteractive] is true, the progress
 * can be changed by dragging or tapping the ring. When [isStopwatch] is true, the ring pulses
 * instead of showing a fixed progress amount.
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
    gaugeShape: GaugeShape = GaugeShape.Circle,
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
            animation = tween(AppTheme.PULSE_DURATION_MS, easing = LinearEasing),
        ),
        label = "gaugePulseValue",
    )

    val paintProgress = if (isStopwatch) pulseValue else currentProgress
    val trackOpacity = if (isStopwatch) (1f - pulseValue).coerceIn(0.2f, 1f) else 1f

    // Track/progress path for the square shape, built once per size/stroke/radius: the same
    // path (and its PathMeasure) drives both drawing (getSegment/getPosition) and gesture mapping
    // (progressFromSquareEdge), so the two can never drift apart.
    val density = LocalDensity.current
    // Half-side of the inner disc, in dp, so the content Column can be sized to exactly match the
    // disc the Canvas draws (the gap = the track stroke plus a small inset between disc and track).
    val squareInnerHalfSide = AppTheme.gaugeSquareHalfSide - AppTheme.gaugeStrokeWidth - 6.dp
    val squareGeometry = remember(density) {
        with(density) {
            val gaugeRadiusPx = AppTheme.gaugeSize.toPx() / 2f
            val cornerRadiusPx = AppTheme.radiusNeo.toPx()
            val center = Offset(gaugeRadiusPx, gaugeRadiusPx)
            // A square this small still reaches the same corners a circle of `gaugeRadiusPx`
            // would, but a circle curves away near the top/bottom - a square's flat edge doesn't,
            // so it needs a real inset to clear the icon buttons anchored to the surrounding
            // layout's corners (they were positioned assuming that circular clearance).
            val halfSide = AppTheme.gaugeSquareHalfSide.toPx()
            val path = buildSquareTrackPath(center, halfSide, cornerRadiusPx)
            val measure = PathMeasure().apply { setPath(path, forceClosed = true) }
            val innerCornerRadiusPx = AppTheme.radiusMedium.toPx()
            val innerHalfSide = squareInnerHalfSide.toPx()
            val innerPath = buildSquareTrackPath(center, innerHalfSide, innerCornerRadiusPx)
            val shadowPath = buildSquareTrackPath(
                center,
                innerHalfSide + 3.dp.toPx(),
                innerCornerRadiusPx,
            )
            SquareGaugeGeometry(
                halfSide = halfSide,
                path = path,
                measure = measure,
                length = measure.length,
                innerPath = innerPath,
                shadowPath = shadowPath,
            )
        }
    }

    fun handlePointer(offset: Offset, size: IntSize) {
        if (!isInteractive) return
        val center = Offset(size.width / 2f, size.height / 2f)
        val newProgress = when (gaugeShape) {
            GaugeShape.Circle -> progressFromCircleAngle(offset, center)
            GaugeShape.Square -> progressFromSquareEdge(offset, center, squareGeometry.halfSide)
        }
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

            val shadowColor = (if (isDark) GaugeBorderDark else GaugeBorderLight).copy(alpha = 0.15f)
            val discFillColor = if (isDark) GaugeBgDark else GaugeWhite
            val discBorderColor = if (isDark) GaugeBorderDark else GaugeBorderLight

            when (gaugeShape) {
                GaugeShape.Circle -> {
                    // Soft shadow approximation behind the inner disc.
                    drawCircle(color = shadowColor, radius = innerRadius + 3.dp.toPx(), center = center)
                    // Inner disc.
                    drawCircle(color = discFillColor, radius = innerRadius, center = center)
                    drawCircle(
                        color = discBorderColor,
                        radius = innerRadius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }

                GaugeShape.Square -> {
                    // Soft shadow approximation behind the inner disc.
                    drawPath(path = squareGeometry.shadowPath, color = shadowColor)
                    // Inner disc, shaped to match the outer square track instead of a mismatched
                    // circle-in-a-square look.
                    drawPath(path = squareGeometry.innerPath, color = discFillColor)
                    drawPath(
                        path = squareGeometry.innerPath,
                        color = discBorderColor,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }

            val trackColor = if (isDark) GaugeTrackDark else GaugeBorderLight

            val thumbCenter = when (gaugeShape) {
                GaugeShape.Circle -> {
                    // Outer track.
                    drawCircle(
                        color = trackColor,
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
                    val thumbAngle = (-PI / 2) + (2 * PI * paintProgress)
                    Offset(
                        (center.x + (radius - strokeWidth / 2f) * cos(thumbAngle)).toFloat(),
                        (center.y + (radius - strokeWidth / 2f) * sin(thumbAngle)).toFloat(),
                    )
                }

                GaugeShape.Square -> {
                    // Outer track.
                    drawPath(
                        path = squareGeometry.path,
                        color = trackColor,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                    // Progress fill: the sub-length of the track path walked so far, starting
                    // at top-center and going clockwise (same seam/direction as the circle's
                    // startAngle = -90f).
                    if (paintProgress > 0f) {
                        val progressPath = Path()
                        squareGeometry.measure.getSegment(
                            startDistance = 0f,
                            stopDistance = squareGeometry.length * paintProgress,
                            destination = progressPath,
                        )
                        drawPath(
                            path = progressPath,
                            color = gaugeColor.copy(alpha = trackOpacity),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        )
                    }
                    squareGeometry.measure.getPosition(squareGeometry.length * paintProgress)
                }
            }

            // Draggable thumb.
            if (isInteractive) {
                drawCircle(
                    color = gaugeColor.copy(alpha = AppTheme.OPACITY_LIGHT),
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

        // Label, time and tag pill spread evenly over the inner disc's full height, centered both
        // ways. The circle spreads over the whole gauge box (its inner disc is nearly box-sized)
        // with a generous inset to keep text off the curve; the square's inner disc is smaller than
        // the box, so the content is sized to that disc and padded inside it, keeping text and the
        // bottom chip clear of the disc's flat edges.
        val contentModifier = when (gaugeShape) {
            GaugeShape.Square -> Modifier
                .size(squareInnerHalfSide * 2)
                .padding(AppTheme.spaceLarge)

            GaugeShape.Circle -> Modifier
                .fillMaxSize()
                .padding(40.dp)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = contentModifier,
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

/** The square track/inner-disc paths plus the track's precomputed [PathMeasure]. */
private data class SquareGaugeGeometry(
    val halfSide: Float,
    val path: Path,
    val measure: PathMeasure,
    val length: Float,
    val innerPath: Path,
    val shadowPath: Path,
)

/**
 * Builds a rounded-square path inscribed around [center], starting at the top-center midpoint and
 * winding clockwise (matching the circle's `startAngle = -90f` seam/direction), so a length
 * fraction along this path is a direct analog of the circle's angle fraction.
 */
private fun buildSquareTrackPath(center: Offset, halfSide: Float, cornerRadius: Float): Path {
    val left = center.x - halfSide
    val top = center.y - halfSide
    val right = center.x + halfSide
    val bottom = center.y + halfSide
    val d = cornerRadius * 2f
    return Path().apply {
        moveTo(center.x, top)
        lineTo(right - cornerRadius, top)
        arcTo(Rect(right - d, top, right, top + d), -90f, 90f, false)
        lineTo(right, bottom - cornerRadius)
        arcTo(Rect(right - d, bottom - d, right, bottom), 0f, 90f, false)
        lineTo(left + cornerRadius, bottom)
        arcTo(Rect(left, bottom - d, left + d, bottom), 90f, 90f, false)
        lineTo(left, top + cornerRadius)
        arcTo(Rect(left, top, left + d, top + d), 180f, 90f, false)
        lineTo(center.x, top)
        close()
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

/**
 * Maps a touch point to a progress fraction along the square's perimeter, starting at top-center
 * and going clockwise. Unlike a circle, angle-from-center isn't proportional to position along the
 * perimeter for a square, so this projects the touch point onto whichever of the 4 straight edges
 * it's closest to (a corner is treated as sharp for this projection - the small rounding used for
 * drawing is negligible next to the full perimeter length and isn't worth bespoke corner math).
 * The four quarter-points (0, 0.25, 0.5, 0.75) land on the top/right/bottom/left edge midpoints,
 * matching the circle's cardinal points exactly.
 */
private fun progressFromSquareEdge(offset: Offset, center: Offset, halfSide: Float): Float {
    val dx = (offset.x - center.x).toDouble()
    val dy = (offset.y - center.y).toDouble()
    if (dx == 0.0 && dy == 0.0) return 0f
    val side = halfSide.toDouble()
    val raw = when {
        dx > 0 && abs(dx) >= abs(dy) -> {
            // Right edge: top-right corner (1/8) to bottom-right corner (3/8).
            val yAtEdge = (dy / dx * side).coerceIn(-side, side)
            0.125 + 0.25 * ((yAtEdge + side) / (2 * side))
        }

        abs(dx) >= abs(dy) -> {
            // Left edge: bottom-left corner (5/8) to top-left corner (7/8).
            val yAtEdge = (-dy / dx * side).coerceIn(-side, side)
            0.625 + 0.25 * ((side - yAtEdge) / (2 * side))
        }

        dy < 0 && dx >= 0 -> {
            // Top edge, right half: seam (0) to top-right corner (1/8).
            val xAtEdge = (-dx / dy * side).coerceIn(0.0, side)
            0.125 * (xAtEdge / side)
        }

        dy < 0 -> {
            // Top edge, left half: top-left corner (7/8) to seam (1.0).
            val xAtEdge = (-dx / dy * side).coerceIn(-side, 0.0)
            0.875 + 0.125 * ((xAtEdge + side) / side)
        }

        else -> {
            // Bottom edge: bottom-right corner (3/8) to bottom-left corner (5/8).
            val xAtEdge = (dx / dy * side).coerceIn(-side, side)
            0.375 + 0.25 * ((side - xAtEdge) / (2 * side))
        }
    }
    return snapProgress(raw.toFloat())
}

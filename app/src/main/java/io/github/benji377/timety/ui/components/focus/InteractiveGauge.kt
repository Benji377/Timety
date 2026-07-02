package io.github.benji377.timety.ui.components.focus

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import io.github.benji377.timety.ui.theme.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

@Composable
fun InteractiveGauge(
    progress: Float,
    isInteractive: Boolean = false,
    isStopwatch: Boolean = false,
    label: String,
    centerText: String,
    centerTextColor: Color = Color.Unspecified,
    labelColor: Color = Color.Unspecified,
    bottomText: String,
    bottomTextColor: Color = Color.Unspecified,
    bottomTextIcon: ImageVector? = null,
    onBottomTextTapped: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    onChanged: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val paintProgress = if (isStopwatch) pulse else progress
    val trackOpacity = if (isStopwatch) pulse else 1f

    var currentProgress by remember(progress) { mutableStateOf(progress) }
    val gaugeColor = color

    val isDark = false // TODO: isSystemInDarkTheme() 
    val trackColor = if (isDark) Color(0xFF8C8C8C) else Color(0xFFE5DED1)
    val bgColor = if (isDark) Color(0xFF212121) else Color(0xFFF7F5F0)

    Box(
        modifier = modifier
            .size(300.dp)
            .pointerInput(isInteractive) {
                if (!isInteractive) return@pointerInput
                detectDragGestures { change, _ ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = change.position.x - center.x
                    val dy = change.position.y - center.y

                    var angle = atan2(dy.toDouble(), dx.toDouble())
                    angle += PI / 2
                    if (angle < 0) angle += 2 * PI

                    var newProgress = (angle / (2 * PI)).toFloat()
                    if (newProgress < 0.02f) newProgress = 0f
                    if (newProgress > 0.98f) newProgress = 1f

                    currentProgress = newProgress
                    onChanged?.invoke(newProgress)
                }
            }
            .pointerInput(isInteractive) {
                if (!isInteractive) return@pointerInput
                detectTapGestures { offset ->
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
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = minOf(size.width, size.height) / 2f
            val strokeWidth = 16.dp.toPx()
            val innerRadius = radius - strokeWidth - 14.dp.toPx()

            // Inner circle
            drawCircle(color = bgColor, radius = innerRadius, center = center)
            drawCircle(
                color = trackColor,
                radius = innerRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Outer track
            drawCircle(
                color = trackColor,
                radius = radius - strokeWidth / 2f,
                center = center,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress Arc
            drawArc(
                color = gaugeColor.copy(alpha = trackOpacity),
                startAngle = -90f,
                sweepAngle = 360f * (if (isStopwatch) pulse else currentProgress),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size((radius - strokeWidth / 2f) * 2, (radius - strokeWidth / 2f) * 2),
                topLeft = Offset(center.x - (radius - strokeWidth / 2f), center.y - (radius - strokeWidth / 2f))
            )

            // Thumb
            if (isInteractive) {
                val thumbAngle = (-PI / 2) + (2 * PI * currentProgress)
                val thumbCenter = Offset(
                    (center.x + (radius - strokeWidth / 2f) * cos(thumbAngle)).toFloat(),
                    (center.y + (radius - strokeWidth / 2f) * sin(thumbAngle)).toFloat()
                )

                // Shadow
                drawCircle(color = gaugeColor.copy(alpha = 0.3f), radius = 20.dp.toPx(), center = thumbCenter)
                // White core
                drawCircle(color = Color.White, radius = 14.dp.toPx(), center = thumbCenter)
                // Border
                drawCircle(color = gaugeColor, radius = 14.dp.toPx(), center = thumbCenter, style = Stroke(width = 4.dp.toPx()))
            }
        }

        // Inner Text Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            val actualLabelColor = if (labelColor == Color.Unspecified) Color(0xFF7C7C7C) else labelColor
            Text(
                text = label.replace(" ", "\n"),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = actualLabelColor,
                lineHeight = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            val actualCenterColor = if (centerTextColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else centerTextColor
            Text(
                text = centerText,
                fontSize = 48.sp,
                fontWeight = FontWeight.Light,
                color = actualCenterColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(bgColor)
                    .border(1.dp, trackColor, CircleShape)
                    .clickable(enabled = onBottomTextTapped != null) { onBottomTextTapped?.invoke() }
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (bottomTextIcon != null) {
                        Icon(bottomTextIcon, contentDescription = null, tint = bottomTextColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = bottomText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = bottomTextColor
                    )
                }
            }
        }
    }
}

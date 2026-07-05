package io.github.benji377.timety.ui.components.user

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.WarningColor
import kotlin.math.PI
import kotlin.math.sin


@Composable
fun StreakStatusBadge(
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "streakStatusBadge")
    val t by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(AppTheme.pulseDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "streakStatusBadgeT",
    )

    val flameRise = if (isActive) sin(t * PI.toFloat() * 2f) else 0f
    val flicker = if (isActive) 1f + (sin(t * PI.toFloat() * 4f) * 0.05f) else 1f
    val density = LocalDensity.current
    val dimColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    Box(modifier = modifier.size(32.dp), contentAlignment = Alignment.Center) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(WarningColor.copy(alpha = 0.18f), CircleShape),
            )
        }

        Icon(
            imageVector = Icons.Filled.LocalFireDepartment,
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .graphicsLayer {
                    translationY = with(density) { (-0.5f * flameRise).dp.toPx() }
                }
                .scale(flicker),
            tint = if (isActive) WarningColor.copy(alpha = 0.6f) else dimColor.copy(alpha = 0.5f),
        )

        if (isActive) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        translationY = with(density) { (-1.0f * flameRise).dp.toPx() }
                    },
                tint = Color(0xFFFFE08A),
            )
        }
    }
}

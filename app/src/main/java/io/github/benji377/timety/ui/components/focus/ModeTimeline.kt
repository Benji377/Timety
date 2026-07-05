package io.github.benji377.timety.ui.components.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.FocusModeEntity
import io.github.benji377.timety.data.model.focus.PhaseType
import io.github.benji377.timety.data.model.focus.SessionPhaseEntity
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.BorderDark
import io.github.benji377.timety.ui.theme.BorderLight
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.PaperAltLight
import io.github.benji377.timety.ui.theme.PaperLight
import io.github.benji377.timety.ui.theme.WarningColor


@Composable
fun ModeTimeline(
    phases: List<SessionPhaseEntity>,
    currentPhaseIndex: Int = 0,
    isRunning: Boolean = false,
    awaitingContinue: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (phases.isEmpty()) return

    val isDark = io.github.benji377.timety.ui.theme.LocalIsDarkTheme.current
    val isCompleted = currentPhaseIndex >= phases.size
    val completionFill = if (isDark) PaperLight else PaperAltLight
    val isRunningOrAwaiting = isRunning || awaitingContinue
    val outline = MaterialTheme.colorScheme.outline
    val shadowColor = Color(0xFF111111).copy(alpha = 0.12f)
    val lineColor = if (isDark) BorderDark else BorderLight

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = AppTheme.spaceXLarge, vertical = AppTheme.spaceMedium),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        // Start node
        CompletionNode(
            active = isRunningOrAwaiting && currentPhaseIndex == 0 && !isCompleted,
            fillColor = completionFill,
            borderColor = outline,
            shadowColor = shadowColor,
        )

        for (i in phases.indices) {
            ConnectorLine(
                isPast = currentPhaseIndex > i,
                isRunningOrAwaiting = isRunningOrAwaiting,
                lineColor = lineColor,
            )

            val isActive = isRunningOrAwaiting && currentPhaseIndex == i && !isCompleted
            var dotColor = if (phases[i].type == PhaseType.FOCUS) FocusColor else WarningColor
            if (isRunningOrAwaiting && currentPhaseIndex < i) {
                dotColor = dotColor.copy(alpha = AppTheme.opacityLight)
            }
            PhaseDot(color = dotColor, active = isActive)
        }

        // End node
        ConnectorLine(
            isPast = isCompleted,
            isRunningOrAwaiting = isRunningOrAwaiting,
            lineColor = lineColor
        )
        CompletionNode(
            active = isRunningOrAwaiting && isCompleted,
            fillColor = completionFill,
            borderColor = outline,
            shadowColor = shadowColor,
        )
    }
}

@Composable
private fun PhaseDot(color: Color, active: Boolean) {
    val size = if (active) 18.dp else 12.dp
    var mod = Modifier
        .size(size)
        .background(color, CircleShape)
    if (active) {
        mod = mod
            .shadow(elevation = 8.dp, shape = CircleShape, ambientColor = color, spotColor = color)
            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
    }
    Box(modifier = mod)
}

@Composable
private fun CompletionNode(
    active: Boolean,
    fillColor: Color,
    borderColor: Color,
    shadowColor: Color
) {
    val size = if (active) 22.dp else 16.dp
    Box(
        modifier = Modifier
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .size(size)
            .background(fillColor, CircleShape)
            .border(if (active) 3.dp else 2.dp, borderColor, CircleShape)
    )
}

@Composable
private fun ConnectorLine(isPast: Boolean, isRunningOrAwaiting: Boolean, lineColor: Color) {
    val alpha = if (isRunningOrAwaiting && isPast) 0.6f else 0.2f
    Box(
        modifier = Modifier
            .width(24.dp)
            .height(3.dp)
            .background(lineColor.copy(alpha = alpha), RectangleShape)
    )
}


@Composable
fun localizedFocusModeName(mode: FocusModeEntity): String {
    if (!mode.isSystem) return mode.name
    return when (mode.id) {
        FocusModeEntity.SYSTEM_STOPWATCH_ID -> stringResource(R.string.focusModeStopwatch)
        FocusModeEntity.SYSTEM_FLEXIBLE_ID -> stringResource(R.string.focusModeFlexible)
        FocusModeEntity.SYSTEM_POMODORO_ID -> stringResource(R.string.focusModePomodoro)
        else -> mode.name
    }
}

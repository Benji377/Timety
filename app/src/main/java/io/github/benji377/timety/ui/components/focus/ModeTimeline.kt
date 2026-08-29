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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import io.github.benji377.timety.ui.theme.LocalIsDarkTheme
import io.github.benji377.timety.ui.theme.PaperAltLight
import io.github.benji377.timety.ui.theme.PaperLight
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.ui.theme.hairlineBorder


/** Horizontal row of connected dots showing a focus mode's phases and the currently active one. */
@Composable
fun ModeTimeline(
    phases: List<SessionPhaseEntity>,
    modifier: Modifier = Modifier,
    currentPhaseIndex: Int = 0,
    isRunning: Boolean = false,
    awaitingContinue: Boolean = false,
) {
    if (phases.isEmpty()) return

    val isDark = LocalIsDarkTheme.current
    val isCompleted = currentPhaseIndex >= phases.size
    val completionFill = if (isDark) PaperLight else PaperAltLight
    val isRunningOrAwaiting = isRunning || awaitingContinue
    val outline = MaterialTheme.colorScheme.outline
    val lineColor = if (isDark) BorderDark else BorderLight

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = AppTheme.spaceXLarge, vertical = AppTheme.spaceMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Start node
        CompletionNode(
            active = isRunningOrAwaiting && currentPhaseIndex == 0 && !isCompleted,
            fillColor = completionFill,
            borderColor = outline,
        )

        for (i in phases.indices) {
            ConnectorLine(
                isPast = currentPhaseIndex > i,
                isRunningOrAwaiting = isRunningOrAwaiting,
                lineColor = lineColor,
                idleColor = completionFill,
            )

            val isActive = isRunningOrAwaiting && currentPhaseIndex == i && !isCompleted
            var dotColor = if (phases[i].type == PhaseType.FOCUS) FocusColor else WarningColor
            if (isRunningOrAwaiting && currentPhaseIndex < i) {
                // A dot is chrome (a small marker/badge), not chart data, so an upcoming phase gets
                // a solid color swap - the same flat neutral CompletionNode already uses for its own
                // idle state - instead of an alpha-faded tint of the phase color.
                dotColor = completionFill
            }
            PhaseDot(color = dotColor, active = isActive)
        }

        // End node
        ConnectorLine(
            isPast = isCompleted,
            isRunningOrAwaiting = isRunningOrAwaiting,
            lineColor = lineColor,
            idleColor = completionFill,
        )
        CompletionNode(
            active = isRunningOrAwaiting && isCompleted,
            fillColor = completionFill,
            borderColor = outline,
        )
    }
}

@Composable
private fun PhaseDot(color: Color, active: Boolean) {
    val size = if (active) 18.dp else 12.dp
    // Inactive dots stay a minimal borderless fill; the active dot is told apart by its larger
    // size plus an outline ring, in the app's standard outline color rather than
    // colorScheme.primary (TaskColor blue) - a hardcoded primary border would break the screen's
    // green focus-accent consistency.
    Box(
        modifier = Modifier
            .size(size)
            .background(color, CircleShape)
            .then(
                if (active) {
                    Modifier.hairlineBorder()
                } else {
                    Modifier
                }
            )
    )
}

@Composable
private fun CompletionNode(
    active: Boolean,
    fillColor: Color,
    borderColor: Color,
) {
    val size = if (active) 22.dp else 16.dp
    Box(
        modifier = Modifier
            .size(size)
            .background(fillColor, CircleShape)
            .border(AppTheme.borderHairline, borderColor, CircleShape)
    )
}

@Composable
private fun ConnectorLine(
    isPast: Boolean,
    isRunningOrAwaiting: Boolean,
    lineColor: Color,
    idleColor: Color,
) {
    // Solid color swap instead of an alpha-faded line: a "crossed" segment gets the full ink
    // color, an untouched one gets the same flat neutral fill used by CompletionNode/PhaseDot's
    // own idle state, so the timeline reads as one consistent solid-fill language.
    val color = if (isRunningOrAwaiting && isPast) lineColor else idleColor
    Box(
        modifier = Modifier
            .width(24.dp)
            .height(3.dp)
            .background(color, RectangleShape)
    )
}


/** Returns the localized display name for a system focus mode, or [mode]'s own name otherwise. */
@Composable
@ReadOnlyComposable
fun localizedFocusModeName(mode: FocusModeEntity): String {
    if (!mode.isSystem) return mode.name
    return when (mode.id) {
        FocusModeEntity.SYSTEM_STOPWATCH_ID -> stringResource(R.string.focusModeStopwatch)
        FocusModeEntity.SYSTEM_FLEXIBLE_ID -> stringResource(R.string.focusModeFlexible)
        FocusModeEntity.SYSTEM_POMODORO_ID -> stringResource(R.string.focusModePomodoro)
        else -> mode.name
    }
}

package io.github.benji377.timety.ui.components.common

import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlin.math.roundToInt

/**
 * Slider for a whole number in [range] whose value the caller owns. Drags round to whole
 * values. [showSteps] draws a tick per value, which suits short ranges; long ranges stay
 * continuous so the track is not crowded with ticks.
 */
@Composable
fun NeoSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showSteps: Boolean = false,
    colors: SliderColors = SliderDefaults.colors(),
) {
    // Steps count the stops between the ends: one per whole value.
    val steps = if (showSteps) (range.last - range.first - 1).coerceAtLeast(0) else 0
    val trackRange = range.first.toFloat()..range.last.toFloat()
    val state = remember(steps, trackRange) { SliderState(value.toFloat(), steps, trackRange) }
    // With onValueChange set the slider never moves itself, so follow the caller's value.
    SideEffect { state.value = value.toFloat() }
    Slider(
        state = state,
        onValueChange = { onValueChange(it.roundToInt()) },
        modifier = modifier,
        enabled = enabled,
        colors = colors,
    )
}

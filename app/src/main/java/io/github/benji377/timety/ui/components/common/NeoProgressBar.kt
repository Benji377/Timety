package io.github.benji377.timety.ui.components.common

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp


/**
 * Determinate progress bar in the app's neobrutalist style: sharp corners, flat butt caps, and
 * no indicator/track gap or stop dot. Size (including height) is the caller's via [modifier].
 */
@Composable
fun NeoProgressBar(
    progress: () -> Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
) {
    LinearProgressIndicator(
        progress = progress,
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        strokeCap = StrokeCap.Butt,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

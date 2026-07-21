package io.github.benji377.timety.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


/** Design tokens (font sizes, weights, spacing, radii, and misc constants) shared across the UI. */
object AppTheme {
    // Typography.
    val fsHeadingLarge = 24.sp
    val fsHeadingMedium = 20.sp
    val fsHeadingSmall = 18.sp
    val fsBodyLarge = 16.sp
    val fsBodyMedium = 14.sp
    val fsBodySmall = 12.sp
    val fsCaption = 10.sp
    val fsGaugeDisplay = 60.sp
    val fsGaugeLabel = 20.sp
    val fsLabel = 12.sp

    // Font weights
    val fwLight = FontWeight.W300
    val fwNormal = FontWeight.Normal
    val fwMedium = FontWeight.W500
    val fwBold = FontWeight.Bold
    val fwExtraBold = FontWeight.W900

    val lsWide = 1.2.sp
    val lsExtraWide = 1.5.sp

    val spaceXSmall = 4.dp
    val spaceSmall = 8.dp
    val spaceMedium = 12.dp
    val spaceLarge = 16.dp
    val spaceXLarge = 24.dp
    val space2XLarge = 32.dp
    val space3XLarge = 40.dp

    // Common padding configurations
    val paddingScreenHorizontal =
        PaddingValues(horizontal = spaceLarge)

    // Outer margin shared by all list tiles (tasks, recurring tasks, habits) so lists have
    // identical card spacing on every screen.
    val listTileScreenMargin =
        PaddingValues(horizontal = spaceLarge, vertical = spaceXSmall)

    val radiusMedium = 8.dp
    val radiusNeo = 14.dp

    val brMedium = RoundedCornerShape(radiusMedium)
    val brNeo = RoundedCornerShape(radiusNeo)

    // Dimensions.
    val gaugeSize = 300.dp
    val gaugeStrokeWidth = 16.dp

    // A circle inscribed in `gaugeSize` curves away from the corners of its own bounding box, so
    // the surrounding layout's corner buttons sit in the gap that leaves; a square's flat edge
    // doesn't curve away, so the square variant needs a smaller half-side than the circle's
    // radius (142dp) to keep clear of those same corner buttons, which sit only ~8dp inside the
    // gauge box's own left/right edges (measured via uiautomator: 16-64dp from the screen edge,
    // gauge box starts ~56dp from the screen edge) - so 120dp leaves a comfortable ~20dp margin.
    val gaugeSquareHalfSide = 120.dp
    val iconSizeSmall = 18.dp
    val listSectionIconSize = 12.dp
    val listTileBorderWidth = 2.dp
    val neoBorderWidth = 3.dp
    val listTileTrailingSpacing = 8.dp
    val listTileSwipeIconSize = 24.dp

    const val PULSE_DURATION_MS = 2000

    // Settings defaults.
    const val MAX_NODE_MINS = 240

    // Opacity values.
    const val OPACITY_MEDIUM = 0.5f
    const val OPACITY_LIGHT = 0.3f
    const val OPACITY_VERY_LIGHT = 0.1f

}

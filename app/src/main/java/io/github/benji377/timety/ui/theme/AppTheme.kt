package io.github.benji377.timety.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.runtime.Composable
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

    // Label inside an AccordionSelector's expanded segment - smaller than fsLabel so it fits
    // next to the segment's icon within the control's fixed height.
    val fsSegmentLabel = 11.sp

    // Font weights
    val fwLight = FontWeight.W300
    val fwNormal = FontWeight.Normal
    val fwMedium = FontWeight.W500
    val fwBold = FontWeight.Bold
    val fwExtraBold = FontWeight.W900

    val lsNarrow = 0.5.sp
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

    // Pill-shaped controls (reference sheet §5 permits full-round only for toggle controls like
    // the accordion selector, badges, and chips - not for cards/inputs).
    val radiusPill = 24.dp

    val brMedium = RoundedCornerShape(radiusMedium)
    val brNeo = RoundedCornerShape(radiusNeo)
    val brPill = RoundedCornerShape(radiusPill)

    // Dimensions.
    val gaugeSize = 300.dp
    val gaugeStrokeWidth = 16.dp

    // Fixed height of a segmented/accordion selector row (e.g. Priority, Effort).
    val segmentedControlHeight = 48.dp

    // Border / stroke widths (dp), a 1..4 scale. listTileBorderWidth (2) and neoBorderWidth (3)
    // keep their semantic names for list tiles and neo cards; borderThin/borderThick fill the ends.
    val borderThin = 1.dp
    val listTileBorderWidth = 2.dp
    val neoBorderWidth = 3.dp
    val borderThick = 4.dp

    // Hard-shadow offsets (dp) for Modifier.neoShadow / Modifier.neoPressShadow. neoShadowOffset
    // is the default used by cards, buttons, and FABs; neoShadowOffsetSmall is for small chips and
    // dense list rows, where a full 4dp offset would visually crowd neighboring elements. There is
    // no separate "press offset" token: the press interaction travels the full resting offset (see
    // neoPressShadow), so the shadow shrinks to zero exactly as far as the element moves.
    val neoShadowOffset = 4.dp
    val neoShadowOffsetSmall = 2.dp

    // Neobrutalist icon-button container footprint (dp): NeoIconButton's square touch target.
    val neoIconButtonSize = 40.dp

    // Footprint (dp) shared by the focus screen's secondary transport controls (reset, pause) and
    // the Spacer that reserves their place while hidden pre-session - named so the button and its
    // placeholder can never drift out of sync.
    val focusTransportButtonSize = 64.dp

    // Icon sizes (dp). iconSizeSmall (18) is kept for its existing call sites; iconSizeMedium/
    // iconSizeXLarge cover the other common sizes. NOTE: raw 16.dp icons are still widespread and
    // want normalizing to one of these in a later pass.
    val iconSizeXSmall = 12.dp
    val iconSizeSmall = 18.dp
    val iconSizeMedium = 20.dp
    val iconSizeLarge = 24.dp
    val iconSizeXLarge = 32.dp
    val listSectionIconSize = iconSizeXSmall
    val listTileSwipeIconSize = iconSizeLarge

    val listTileTrailingSpacing = 8.dp

    // Neo cards keep Material *elevation* at 0 (no blurred/tonal shadow) - their depth instead
    // comes from Modifier.neoShadow's flat, hard-offset shadow, drawn independently of this. Named
    // once so every card expresses the elevation rule instead of repeating
    // `CardDefaults.cardElevation(defaultElevation = 0.dp)`.
    val neoCardElevation: CardElevation
        @Composable get() = CardDefaults.cardElevation(defaultElevation = 0.dp)

    const val PULSE_DURATION_MS = 2000

    // Settings defaults.
    const val MAX_NODE_MINS = 240

    // Opacity values.
    const val OPACITY_MEDIUM = 0.5f
    const val OPACITY_LIGHT = 0.3f
    const val OPACITY_VERY_LIGHT = 0.1f

}

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

    // Pill-shaped controls. Full-round is for toggle controls only - the accordion selector,
    // badges, and chips - not for cards or inputs.
    val radiusPill = 24.dp

    val brMedium = RoundedCornerShape(radiusMedium)
    val brNeo = RoundedCornerShape(radiusNeo)
    val brPill = RoundedCornerShape(radiusPill)

    // Dimensions.
    val gaugeSize = 300.dp
    val gaugeStrokeWidth = 16.dp

    // Fixed height of a segmented/accordion selector row (e.g. Priority, Effort).
    val segmentedControlHeight = 48.dp

    // Inset between a segmented selector's outer track and its selected segment's own fill, so the
    // two rounded shapes read as nested rather than as one shape with a stray edge.
    val segmentedSelectorInset = 2.dp

    // Border / stroke width (dp). The flat design uses one hairline weight for every border,
    // divider, and frame in the app - there is no weight scale, because depth and hierarchy come
    // from fill color and spacing rather than from stroke thickness. Paired with the muted
    // `outline` color in Theme.kt, a 1dp stroke reads as a separator without drawing attention.
    val borderHairline = 1.dp

    // Icon-button container footprint (dp): NeoIconButton's square touch target.
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

    // Cards carry no Material elevation: the flat design has no blurred or tonal shadows at all,
    // so a card is separated from the page by its fill and hairline border alone. Named once so
    // every card expresses the rule instead of repeating
    // `CardDefaults.cardElevation(defaultElevation = 0.dp)`.
    val flatCardElevation: CardElevation
        @Composable get() = CardDefaults.cardElevation(defaultElevation = 0.dp)

    const val PULSE_DURATION_MS = 2000

    // Settings defaults.
    const val MAX_NODE_MINS = 240

    // Opacity values.
    const val OPACITY_MEDIUM = 0.5f
    const val OPACITY_LIGHT = 0.3f
    const val OPACITY_VERY_LIGHT = 0.1f

}

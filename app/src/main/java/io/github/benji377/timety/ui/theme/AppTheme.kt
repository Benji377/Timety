package io.github.benji377.timety.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
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

    // Smaller than fsLabel so it fits beside the segment's icon.
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

    val paddingScreenHorizontal = PaddingValues(horizontal = spaceLarge)

    // Outer margin shared by every list tile so lists space identically across screens.
    val listTileScreenMargin = PaddingValues(horizontal = spaceLarge, vertical = spaceXSmall)

    val radiusMedium = 8.dp
    val radiusNeo = 14.dp

    // Full-round: toggle controls, badges, and chips only - never cards or inputs.
    val radiusPill = 24.dp

    val brMedium = RoundedCornerShape(radiusMedium)
    val brNeo = RoundedCornerShape(radiusNeo)
    val brPill = RoundedCornerShape(radiusPill)

    // Dimensions.
    val gaugeSize = 300.dp
    val gaugeStrokeWidth = 16.dp

    // Fixed height of a segmented/accordion selector row (e.g. Priority, Effort).
    val segmentedControlHeight = 48.dp

    // Inset so a selected segment reads as nested inside its track.
    val segmentedSelectorInset = 2.dp

    // The design is flat, so a border is all that gives a container edges. Two weights:
    // borderHairline for small repeated elements (dividers, chips, icon buttons, badge circles),
    // borderCard for containers that hold other things (cards, list tiles, dialogs).
    val borderHairline = 1.dp
    val borderCard = 2.dp

    // NeoIconButton's square touch target.
    val neoIconButtonSize = 40.dp

    // Shared by the focus screen's secondary transport controls and the Spacer that reserves
    // their place while hidden, so the two can never drift apart.
    val focusTransportButtonSize = 64.dp

    val iconSizeXSmall = 12.dp
    val iconSizeSmall = 18.dp
    val iconSizeMedium = 20.dp
    val iconSizeLarge = 24.dp
    val iconSizeXLarge = 32.dp
    val listSectionIconSize = iconSizeXSmall
    val listTileSwipeIconSize = iconSizeLarge

    val listTileTrailingSpacing = 8.dp

    // Nothing carries Material elevation; fill and border alone separate a surface from the page.
    val flatCardElevation: CardElevation
        @Composable get() = CardDefaults.cardElevation(defaultElevation = 0.dp)

    val flatButtonElevation: ButtonElevation
        @Composable get() = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp)

    val flatFabElevation: FloatingActionButtonElevation
        @Composable get() = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)

    /** Hairline stroke in the theme outline color, shared by every small bordered control. */
    val hairlineStroke: BorderStroke
        @Composable get() = BorderStroke(borderHairline, MaterialTheme.colorScheme.outline)

    const val PULSE_DURATION_MS = 2000

    // Settings defaults.
    const val MAX_NODE_MINS = 240

    const val OPACITY_MEDIUM = 0.5f
}

/** Draws [AppTheme.hairlineStroke] around this element, circular by default. */
@Composable
fun Modifier.hairlineBorder(shape: Shape = CircleShape): Modifier =
    border(AppTheme.borderHairline, MaterialTheme.colorScheme.outline, shape)

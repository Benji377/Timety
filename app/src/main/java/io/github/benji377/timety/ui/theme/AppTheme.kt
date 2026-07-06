package io.github.benji377.timety.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues


object AppTheme {
    // ===== TYPOGRAPHY =====
    val fsHeadingLarge = 24.sp
    val fsHeadingMedium = 20.sp
    val fsHeadingSmall = 18.sp
    val fsBodyLarge = 16.sp
    val fsBodyMedium = 14.sp
    val fsBodySmall = 12.sp
    val fsCaption = 10.sp
    val fsGaugeDisplay = 60.sp
    val fsGaugeLabel = 20.sp
    val fsLargeNumber = 28.sp
    val fsTimeDisplay = 15.sp
    val fsPhaseTime = 18.sp
    val fsLabel = 12.sp

    // Font weights
    val fwLight = FontWeight.W300
    val fwNormal = FontWeight.Normal
    val fwMedium = FontWeight.W500
    val fwBold = FontWeight.Bold
    val fwExtraBold = FontWeight.W900

    // Letter spacing (Flutter logical px ≈ sp here)
    val lsNormal = 0.sp
    val lsWide = 1.2.sp
    val lsExtraWide = 1.5.sp
    val lsTight = (-0.5).sp

    // ===== SPACING =====
    val spaceTiny = 2.dp
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
    val paddingScreenVertical = PaddingValues(all = spaceLarge)
    val paddingCard = PaddingValues(all = spaceLarge)
    val paddingSection = PaddingValues(
        start = spaceXLarge,
        top = spaceXLarge,
        end = spaceXLarge,
        bottom = spaceSmall
    )

    // ===== BORDER RADIUS =====
    val radiusSmall = 4.dp
    val radiusMedium = 8.dp
    val radiusLarge = 12.dp
    val radiusXLarge = 16.dp
    val radiusCircle = 20.dp
    val radiusCard = 25.dp
    val radiusNeo = 14.dp

    val brSmall = RoundedCornerShape(radiusSmall)
    val brMedium = RoundedCornerShape(radiusMedium)
    val brLarge = RoundedCornerShape(radiusLarge)
    val brXLarge = RoundedCornerShape(radiusXLarge)
    val brCircle = RoundedCornerShape(radiusCircle)
    val brCard = RoundedCornerShape(radiusCard)
    val brNeo = RoundedCornerShape(radiusNeo)

    // ===== DIMENSIONS =====
    val gaugeSize = 300.dp
    val gaugeStrokeWidth = 16.dp
    val iconSizeSmall = 18.dp
    val iconSizeMedium = 24.dp
    val iconSizeLarge = 32.dp
    val profileImageSize = 100.dp
    val listSectionIconSize = 12.dp
    val listSectionTitleSize = fsBodyLarge
    val listTileBorderWidth = 2.dp
    val neoBorderWidth = 3.dp
    val listTileTrailingSpacing = 8.dp
    val listTileSwipeIconSize = 24.dp
    val listTileScreenMargin = PaddingValues(
        horizontal = spaceLarge,
        vertical = spaceXSmall
    )

    // ===== DURATIONS (milliseconds) =====
    const val animationFastMs = 150
    const val animationNormalMs = 300
    const val animationSlowMs = 500
    const val pulseDurationMs = 2000
    const val snackBarDurationMs = 2000

    // ===== SETTINGS DEFAULTS =====
    const val maxNodeMins = 240
    const val maxStopwatchMins = 120
    const val dailyGoalMinsDefault = 90

    // ===== OPACITY VALUES =====
    const val opacityMedium = 0.5f
    const val opacityLight = 0.3f
    const val opacityVeryLight = 0.1f
    const val opacityXLight = 0.2f

    // ===== PHASE / STATUS COLOR HELPERS =====
    fun getPhaseColor(phaseType: String): Color = when (phaseType.lowercase()) {
        "focus" -> PhaseFocusColor
        "break_short" -> PhaseBreakShortColor
        "break_long" -> PhaseBreakLongColor
        "rest" -> PhaseRestColor
        "snack" -> PhaseSnackColor
        "distracted" -> PhaseDistractedColor
        else -> InfoColor
    }

    fun getTaskStatusColor(status: String): Color = when (status.lowercase()) {
        "completed" -> StatusCompleted
        "overdue" -> StatusOverdue
        "due_today" -> StatusDueToday
        else -> StatusDefault
    }
}

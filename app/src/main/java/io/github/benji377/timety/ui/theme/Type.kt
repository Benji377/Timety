package io.github.benji377.timety.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Flutter's AppTheme does not override the global textTheme (only per-component
// title styles), so the Material 3 baseline typography is the faithful default.
// Individual composables set their own font size/weight inline, matching Flutter.
val TimetyTypography = Typography(
    titleLarge = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        letterSpacing = (-0.5).sp
    )
)

// Neobrutalist default shapes (radiusNeo = 14 in AppTheme).
val TimetyShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

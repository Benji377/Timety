package io.github.benji377.timety.ui.theme

import androidx.compose.ui.graphics.Color

// ===== CORE PALETTE (mirrors AppTheme in app_theme.dart) =====
val TaskColor = Color(0xFF2563EB)
val FocusColor = Color(0xFF16A34A)
val HabitColor = Color(0xFF7C3AED)
val UserColor = Color(0xFFDC2626)
val WarningAccent = Color(0xFFF59E0B)

val InkLight = Color(0xFF111111)
val InkDark = Color(0xFFF5F5F5)
val PaperLight = Color(0xFFFFF8EF)
val PaperAltLight = Color(0xFFF6EEDB)
val PaperDark = Color(0xFF151515)
val PaperAltDark = Color(0xFF202020)
val BorderLight = Color(0xFF111111)
val BorderDark = Color(0xFFF2E8D5)
val ShadowColor = Color(0xFF111111)

// ===== SEMANTIC COLORS =====
val SuccessColor = FocusColor
val ErrorColor = UserColor
val WarningColor = WarningAccent
val InfoColor = TaskColor

// Type colors
val TypeTaskColor = TaskColor
val TypeHabitColor = HabitColor
val TypeFocusColor = FocusColor

// Phase type colors
val PhaseFocusColor = FocusColor
val PhaseBreakShortColor = WarningAccent
val PhaseBreakLongColor = WarningAccent
val PhaseRestColor = Color(0xFF6B7280)
val PhaseSnackColor = HabitColor
val PhaseDistractedColor = UserColor

// Status colors
val StatusCompleted = FocusColor
val StatusOverdue = UserColor
val StatusDueToday = WarningAccent
val StatusDefault = TaskColor

// Component colors
val LocationPinColor = UserColor
val WifiOffColor = Color(0xFF6B7280)
val GaugeTrackLight = Color(0xFF7C7C7C)
val GaugeBgLight = PaperLight
val GaugeWhite = Color.White
val GaugeBorderLight = Color(0xFFE5DED1)
val GaugeBgDark = PaperDark
val GaugeBorderDark = Color(0xFF4A4A4A)
val GaugeTrackDark = Color(0xFF8C8C8C)
val GaugeLabelDark = Color(0xFFD4D4D4)

// Surface helper colors used by the neobrutalist scheme
val SurfaceAltDark = Color(0xFF2A2A2A)
val UnselectedNavLight = Color(0xFF666666)
val UnselectedNavDark = Color(0xFF9A9A9A)
val SwitchTrackOffLight = Color(0xFFBDBDBD)
val SwitchTrackOffDark = Color(0xFF3A3A3A)

// Neobrutalism border tokens
val NeoBorderColorLight = BorderLight
val NeoBorderColorDark = BorderDark

// Material accent choices offered by color pickers (habit color, focus tag color).
// Pickers prepend their section's own accent as the first/default choice.
val PickerPalette = listOf(
    Color(0xFFF44336), // red
    Color(0xFFE91E63), // pink
    Color(0xFFFFC107), // amber
    Color(0xFFFF9800), // orange
    Color(0xFF4CAF50), // green
    Color(0xFF8BC34A), // light green
    Color(0xFF009688), // teal
    Color(0xFF2196F3), // blue
    Color(0xFF00BCD4), // cyan
    Color(0xFF3F51B5), // indigo
    Color(0xFF9C27B0), // purple
    Color(0xFF673AB7), // deep purple
    Color(0xFF795548), // brown
    Color(0xFF607D8B), // blue grey
)

// Streak flame inner-glow highlight.
val FlameGlowColor = Color(0xFFFFE08A)

// Extra hues for categorical charts once the core section palette runs out.
val ChartTeal = Color(0xFF009688)
val ChartDeepOrange = Color(0xFFFF5722)

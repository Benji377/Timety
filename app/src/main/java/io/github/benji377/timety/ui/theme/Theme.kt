package io.github.benji377.timety.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.material3.SnackbarHostState

// Neobrutalist color schemes ported 1:1 from AppTheme.buildTheme() in app_theme.dart.
// No Material You / dynamic color — the palette is fixed.

private val DarkColorScheme = darkColorScheme(
    primary = TaskColor,
    onPrimary = Color.White,
    secondary = FocusColor,
    onSecondary = Color.White,
    tertiary = HabitColor,
    onTertiary = Color.White,
    error = UserColor,
    onError = Color.White,
    background = PaperDark,
    onBackground = InkDark,
    surface = PaperAltDark,          // _surfaceColor(dark) = paperAltDark
    onSurface = InkDark,             // _foregroundColor(dark)
    surfaceVariant = SurfaceAltDark, // _surfaceAltColor(dark) = 0xFF2A2A2A
    onSurfaceVariant = Color(0xFFCACACA),
    surfaceContainerHighest = SurfaceAltDark,
    surfaceContainerHigh = SurfaceAltDark,
    surfaceContainer = PaperAltDark,
    outline = BorderDark,
    outlineVariant = Color(0xFF4A4A4A),
)

private val LightColorScheme = lightColorScheme(
    primary = TaskColor,
    onPrimary = Color.White,
    secondary = FocusColor,
    onSecondary = Color.White,
    tertiary = HabitColor,
    onTertiary = Color.White,
    error = UserColor,
    onError = Color.White,
    background = PaperLight,
    onBackground = InkLight,
    surface = Color.White,           // _surfaceColor(light) = white
    onSurface = InkLight,            // _foregroundColor(light)
    surfaceVariant = PaperAltLight,  // _surfaceAltColor(light) = paperAltLight
    onSurfaceVariant = Color(0xFF4A4A4A),
    surfaceContainerHighest = PaperAltLight,
    surfaceContainerHigh = PaperAltLight,
    surfaceContainer = Color.White,
    outline = BorderLight,
    outlineVariant = Color(0xFFE5DED1),
)

val LocalIsDarkTheme = androidx.compose.runtime.staticCompositionLocalOf<Boolean> { false }
val LocalSnackbarHostState = androidx.compose.runtime.staticCompositionLocalOf<SnackbarHostState> {
    error("No SnackbarHostState provided")
}

@Composable
fun TimetyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TimetyTypography,
            shapes = TimetyShapes,
            content = content
        )
    }
}

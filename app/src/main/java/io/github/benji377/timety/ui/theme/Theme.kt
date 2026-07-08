package io.github.benji377.timety.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Fixed neobrutalist color schemes for light and dark mode. No Material You / dynamic color —
// the palette is always the same regardless of device wallpaper.

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
    surface = PaperAltDark,
    onSurface = InkDark,
    surfaceVariant = SurfaceAltDark,
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
    surface = Color.White,
    onSurface = InkLight,
    surfaceVariant = PaperAltLight,
    onSurfaceVariant = Color(0xFF4A4A4A),
    surfaceContainerHighest = PaperAltLight,
    surfaceContainerHigh = PaperAltLight,
    surfaceContainer = Color.White,
    outline = BorderLight,
    outlineVariant = Color(0xFFE5DED1),
)

val LocalIsDarkTheme = androidx.compose.runtime.staticCompositionLocalOf { false }
val LocalSnackbarHostState = androidx.compose.runtime.staticCompositionLocalOf<SnackbarHostState> {
    error("No SnackbarHostState provided")
}

/** Applies the app's fixed light/dark Material 3 color scheme, typography, and shapes. */
@Composable
fun TimetyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // The app draws edge-to-edge (see MainActivity.enableEdgeToEdge), so the status bar is
            // transparent and only its icon appearance needs syncing with the theme.
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TimetyTypography,
            shapes = TimetyShapes,
            content = content
        )
    }
}

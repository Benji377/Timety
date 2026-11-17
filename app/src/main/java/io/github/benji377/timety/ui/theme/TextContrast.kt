package io.github.benji377.timety.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * This color as used for text: unchanged when it already reads well on the current theme's
 * backgrounds, otherwise made just dark enough (light mode) or light enough (dark mode) that it
 * does. Hue and saturation stay, so orange stays orange rather than turning brown or grey. Fills,
 * borders and icons keep the base color.
 */
@Composable
@ReadOnlyComposable
fun Color.forText(): Color = forText(MaterialTheme.colorScheme)

/** [forText] for a given [scheme], for places without a Compose theme such as widgets. */
fun Color.forText(scheme: ColorScheme): Color {
    if (this == Color.Unspecified) return this
    // The darkest and lightest backgrounds text sits on: screens, cards and tinted containers.
    val backgrounds = listOf(scheme.background, scheme.surface, scheme.surfaceContainerHighest)
    val darkTheme = scheme.onSurface.luminance() > scheme.background.luminance()
    val (hue, saturation, lightness) = toHsl()
    val target = if (darkTheme) 1f else 0f
    for (step in 0..TEXT_SHIFT_STEPS) {
        val shifted = lightness + (target - lightness) * step / TEXT_SHIFT_STEPS
        val candidate = Color.hsl(hue, saturation, shifted, alpha)
        if (backgrounds.all { contrastRatio(candidate, it) >= TEXT_MIN_CONTRAST }) return candidate
    }
    return scheme.onSurface
}

/** WCAG contrast ratio between two opaque colors, from 1 (same) to 21 (black on white). */
internal fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
}

/** Hue in degrees, saturation and lightness from 0 to 1; the inverse of [Color.hsl]. */
private fun Color.toHsl(): Triple<Float, Float, Float> {
    val maxC = max(red, max(green, blue))
    val minC = min(red, min(green, blue))
    val lightness = (maxC + minC) / 2
    val delta = maxC - minC
    if (delta == 0f) return Triple(0f, 0f, lightness)
    val saturation = delta / (1 - abs(2 * lightness - 1))
    val hue = when (maxC) {
        red -> 60 * (((green - blue) / delta).mod(6f))
        green -> 60 * ((blue - red) / delta + 2)
        else -> 60 * ((red - green) / delta + 4)
    }
    return Triple(hue, saturation.coerceIn(0f, 1f), lightness)
}

/** WCAG AA for normal-size text. */
private const val TEXT_MIN_CONTRAST = 4.5f

/** How finely the lightness moves; 40 steps of 2.5% each. */
private const val TEXT_SHIFT_STEPS = 40

package io.github.benji377.timety.widget

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.material3.ColorProviders
import androidx.glance.unit.ColorProvider
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.theme.DarkColorScheme
import io.github.benji377.timety.ui.theme.LightColorScheme

/**
 * The app's own light and dark palettes for the widgets, in place of the wallpaper-based system
 * colors. Light or dark follows the phone.
 */
internal val TimetyWidgetColors = ColorProviders(light = LightColorScheme, dark = DarkColorScheme)

/**
 * Which corners an [OutlinedBox] rounds, and how much: the widget itself like the app's cards,
 * rows like its list tiles. The pieces of a habit stack join into one tile-shaped outline.
 */
internal enum class WidgetCorners(@param:DrawableRes val outer: Int?, @param:DrawableRes val inner: Int?) {
    CARD(R.drawable.widget_shape_rounded, R.drawable.widget_shape_rounded_inner),
    ALL(R.drawable.widget_shape_tile, R.drawable.widget_shape_tile_inner),
    TOP(R.drawable.widget_shape_top, R.drawable.widget_shape_top_inner),
    BOTTOM(R.drawable.widget_shape_bottom, R.drawable.widget_shape_bottom_inner),

    // A stack's middle row: square, outlined on the sides only.
    NONE(null, null),
}

/**
 * A [fill]ed box with a 1dp [border], the widget version of the app's outlined tiles. Glance has no
 * border modifier, so the border is a [border]-tinted shape showing around a [fill]-tinted one.
 */
@Composable
internal fun OutlinedBox(
    border: ColorProvider,
    fill: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
    corners: WidgetCorners = WidgetCorners.ALL,
    innerModifier: GlanceModifier = GlanceModifier.fillMaxWidth(),
    content: @Composable () -> Unit,
) {
    val inset = when (corners) {
        WidgetCorners.CARD, WidgetCorners.ALL -> GlanceModifier.padding(1.dp)
        WidgetCorners.TOP -> GlanceModifier.padding(start = 1.dp, top = 1.dp, end = 1.dp)
        WidgetCorners.BOTTOM -> GlanceModifier.padding(start = 1.dp, bottom = 1.dp, end = 1.dp)
        WidgetCorners.NONE -> GlanceModifier.padding(horizontal = 1.dp)
    }
    Box(modifier = modifier.then(tinted(corners.outer, border)).then(inset)) {
        Box(modifier = tinted(corners.inner, fill).then(innerModifier)) { content() }
    }
}

private fun tinted(@DrawableRes shape: Int?, color: ColorProvider): GlanceModifier =
    if (shape == null) GlanceModifier.background(color)
    else GlanceModifier.background(ImageProvider(shape), colorFilter = ColorFilter.tint(color))

package io.github.benji377.timety.ui.components.common

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.ui.theme.AppTheme

// Duration (ms) of the press-toward-shadow animation in neoPressShadow - short enough to read as
// an immediate physical "push," per the neobrutalist reference sheet's tap interaction.
private const val NEO_PRESS_ANIM_DURATION_MS = 100

/**
 * Composable defaults for [neoShadow] and [neoPressShadow], split out because a shadow color
 * needs a [MaterialTheme] read and so can't be a plain top-level `val`.
 */
object NeoShadowDefaults {
    /**
     * Default hard-shadow color: [MaterialTheme.colorScheme.outline], the same color used for
     * every neo border, so a shadow reads as a heavier echo of its own element's border rather
     * than a separate design choice. Pass an accent color instead (e.g. a card's colored border)
     * for the reference sheet's "colored shadow echoing a card's accent border" treatment.
     */
    val color: Color
        @Composable get() = MaterialTheme.colorScheme.outline
}

/**
 * Draws neobrutalism's signature flat, zero-blur, hard-offset shadow behind the element: [shape]
 * filled solid with [color], translated [offset] right and down of the element's own bounds - no
 * blur, no alpha falloff.
 *
 * This is `@Composable` (rather than a plain [Modifier] extension) purely so its [color] default
 * ([NeoShadowDefaults.color]) can read [MaterialTheme] without every call site having to thread a
 * color through by hand; nothing about the shadow itself depends on composition beyond that.
 *
 * Opacity note: the shadow is the whole [shape] filled solid, drawn *behind* the element - so the
 * element must paint an opaque background of its own. Give a transparent element a shadow and the
 * shadow shows straight through it, rendering it as a solid block of [color]. This bites hardest on
 * components whose Material defaults are transparent (text fields, tab rows, unselected chips) and
 * on any "resting" state that deliberately has no fill: apply the modifier conditionally rather
 * than passing a zero [offset], since a zero-offset shadow still paints, exactly under the element.
 *
 * Layout note: the shadow is painted in [Modifier.drawBehind], which extends past this element's
 * own layout bounds by [offset] on the right/bottom edges - it does not reserve any space for
 * itself. A clipping ancestor, or a sibling placed flush with no gap, will cut the shadow off or
 * get drawn over it. Leave at least [offset] of spacing past the element's trailing/bottom edges;
 * [AppTheme.listTileScreenMargin] already clears [AppTheme.neoShadowOffsetSmall], the shadow
 * offset [NeoListTile] actually uses, but a component placed flush against a container edge or
 * another element needs that same margin added explicitly.
 *
 * @param shape the outline the shadow is drawn in; should match the element's own shape.
 * @param offset how far right and down the shadow sits from the element's bounds.
 * @param color solid fill color.
 */
@Composable
fun Modifier.neoShadow(
    shape: Shape = AppTheme.brNeo,
    offset: Dp = AppTheme.neoShadowOffset,
    color: Color = NeoShadowDefaults.color,
): Modifier = drawBehind {
    val offsetPx = offset.toPx()
    translate(left = offsetPx, top = offsetPx) {
        drawOutline(outline = shape.createOutline(size, layoutDirection, this), color = color)
    }
}

/**
 * Combines [neoShadow] with the neobrutalist press interaction: while [interactionSource] reports
 * a press, the element translates toward its shadow (via [Modifier.offset], which shifts
 * placement without changing measured size) while the shadow shrinks to nothing - so the
 * element's total footprint (its own bounds plus the shadow's reach) stays constant. Both animate
 * together over [NEO_PRESS_ANIM_DURATION_MS].
 *
 * Apply this instead of plain [neoShadow] on any bordered, clickable element (buttons, FABs,
 * clickable cards) so the reference sheet's "pushed into the page" tap feedback is consistent
 * app-wide. Shares [neoShadow]'s layout note: leave [offset] of trailing/bottom spacing so the
 * shadow at rest isn't clipped or overdrawn.
 *
 * @param interactionSource the same source driving the element's own press ripple/indication.
 * @param shape the outline for both the shadow and the press translation; should match the
 * element's own shape.
 * @param offset the shadow's resting offset, and the element's full press-travel distance.
 * @param color solid shadow fill color.
 */
@Composable
fun Modifier.neoPressShadow(
    interactionSource: InteractionSource,
    shape: Shape = AppTheme.brNeo,
    offset: Dp = AppTheme.neoShadowOffset,
    color: Color = NeoShadowDefaults.color,
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val shadowOffset by animateDpAsState(
        targetValue = if (isPressed) 0.dp else offset,
        animationSpec = tween(NEO_PRESS_ANIM_DURATION_MS),
        label = "neoPressShadowOffset",
    )
    return this
        .offset(x = offset - shadowOffset, y = offset - shadowOffset)
        .neoShadow(shape = shape, offset = shadowOffset, color = color)
}

package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import io.github.benji377.timety.ui.theme.AppTheme

/**
 * Bordered [Card] used for the app's prominent "feature card" tier: standalone summary/config
 * cards such as the home screen's daily-goal card or a stat card, where the bold
 * [AppTheme.neoBorderWidth] border and [AppTheme.brNeo] corner radius are meant to draw the eye.
 * For the denser list-row tier (task/habit/recurring-task rows), use [NeoListTile] instead.
 *
 * By default the card casts a hard [neoShadow] in [NeoShadowDefaults.color] (the same solid
 * near-black-or-cream used everywhere else). [shadowColor] does *not* default to [borderColor]:
 * many call sites pass a translucent, alpha-faded [borderColor] (a completed row's dimmed accent,
 * a picker's `outlineVariant.copy(alpha = ...)`), and silently tracking that would smuggle a soft
 * translucent shadow past reference sheet §2's "hard offset shadow only... solid color" rule.
 * Pass a solid accent color explicitly to opt into the reference sheet's "colored shadow echoing a
 * card's accent border" treatment. Pass [shadow] = false for the rare case a card should stay flat.
 * When [onClick] is non-null, the card is rendered with Material3's clickable
 * `Card(onClick = ...)` overload and the shadow uses [neoPressShadow] so the card pushes toward
 * the page on tap; otherwise it renders as a plain, non-interactive card with a static shadow.
 */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    shape: Shape = AppTheme.brNeo,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: Dp = AppTheme.neoBorderWidth,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    shadow: Boolean = true,
    shadowOffset: Dp = AppTheme.neoShadowOffset,
    shadowColor: Color = NeoShadowDefaults.color,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val cardModifier = when {
        !shadow -> modifier
        onClick != null -> modifier.neoPressShadow(
            interactionSource = interactionSource,
            shape = shape,
            offset = shadowOffset,
            color = shadowColor,
        )

        else -> modifier.neoShadow(shape = shape, offset = shadowOffset, color = shadowColor)
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            shape = shape,
            border = BorderStroke(borderWidth, borderColor),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = AppTheme.neoCardElevation,
            interactionSource = interactionSource,
            content = content,
        )
    } else {
        Card(
            modifier = cardModifier,
            shape = shape,
            border = BorderStroke(borderWidth, borderColor),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = AppTheme.neoCardElevation,
            content = content,
        )
    }
}

/**
 * Bordered [Card] used for the app's dense "list row" tier: task, recurring-task, and habit rows,
 * where the thinner [AppTheme.listTileBorderWidth] border and [AppTheme.brMedium] corner radius
 * keep repeated rows visually quiet. Casts the smaller [AppTheme.neoShadowOffsetSmall] shadow by
 * default so adjacent rows don't crowd each other. For standalone feature/summary cards, use
 * [NeoCard] instead.
 *
 * Delegates to [NeoCard] with list-tile defaults so both tiers share one implementation.
 */
@Composable
fun NeoListTile(
    modifier: Modifier = Modifier,
    shape: Shape = AppTheme.brMedium,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: Dp = AppTheme.listTileBorderWidth,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    shadow: Boolean = true,
    shadowOffset: Dp = AppTheme.neoShadowOffsetSmall,
    shadowColor: Color = NeoShadowDefaults.color,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    NeoCard(
        modifier = modifier,
        shape = shape,
        borderColor = borderColor,
        borderWidth = borderWidth,
        containerColor = containerColor,
        shadow = shadow,
        shadowOffset = shadowOffset,
        shadowColor = shadowColor,
        onClick = onClick,
        content = content,
    )
}

package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import io.github.benji377.timety.ui.theme.AppTheme

/**
 * Bordered [Card] used for the app's prominent "feature card" tier: standalone summary/config
 * cards such as the home screen's daily-goal card or a stat card, distinguished from the page by
 * their [containerColor] fill, hairline border, and the larger [AppTheme.brNeo] corner radius.
 * For the denser list-row tier (task/habit/recurring-task rows), use [NeoListTile] instead.
 *
 * The card is flat: no Material elevation and no offset shadow. Depth cues come from the fill and
 * border alone, so a card whose [containerColor] matches its background will not separate from it
 * - give such a card a distinct fill rather than expecting a shadow to do the work.
 *
 * When [onClick] is non-null the card renders with Material3's clickable `Card(onClick = ...)`
 * overload, whose ripple supplies the tap feedback; otherwise it is a plain, non-interactive card.
 */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    shape: Shape = AppTheme.brNeo,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: Dp = AppTheme.borderHairline,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            border = BorderStroke(borderWidth, borderColor),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = AppTheme.flatCardElevation,
            content = content,
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            border = BorderStroke(borderWidth, borderColor),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = AppTheme.flatCardElevation,
            content = content,
        )
    }
}

/**
 * Bordered [Card] used for the app's dense "list row" tier: task, recurring-task, and habit rows,
 * where the tighter [AppTheme.brMedium] corner radius keeps repeated rows visually quiet. For
 * standalone feature/summary cards, use [NeoCard] instead.
 *
 * Delegates to [NeoCard] with list-tile defaults so both tiers share one implementation.
 */
@Composable
fun NeoListTile(
    modifier: Modifier = Modifier,
    shape: Shape = AppTheme.brMedium,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: Dp = AppTheme.borderHairline,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    NeoCard(
        modifier = modifier,
        shape = shape,
        borderColor = borderColor,
        borderWidth = borderWidth,
        containerColor = containerColor,
        onClick = onClick,
        content = content,
    )
}

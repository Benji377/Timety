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
 * Bordered, flat (no shadow) [Card] used for the app's prominent "feature card" tier: standalone
 * summary/config cards such as the home screen's daily-goal card or a stat card, where the bold
 * [AppTheme.neoBorderWidth] border and [AppTheme.brNeo] corner radius are meant to draw the eye.
 * For the denser list-row tier (task/habit/recurring-task rows), use [NeoListTile] instead.
 *
 * When [onClick] is non-null, the card is rendered with Material3's clickable `Card(onClick = ...)`
 * overload; otherwise it renders as a plain, non-interactive card.
 */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    shape: Shape = AppTheme.brNeo,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: Dp = AppTheme.neoBorderWidth,
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
            elevation = AppTheme.neoCardElevation,
            content = content,
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            border = BorderStroke(borderWidth, borderColor),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = AppTheme.neoCardElevation,
            content = content,
        )
    }
}

/**
 * Bordered, flat (no shadow) [Card] used for the app's dense "list row" tier: task, recurring-task,
 * and habit rows, where the thinner [AppTheme.listTileBorderWidth] border and [AppTheme.brMedium]
 * corner radius keep repeated rows visually quiet. For standalone feature/summary cards, use
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

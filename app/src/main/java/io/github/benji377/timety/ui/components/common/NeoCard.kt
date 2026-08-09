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
 * Bordered [Card] for the prominent "feature card" tier: standalone summary/config cards such as
 * the home screen's daily-goal card. For dense list rows use [NeoListTile].
 *
 * The card is flat, so its [AppTheme.borderCard] border and [containerColor] fill are the only
 * things separating it from the page - give it a fill distinct from its background.
 * A non-null [onClick] selects Material3's clickable `Card` overload, whose ripple supplies tap
 * feedback.
 */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    shape: Shape = AppTheme.brNeo,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: Dp = AppTheme.borderCard,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val border = BorderStroke(borderWidth, borderColor)
    val colors = CardDefaults.cardColors(containerColor = containerColor)
    val elevation = AppTheme.flatCardElevation
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            border = border,
            colors = colors,
            elevation = elevation,
            content = content,
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            border = border,
            colors = colors,
            elevation = elevation,
            content = content,
        )
    }
}

/**
 * Bordered [Card] for the dense "list row" tier (task, recurring-task, and habit rows), where the
 * tighter [AppTheme.brMedium] radius keeps repeated rows visually quiet. Delegates to [NeoCard].
 */
@Composable
fun NeoListTile(
    modifier: Modifier = Modifier,
    shape: Shape = AppTheme.brMedium,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: Dp = AppTheme.borderCard,
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

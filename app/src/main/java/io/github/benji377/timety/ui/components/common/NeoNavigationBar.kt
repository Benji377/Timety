package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.ui.theme.AppTheme

/**
 * Standard bottom navigation bar: a bold top edge and a flat, non-elevated background,
 * so the bar reads as a distinct neobrutalist band separated from screen content
 * (mirroring [NeoTopBar]'s bottom border).
 */
@Composable
fun NeoNavigationBar(content: @Composable RowScope.() -> Unit) {
    Column {
        // Bold top edge so the bar reads as a distinct neobrutalist band.
        HorizontalDivider(
            thickness = AppTheme.neoBorderWidth,
            color = MaterialTheme.colorScheme.outline,
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp,
            content = content,
        )
    }
}

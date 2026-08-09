package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import io.github.benji377.timety.ui.theme.AppTheme

/**
 * Circular badge holding an icon or short label: solid neutral fill plus a hairline border in
 * [color], the app's one icon-badge idiom (XP breakdown, session rows, stat buckets). The fill
 * stays solid - an alpha tint would read as a borderless soft-UI badge.
 */
@Composable
fun AccentBadge(
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(AppTheme.borderHairline, color, CircleShape),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

package io.github.benji377.timety.ui.components.common

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.forText

/**
 * Low-emphasis, neo-consistent sibling of [NeoButton]: no fill, no border, just a bold label
 * ripple-clipped to [AppTheme.brNeo]. Intended for dialog dismiss/cancel actions. The label takes
 * [color] in its readable text shade (see [forText]).
 */
@Composable
fun NeoTextButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    NeoTextButton(onClick = onClick, modifier = modifier, enabled = enabled, color = color) {
        Text(text, fontWeight = AppTheme.fwBold)
    }
}

/** [NeoTextButton] with custom [content] in place of a bold text label. */
@Composable
fun NeoTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = AppTheme.brNeo,
        colors = ButtonDefaults.textButtonColors(contentColor = color.forText()),
        content = content,
    )
}

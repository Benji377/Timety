package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.ui.theme.AppTheme

/**
 * Filled [Button] restyled with the app's neobrutalist border, no Material elevation, and a hard
 * [neoPressShadow] that pushes the button toward the page on tap.
 */
@Composable
fun NeoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = AppTheme.brNeo,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
    border: BorderStroke? = BorderStroke(
        AppTheme.neoBorderWidth,
        MaterialTheme.colorScheme.outline
    ),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    shadowColor: Color = NeoShadowDefaults.color,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.neoPressShadow(
            interactionSource = interactionSource,
            shape = shape,
            color = shadowColor,
        ),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Elevated variant of [NeoButton], restyled with the app's neobrutalist border and the same hard
 * [neoPressShadow] press interaction.
 */
@Composable
fun NeoElevatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = AppTheme.brNeo,
    colors: ButtonColors = ButtonDefaults.elevatedButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.elevatedButtonElevation(
        0.dp,
        0.dp,
        0.dp,
        0.dp,
        0.dp
    ),
    border: BorderStroke? = BorderStroke(
        AppTheme.neoBorderWidth,
        MaterialTheme.colorScheme.outline
    ),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    shadowColor: Color = NeoShadowDefaults.color,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier.neoPressShadow(
            interactionSource = interactionSource,
            shape = shape,
            color = shadowColor,
        ),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

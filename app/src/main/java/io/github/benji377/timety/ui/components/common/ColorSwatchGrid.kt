package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.ui.theme.AppTheme


/**
 * Grid of circular color swatches with the selected one outlined, for the color pickers in the
 * category, focus-tag, and habit editors. Callers size the grid via [modifier].
 */
@Composable
fun ColorSwatchGrid(
    colors: List<Color>,
    selectedColor: Color,
    onSelect: (Color) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 6,
    swatchSize: Dp = 36.dp,
    spacing: Dp = AppTheme.spaceSmall,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing),
        modifier = modifier,
    ) {
        items(colors) { optionColor ->
            val isSelected = optionColor == selectedColor
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(swatchSize)
                    .clip(CircleShape)
                    .background(optionColor)
                    // Every swatch gets a border so it reads as a deliberate component, not just a
                    // floating disc of color. Selection is carried by that border's *color*, not
                    // its width: at the flat design's single hairline weight a thicker ring is no
                    // longer available as a state cue, so the selected swatch takes the full-
                    // contrast `onSurface` ink while the rest keep the muted `outline`.
                    .border(
                        width = AppTheme.borderHairline,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = CircleShape,
                    )
                    .clickable(interactionSource = interactionSource, indication = ripple()) {
                        onSelect(optionColor)
                    },
            )
        }
    }
}

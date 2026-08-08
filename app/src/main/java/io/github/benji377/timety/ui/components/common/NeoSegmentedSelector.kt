package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.github.benji377.timety.ui.theme.AppTheme

/**
 * Equal-width segmented selector for options that read as short text labels rather than icons
 * (e.g. habit frequency: Daily / Flexible / Specific). This is the text-label sibling of Task
 * Detail's `AccordionSelector`: values like frequency don't have a natural per-option icon to
 * collapse to when unselected, so every segment keeps its label at a fixed equal width instead of
 * expanding on select - but the visual language is identical: a bold outer border and hard shadow
 * on the whole track, and the selected segment gets its own solid fill + border rather than a flat
 * tint with only a checkmark. Full contrast is kept in both edit and view mode; only tap-gating and
 * the track's background respond to [isEditing].
 */
@Composable
fun <T> NeoSegmentedSelector(
    values: List<T>,
    selectedValue: T,
    isEditing: Boolean,
    onSelected: (T) -> Unit,
    labelBuilder: (T) -> String,
    modifier: Modifier = Modifier,
    activeBgColor: Color = MaterialTheme.colorScheme.primary,
    activeTextColor: Color = Color.White,
) {
    val borderColor = MaterialTheme.colorScheme.outline
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AppTheme.segmentedControlHeight)
            // Lifted only while editable, matching the text fields beside it, so a read-only form
            // is uniformly flat instead of mixing raised and recessed controls.
            .then(
                if (isEditing) Modifier.neoShadow(shape = AppTheme.brPill) else Modifier
            )
            .clip(AppTheme.brPill)
            // Opaque in both modes: the hard shadow is painted behind the whole control, so a
            // transparent view-mode fill would show that shadow through the unselected segments.
            .background(
                if (isEditing) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .border(AppTheme.neoBorderWidth, borderColor, AppTheme.brPill)
    ) {
        values.forEachIndexed { index, value ->
            val isSelected = value == selectedValue
            val isLast = index == values.lastIndex
            // A divider next to the selected segment would double up with that segment's own
            // border, so only draw it between two unselected neighbors.
            val nextIsSelected = !isLast && values[index + 1] == selectedValue
            val drawsDivider = !isLast && !isSelected && !nextIsSelected
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .let { m ->
                        if (drawsDivider) {
                            m.drawBehind {
                                drawLine(
                                    color = borderColor,
                                    start = Offset(size.width, 0f),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = AppTheme.listTileBorderWidth.toPx()
                                )
                            }
                        } else m
                    }
                    .clickable(enabled = isEditing) { onSelected(value) }
                    .padding(AppTheme.neoShadowOffsetSmall)
                    .then(
                        if (isSelected) {
                            Modifier
                                .clip(AppTheme.brMedium)
                                .background(activeBgColor, AppTheme.brMedium)
                                .border(AppTheme.listTileBorderWidth, borderColor, AppTheme.brMedium)
                        } else Modifier
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = labelBuilder(value),
                    fontWeight = if (isSelected) AppTheme.fwBold else AppTheme.fwMedium,
                    color = if (isSelected) activeTextColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = AppTheme.fsSegmentLabel,
                    letterSpacing = AppTheme.lsNarrow,
                )
            }
        }
    }
}

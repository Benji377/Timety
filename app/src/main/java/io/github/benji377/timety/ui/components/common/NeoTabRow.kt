package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import io.github.benji377.timety.ui.theme.AppTheme

/**
 * Flat, bold-bordered segmented tab selector - the neobrutalist replacement for Material's
 * `TabRow`. Instead of an animated indicator sliding under a soft-shadowed pill, the selected
 * segment is simply filled solid with [activeColor]; there is no shadow and no elevation anywhere
 * in this component.
 *
 * @param tabs labels shown left to right.
 * @param selectedIndex index into [tabs] of the currently active segment.
 * @param onTabSelected invoked with the tapped segment's index.
 * @param activeColor solid fill used for the selected segment.
 */
@Composable
fun NeoTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.brNeo)
            .border(AppTheme.neoBorderWidth, MaterialTheme.colorScheme.outline, AppTheme.brNeo)
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex

            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(index) }
                    .background(if (isSelected) activeColor else Color.Transparent)
                    .padding(vertical = AppTheme.spaceMedium),
                textAlign = TextAlign.Center,
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) AppTheme.fwBold else FontWeight.Normal,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

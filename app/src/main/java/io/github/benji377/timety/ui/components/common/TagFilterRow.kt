package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.tag.TagEntity
import io.github.benji377.timety.ui.theme.AppTheme

/** How many tags a list row names before summing up the rest as "+N". */
private const val MAX_ROW_TAGS = 2

/**
 * Filter pills for the [tags] used on a screen. Several can be selected, narrowing the list, and
 * "All" clears them. Shows nothing when no tag is in use.
 */
@Composable
fun TagFilterRow(
    tags: List<TagEntity>,
    selectedIds: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = AppTheme.spaceSmall),
) {
    if (tags.isEmpty()) return
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall),
    ) {
        item(key = "all") {
            NeoFilterChip(
                selected = selectedIds.isEmpty(),
                onClick = { onSelectionChange(emptySet()) },
                label = stringResource(R.string.taskListFilterAll),
            )
        }
        items(tags, key = { it.id }) { tag ->
            val isSelected = tag.id in selectedIds
            NeoFilterChip(
                selected = isSelected,
                onClick = {
                    onSelectionChange(if (isSelected) selectedIds - tag.id else selectedIds + tag.id)
                },
                label = tag.name,
                selectedColor = Color(tag.colorValue),
                leadingIcon = if (isSelected) null else ({ TagDot(tag.colorValue) }),
            )
        }
    }
}

/** A list row's tags: the first two as small chips, then "+N" for any others. */
@Composable
fun TagChips(tags: List<TagEntity>, modifier: Modifier = Modifier) {
    if (tags.isEmpty()) return
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        tags.take(MAX_ROW_TAGS).forEach { tag ->
            Text(
                text = tag.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = AppTheme.fsLabel,
                fontWeight = AppTheme.fwMedium,
                color = Color.White,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .widthIn(max = 120.dp)
                    .background(Color(tag.colorValue), AppTheme.brPill)
                    .padding(horizontal = AppTheme.spaceSmall, vertical = 2.dp),
            )
            Spacer(Modifier.width(AppTheme.spaceXSmall))
        }
        if (tags.size > MAX_ROW_TAGS) {
            Text(
                text = "+${tags.size - MAX_ROW_TAGS}",
                fontSize = AppTheme.fsLabel,
                fontWeight = AppTheme.fwMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

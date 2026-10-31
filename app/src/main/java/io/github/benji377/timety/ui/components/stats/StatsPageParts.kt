package io.github.benji377.timety.ui.components.stats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.data.model.tag.TagEntity
import io.github.benji377.timety.ui.theme.AppTheme
import java.time.LocalDate

/** The message of a stats page with nothing to show yet. */
@Composable
fun StatsEmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppTheme.spaceLarge),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, textAlign = TextAlign.Center)
    }
}

/** A stats page: the period controls stay pinned above the scrolling [content]. */
@Composable
fun StatsPeriodPage(
    state: StatsPeriodState,
    today: LocalDate,
    tags: List<TagEntity>,
    activeTagIds: Set<String>,
    accentColor: Color,
    itemSpacing: Dp = AppTheme.space2XLarge,
    content: LazyListScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        StatsPeriodControls(
            state = state,
            today = today,
            tags = tags,
            activeTagIds = activeTagIds,
            accentColor = accentColor,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AppTheme.spaceLarge),
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            content = content,
        )
    }
}

/** A page's headline number, with what it means and how it compares below it when known. */
@Composable
fun StatsTotal(value: String, caption: String? = null, comparison: String? = null) {
    Column {
        Text(
            value,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
        )
        listOfNotNull(caption, comparison).forEach { line ->
            Text(
                line,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A tappable row ending in a chevron, leading to more detail. */
@Composable
fun StatsNavRow(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(vertical = AppTheme.spaceMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** One number in a [StatTileRow]. */
data class StatTile(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: (() -> Unit)? = null,
)

/** Compact tiles sharing a row's width equally, all as tall as the tallest. */
@Composable
fun StatTileRow(tiles: List<StatTile>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall),
    ) {
        tiles.forEach { tile ->
            StatCard(
                title = tile.title,
                value = tile.value,
                icon = tile.icon,
                color = tile.color,
                style = StatCardStyle.COMPACT_VERTICAL,
                onClick = tile.onClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

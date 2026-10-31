package io.github.benji377.timety.ui.components.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.components.common.NeoProgressBar
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.util.stats.TagBreakdown

/**
 * One bar per tag, scaled against the largest, then "Untagged". Bars rather than a stacked whole,
 * because an item with several tags counts under each and the totals overlap.
 */
@Composable
fun TagBreakdownBars(breakdown: TagBreakdown, valueText: @Composable (Int) -> String) {
    val max = breakdown.maxValue.coerceAtLeast(1).toFloat()
    Column {
        breakdown.totals.forEach { total ->
            TagBar(total.tag.name, Color(total.tag.colorValue), valueText(total.value), total.value / max)
        }
        if (breakdown.untagged > 0) {
            TagBar(
                stringResource(R.string.focusTargetUntagged),
                MaterialTheme.colorScheme.outline,
                valueText(breakdown.untagged),
                breakdown.untagged / max,
            )
        }
    }
}

@Composable
private fun TagBar(label: String, color: Color, value: String, fraction: Float) {
    Column(modifier = Modifier.padding(bottom = AppTheme.spaceMedium)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                modifier = Modifier.weight(1f),
                fontSize = AppTheme.fsBodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(value, fontSize = AppTheme.fsBodyMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(AppTheme.spaceXSmall))
        NeoProgressBar(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}

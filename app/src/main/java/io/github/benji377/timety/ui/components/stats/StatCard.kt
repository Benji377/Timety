package io.github.benji377.timety.ui.components.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.ui.theme.AppTheme


/** Layout variants for [StatCard]. */
enum class StatCardStyle { KPI, COMPACT_VERTICAL, COMPACT_HEADER }


/** Bordered card showing an icon, [value], and [title]; [style] picks the layout and sizing. */
@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    style: StatCardStyle = StatCardStyle.KPI,
) {
    val sized = when (style) {
        StatCardStyle.COMPACT_VERTICAL -> modifier.size(width = 105.dp, height = 90.dp)
        StatCardStyle.COMPACT_HEADER -> modifier.size(width = 140.dp, height = 100.dp)
        StatCardStyle.KPI -> modifier
    }

    Card(
        modifier = sized,
        shape = AppTheme.brNeo,
        border = BorderStroke(AppTheme.neoBorderWidth, color),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        when (style) {
            StatCardStyle.KPI -> KpiContent(title, value, icon, color)
            StatCardStyle.COMPACT_VERTICAL -> CompactVerticalContent(title, value, icon, color)
            StatCardStyle.COMPACT_HEADER -> CompactHeaderContent(title, value, icon, color)
        }
    }
}

@Composable
private fun KpiContent(title: String, value: String, icon: ImageVector, color: Color) {
    Column(modifier = Modifier.padding(16.dp)) {
        Icon(icon, contentDescription = null, tint = color)
        Spacer(Modifier.height(12.dp))
        Text(value, fontSize = 24.sp, fontWeight = AppTheme.fwBold)
        Text(
            title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CompactVerticalContent(title: String, value: String, icon: ImageVector, color: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.spaceSmall),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(AppTheme.iconSizeSmall)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            fontSize = AppTheme.fsHeadingMedium,
            fontWeight = AppTheme.fwExtraBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            title,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = AppTheme.fsCaption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = AppTheme.fwBold,
        )
    }
}

@Composable
private fun CompactHeaderContent(title: String, value: String, icon: ImageVector, color: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.spaceMedium),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(AppTheme.iconSizeSmall)
            )
            Spacer(Modifier.width(AppTheme.spaceXSmall))
            Text(
                title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = AppTheme.fsCaption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = AppTheme.fwBold,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            value,
            fontSize = AppTheme.fsHeadingMedium,
            fontWeight = AppTheme.fwExtraBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

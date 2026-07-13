package io.github.benji377.timety.ui.components.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.ui.theme.AppTheme


/** Bold section title with an optional muted subtitle, heading the stats-screen cards. */
@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Column {
        Text(title, fontSize = AppTheme.fsHeadingSmall, fontWeight = AppTheme.fwBold)
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(AppTheme.spaceXSmall))
            Text(
                subtitle,
                fontSize = AppTheme.fsBodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


/** Small filled circle keying a chart legend or breakdown row to its series color. */
@Composable
fun LegendDot(color: Color, size: Dp = 12.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

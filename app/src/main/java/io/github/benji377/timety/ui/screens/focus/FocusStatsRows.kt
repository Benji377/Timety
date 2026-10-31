package io.github.benji377.timety.ui.screens.focus

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.DistractionUIType
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.focus.FocusTargetType
import io.github.benji377.timety.data.model.focus.color
import io.github.benji377.timety.data.model.focus.icon
import io.github.benji377.timety.data.model.tag.TagEntity
import io.github.benji377.timety.ui.components.common.AccentBadge
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.ui.viewmodel.DistractionWithSession
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import java.time.ZoneId

@Composable
internal fun SessionRow(
    session: FocusSessionEntity,
    targetName: String,
    modeName: String,
    tags: List<TagEntity>,
    onEdit: () -> Unit,
) {
    val icon = session.targetType.icon
    val color = session.targetType.takeIf { it != FocusTargetType.TAG }?.color
        ?: tags.firstOrNull()?.let { Color(it.colorValue) }
        ?: session.targetType.color
    val dfs = LocalDateFormatSettings.current
    val dateStr = AppDateFormatUtils.formatDate(
        session.startTime,
        dfs.dateFormatCode
    )
    val timeStr = AppDateFormatUtils.formatTime(
        session.startTime,
        dfs.use24HourFormat
    )
    val mins = session.totalSecondsFocused / 60

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AccentBadge(color = color, size = 40.dp) {
            Icon(icon, contentDescription = null, tint = color)
        }
        Spacer(modifier = Modifier.width(AppTheme.spaceMedium))
        Column(modifier = Modifier.weight(1f)) {
            Text(targetName, fontWeight = FontWeight.SemiBold, color = color)
            Text(
                "$dateStr | $timeStr | ${AppDateFormatUtils.formatMinutesCompact(mins)} | $modeName",
                fontSize = AppTheme.fsBodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.Filled.Edit,
            contentDescription = stringResource(R.string.focusSessionEditTitle),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(AppTheme.iconSizeSmall),
        )
    }
}

@Composable
internal fun DistractionRow(
    entry: DistractionWithSession,
    targetName: String
) {
    val type =
        DistractionUIType.fromEntityType(entry.distraction.type)
    val use24Hour =
        LocalDateFormatSettings.current.use24HourFormat
    val zone = remember { ZoneId.systemDefault() }

    Row(
        modifier = Modifier.padding(vertical = AppTheme.spaceSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Keyed to the distraction type's own color, matching the icon tint.
        AccentBadge(color = type.color, size = 40.dp) {
            Icon(type.icon, contentDescription = null, tint = type.color)
        }
        Spacer(modifier = Modifier.width(AppTheme.spaceMedium))
        Column {
            Text(type.getLocalizedName(), fontWeight = FontWeight.SemiBold)
            Text(
                "${
                    AppDateFormatUtils.formatTimeWithSeconds(
                        entry.distraction.time,
                        use24Hour,
                        zone
                    )
                } | $targetName",
                fontSize = AppTheme.fsBodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

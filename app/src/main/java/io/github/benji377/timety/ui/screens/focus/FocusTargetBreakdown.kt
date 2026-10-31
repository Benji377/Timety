package io.github.benji377.timety.ui.screens.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.focus.FocusTargetType
import io.github.benji377.timety.data.model.focus.color
import io.github.benji377.timety.data.model.focus.icon
import io.github.benji377.timety.data.model.tag.TagEntity
import io.github.benji377.timety.ui.components.stats.SectionHeader
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ChartTeal
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import kotlin.math.roundToInt

private data class TargetTypeStat(
    val label: String,
    val color: Color,
    val icon: ImageVector,
    val minutes: Int
)

/** The string naming sessions of this target type as a group. */
internal val FocusTargetType.pluralLabelRes: Int
    get() = when (this) {
        FocusTargetType.TAG -> R.string.globalLabelTags
        FocusTargetType.TASK -> R.string.globalLabelTasks
        FocusTargetType.RECURRING_TASK -> R.string.recurringTasksTitle
        FocusTargetType.HABIT -> R.string.globalLabelHabits
        FocusTargetType.GOAL -> R.string.globalLabelGoals
    }

// Recurring tasks share the task blue everywhere else, but adjacent segments of the stacked bar
// below need distinct colors to stay readable.
private val FocusTargetType.chartColor: Color
    get() = if (this == FocusTargetType.RECURRING_TASK) ChartTeal else color

@Composable
internal fun TargetBreakdownSection(
    sessions: List<FocusSessionEntity>,
    sessionTags: Map<String, List<TagEntity>>,
) {
    // TAG also covers sessions with nothing selected, which get their own row instead of counting
    // as tagged.
    val (noTarget, targeted) = remember(sessions, sessionTags) {
        sessions.partition { it.targetType == FocusTargetType.TAG && sessionTags[it.id].isNullOrEmpty() }
    }
    val totals = remember(targeted) {
        targeted.groupBy { it.targetType }
            .mapValues { (_, typed) -> typed.sumOf { it.totalSecondsFocused / 60 } }
    }
    val noTargetMinutes = remember(noTarget) { noTarget.sumOf { it.totalSecondsFocused / 60 } }

    val stats = FocusTargetType.entries.map { type ->
        TargetTypeStat(
            stringResource(type.pluralLabelRes),
            type.chartColor,
            type.icon,
            totals[type] ?: 0
        )
    } + listOfNotNull(
        if (noTargetMinutes > 0) TargetTypeStat(
            stringResource(R.string.focusTargetEmpty),
            MaterialTheme.colorScheme.outline,
            FocusTargetType.TAG.icon,
            noTargetMinutes,
        ) else null
    )
    val totalMinutes = stats.sumOf { it.minutes }

    Column {
        Row(verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.weight(1f)) {
                SectionHeader(
                    stringResource(R.string.focusStatsSectionTargetBreakdownTitle),
                    stringResource(R.string.focusStatsSectionTargetBreakdownSubtitle),
                )
            }
            Spacer(modifier = Modifier.width(AppTheme.spaceLarge))
            Text(
                AppDateFormatUtils.formatMinutesCompact(totalMinutes),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(999.dp))
                // Solid neutral track. When totalMinutes is 0 the forEach below draws nothing and
                // this flat surfaceVariant fill alone reads as the "no data yet" state - no need
                // for a second, alpha-tinted fill layered on top of an already-solid background.
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            stats.filter { it.minutes > 0 }.forEach { stat ->
                Box(
                    modifier = Modifier
                        .weight(stat.minutes.toFloat())
                        .fillMaxHeight()
                        .background(stat.color)
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        stats.forEach { stat ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = AppTheme.spaceMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(stat.color)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    stat.label,
                    modifier = Modifier.weight(1f),
                    fontSize = AppTheme.fsBodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    AppDateFormatUtils.formatMinutesCompact(stat.minutes),
                    fontSize = AppTheme.fsBodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(AppTheme.spaceMedium))
                val pct =
                    if (totalMinutes == 0) 0 else ((stat.minutes.toFloat() / totalMinutes) * 100).roundToInt()
                Text(
                    "$pct%",
                    fontSize = AppTheme.fsBodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

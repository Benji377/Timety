package io.github.benji377.timety.ui.screens.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.goal.GoalWithEntries
import io.github.benji377.timety.ui.components.common.TagFilterRow
import io.github.benji377.timety.ui.components.stats.SectionHeader
import io.github.benji377.timety.ui.components.stats.StatCard
import io.github.benji377.timety.ui.components.stats.StatCardStyle
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.ui.utils.rememberToday
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.GoalViewModel
import io.github.benji377.timety.ui.viewmodel.TagViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.stats.GoalPace
import io.github.benji377.timety.util.stats.GoalPaceStatus
import io.github.benji377.timety.util.tag.TagFilter
import java.time.ZoneId
import kotlin.math.abs

/**
 * Where goals stand now, so there is no period to choose: active goals against a steady pace to
 * their deadlines, and completed goals against theirs.
 */
@Composable
fun StatsGoalsScreen(
    onOpenGoal: (String) -> Unit,
    goalViewModel: GoalViewModel = viewModel(factory = AppViewModelProvider.Factory),
    tagViewModel: TagViewModel = activityScopedViewModel(),
) {
    val allGoals by goalViewModel.goalsWithEntries.collectAsState()
    val goalTags by tagViewModel.goalTags.collectAsState()
    val today by rememberToday()
    val zone = remember { ZoneId.systemDefault() }
    var selectedTags by rememberSaveable { mutableStateOf(emptySet<String>()) }

    if (allGoals.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppTheme.spaceLarge),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.goalsEmpty), textAlign = TextAlign.Center)
        }
        return
    }

    val usedTags = remember(allGoals, goalTags) {
        TagFilter.usedTags(allGoals.map { it.goal.id }, goalTags)
    }
    val activeTagIds = TagFilter.activeIds(selectedTags, usedTags)
    val goals = remember(allGoals, activeTagIds, goalTags) {
        TagFilter.filter(allGoals, activeTagIds, goalTags) { it.goal.id }
    }
    val (completed, active) = remember(goals) { goals.partition { it.goal.completedAt != null } }
    val paces = remember(active, today) {
        active.sortedBy { it.goal.targetDate }
            .map { it to GoalPace.of(it.goal, it.progress, today, zone) }
    }
    val behindCount = paces.count { (_, pace) -> pace.status.isBehind }
    val newestFirst = remember(completed) { completed.sortedByDescending { it.goal.completedAt } }

    Column(modifier = Modifier.fillMaxSize()) {
        if (usedTags.isNotEmpty()) {
            TagFilterRow(
                tags = usedTags,
                selectedIds = activeTagIds,
                onSelectionChange = { selectedTags = it },
                contentPadding = PaddingValues(horizontal = AppTheme.spaceLarge),
                modifier = Modifier.padding(vertical = AppTheme.spaceSmall),
            )
            HorizontalDivider()
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AppTheme.spaceLarge),
        ) {
            item(key = "cards") {
                Row(
                    modifier = Modifier
                        .padding(bottom = AppTheme.space2XLarge)
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall),
                ) {
                    StatCard(
                        title = stringResource(R.string.statsGoalsActive),
                        value = "${active.size}",
                        icon = Icons.Filled.Flag,
                        color = MaterialTheme.colorScheme.primary,
                        style = StatCardStyle.COMPACT_VERTICAL,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                    StatCard(
                        title = stringResource(R.string.statsGoalsBehind),
                        value = "$behindCount",
                        icon = Icons.Filled.WarningAmber,
                        color = ErrorColor,
                        style = StatCardStyle.COMPACT_VERTICAL,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                    StatCard(
                        title = stringResource(R.string.statsGoalsCompleted),
                        value = "${completed.size}",
                        icon = Icons.Filled.CheckCircle,
                        color = SuccessColor,
                        style = StatCardStyle.COMPACT_VERTICAL,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
            if (paces.isNotEmpty()) {
                item(key = "activeHeader") {
                    SectionHeader(
                        stringResource(R.string.statsGoalsActive),
                        stringResource(R.string.statsGoalsPaceHint),
                    )
                    Spacer(Modifier.height(AppTheme.spaceSmall))
                }
                items(paces, key = { (item, _) -> item.goal.id }) { (item, pace) ->
                    ActiveGoalRow(item, pace, onClick = { onOpenGoal(item.goal.id) })
                    HorizontalDivider()
                }
                item(key = "activeSpace") { Spacer(Modifier.height(AppTheme.space2XLarge)) }
            }
            if (newestFirst.isNotEmpty()) {
                item(key = "completedHeader") {
                    SectionHeader(stringResource(R.string.statsGoalsCompleted))
                    Spacer(Modifier.height(AppTheme.spaceSmall))
                }
                items(newestFirst, key = { it.goal.id }) { item ->
                    CompletedGoalRow(item, zone, onClick = { onOpenGoal(item.goal.id) })
                    HorizontalDivider()
                }
            }
        }
    }
}

/** Whether a goal counts as behind on the page and in the overview's summary. */
internal val GoalPaceStatus.isBehind: Boolean
    get() = this == GoalPaceStatus.BEHIND || this == GoalPaceStatus.OVERDUE

@Composable
private fun ActiveGoalRow(item: GoalWithEntries, pace: GoalPace, onClick: () -> Unit) {
    val dateFormat = LocalDateFormatSettings.current.dateFormatCode
    val deadline = AppDateFormatUtils.formatDate(item.goal.targetDate, dateFormat)
    val finish = pace.projectedFinish?.let { AppDateFormatUtils.formatDate(it, dateFormat) }
    val overdue = pace.status == GoalPaceStatus.OVERDUE
    val detail = listOfNotNull(
        stringResource(if (overdue) R.string.statsGoalWasDue else R.string.statsGoalDue, deadline),
        finish?.let { stringResource(R.string.statsGoalProjected, it) },
    ).joinToString(" ")
    val status = paceLabel(pace.status)

    GoalRowFrame(onClick) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                item.goal.name,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(AppTheme.spaceSmall))
            Text(
                stringResource(
                    R.string.statsGoalProgress,
                    item.progress,
                    item.goal.targetValue,
                    item.goal.unitLabel,
                ),
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(AppTheme.spaceSmall))
        PaceBar(
            progress = pace.progressFraction,
            expected = pace.expectedFraction,
            color = Color(item.goal.colorValue),
        )
        Spacer(Modifier.height(AppTheme.spaceSmall))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(status) }
                append(" ")
                append(detail)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CompletedGoalRow(item: GoalWithEntries, zone: ZoneId, onClick: () -> Unit) {
    val dateFormat = LocalDateFormatSettings.current.dateFormatCode
    val finished = item.goal.completedAt?.let { AppDateFormatUtils.formatDate(it, dateFormat, zone) }.orEmpty()
    val days = GoalPace.daysEarly(item.goal, zone) ?: 0
    val count = abs(days).toInt()
    GoalRowFrame(onClick) {
        Text(item.goal.name, fontWeight = FontWeight.Bold)
        Text(
            when {
                days > 0 -> pluralStringResource(R.plurals.statsGoalFinishedEarly, count, finished, count)
                days < 0 -> pluralStringResource(R.plurals.statsGoalFinishedLate, count, finished, count)
                else -> stringResource(R.string.statsGoalFinishedOnDeadline, finished)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GoalRowFrame(onClick: () -> Unit, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = AppTheme.spaceMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) { content() }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Progress along a track, with a mark at where a steady pace would be today. */
@Composable
private fun PaceBar(progress: Float, expected: Float, color: Color) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val mark = MaterialTheme.colorScheme.onSurface
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp),
    ) {
        val barHeight = 8.dp.toPx()
        val top = (size.height - barHeight) / 2f
        val radius = CornerRadius(barHeight / 2f)
        drawRoundRect(track, Offset(0f, top), Size(size.width, barHeight), radius)
        drawRoundRect(color, Offset(0f, top), Size(size.width * progress, barHeight), radius)
        val markWidth = 2.dp.toPx()
        drawRect(
            mark,
            Offset((size.width * expected - markWidth / 2f).coerceIn(0f, size.width - markWidth), 0f),
            Size(markWidth, size.height),
        )
    }
}

@Composable
@ReadOnlyComposable
private fun paceLabel(status: GoalPaceStatus): String = stringResource(
    when (status) {
        GoalPaceStatus.AHEAD -> R.string.statsGoalAhead
        GoalPaceStatus.ON_PACE -> R.string.statsGoalOnPace
        GoalPaceStatus.BEHIND -> R.string.statsGoalBehind
        GoalPaceStatus.OVERDUE -> R.string.statsGoalOverdue
    }
)

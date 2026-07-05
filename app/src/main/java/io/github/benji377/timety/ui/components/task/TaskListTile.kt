package io.github.benji377.timety.ui.components.task

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.ui.components.common.ConfirmationDialog
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.ui.utils.AppUtils
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Mirrors `TaskListTile` in `task_list_tile.dart`.
 *
 * The Flutter [Task] model embeds its subtasks, whereas the Kotlin [TaskEntity] does
 * not (subtasks live in a separate Room table), so [subtasksCompleted]/[subtasksTotal]
 * are passed in explicitly by the caller instead of being read off the entity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListTile(
    task: TaskEntity,
    onToggleCompleted: () -> Unit,
    onTap: () -> Unit,
    onDelete: (() -> Unit)? = null,
    isOverdue: Boolean = false,
    enableDismissible: Boolean = true,
    showDescription: Boolean = true,
    showDueDate: Boolean = true,
    showTrailing: Boolean = true,
    subtasksCompleted: Int = 0,
    subtasksTotal: Int = 0,
    use24HourFormat: Boolean = LocalDateFormatSettings.current.use24HourFormat,
    dateFormatCode: String = LocalDateFormatSettings.current.dateFormatCode,
    modifier: Modifier = Modifier,
    // Screen margin, mirrors AppTheme.listTileScreenMargin (spaceLarge horizontal, spaceXSmall vertical).
    margin: PaddingValues = PaddingValues(
        horizontal = AppTheme.spaceLarge,
        vertical = AppTheme.spaceXSmall
    ),
) {
    val borderColor = getBorderColor(task, isOverdue)
    val hasSubtasks = subtasksTotal > 0
    val progress = if (hasSubtasks) subtasksCompleted.toFloat() / subtasksTotal.toFloat() else 0f

    val card = @Composable {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(margin)
                .clickable { onTap() },
            shape = AppTheme.brMedium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(AppTheme.listTileBorderWidth, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { onToggleCompleted() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = SuccessColor,
                            uncheckedColor = TaskColor,
                            checkmarkColor = Color.White,
                        ),
                    )
                },
                headlineContent = {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                            color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            fontWeight = AppTheme.fwBold,
                        ),
                    )
                },
                supportingContent = {
                    Column {
                        if (showDescription && task.description.isNotEmpty()) {
                            Text(
                                text = task.description,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = AppTheme.fsLabel,
                            )
                        }

                        if (hasSubtasks) {
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Checklist,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "$subtasksCompleted/$subtasksTotal",
                                    fontSize = AppTheme.fsLabel,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = AppTheme.fwMedium,
                                )
                                Spacer(Modifier.width(8.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (task.isCompleted) SuccessColor else borderColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                )
                            }
                        }

                        if (showDueDate && task.dueDate != null) {
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = borderColor,
                                )
                                Spacer(Modifier.width(4.dp))
                                // NOTE: Flutter uses settings.getFormattedDateTime (24h flag + locale
                                // aware). No centralized Kotlin equivalent exists yet, so this falls
                                // back to a locale-aware medium date/time formatter.
                                Text(
                                    text = formatDueDateTime(
                                        task.dueDate,
                                        use24HourFormat,
                                        dateFormatCode
                                    ),
                                    fontSize = AppTheme.fsLabel,
                                    color = borderColor,
                                    fontWeight = AppTheme.fwMedium,
                                )
                            }
                        }
                    }
                },
                trailingContent = if (showTrailing) {
                    {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = AppUtils.getSizeEmoji(task.size), fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            AppUtils.PriorityIcon(priority = task.priority)
                        }
                    }
                } else null,
            )
        }
    }

    if (!enableDismissible || onDelete == null) {
        Box(modifier = modifier) { card() }
        return
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                showDeleteDialog = true
            }
            false
        },
    )

    ConfirmationDialog(
        visible = showDeleteDialog,
        title = stringResource(R.string.taskDeleteTitle),
        content = stringResource(R.string.taskDeleteContent),
        confirmLabel = stringResource(R.string.commonLabelDelete),
        confirmColor = ErrorColor,
        onConfirm = {
            showDeleteDialog = false
            onDelete()
        },
        onDismiss = { showDeleteDialog = false },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        modifier = modifier,
        backgroundContent = {
            // Only visible mid-swipe: at rest the red would bleed through the card's
            // rounded corners.
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(margin)
                        .clip(AppTheme.brMedium)
                        .background(ErrorColor),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.commonLabelDelete),
                        tint = Color.White,
                        modifier = Modifier
                            .padding(end = AppTheme.spaceLarge)
                            .size(AppTheme.listTileSwipeIconSize),
                    )
                }
            }
        },
    ) {
        card()
    }
}

private fun getBorderColor(task: TaskEntity, isOverdue: Boolean): Color {
    if (task.isCompleted) return SuccessColor
    if (isOverdue) return ErrorColor
    val dueDate = task.dueDate
    if (dueDate != null) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        if (dueDate.atZone(zone).toLocalDate() == today) return WarningColor
    }
    return TaskColor
}

private fun formatDueDateTime(
    dueDate: Instant,
    use24HourFormat: Boolean,
    dateFormatCode: String
): String =
    AppDateFormatUtils.formatDateTime(dueDate, dateFormatCode, use24HourFormat)

package io.github.benji377.timety.ui.components.habit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.ui.components.common.SwipeToDeleteBox
import io.github.benji377.timety.ui.components.common.TimetyProgressBar
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.LocalSnackbarHostState
import io.github.benji377.timety.ui.theme.WifiOffColor
import io.github.benji377.timety.util.habit.HabitIcons
import kotlinx.coroutines.launch


/**
 * List row for a single habit, with a completion toggle, icon, name, notes, and optional progress
 * bar. Renders as a bordered card standalone, or as a slimmer row with a leading color bar when
 * [isStacked]. When [isLocked], the completion toggle is disabled and taps show a snackbar
 * instead. Swipe-to-delete is enabled by default when [onDelete] is provided.
 *
 * When [isReorderMode] is true, swipe-to-delete and tap/toggle are all suppressed and a drag
 * handle is shown instead, so the tile is purely a draggable row (dragging itself is handled by
 * the caller wrapping this tile, e.g. [DraggableReorderColumn]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitListTile(
    modifier: Modifier = Modifier,
    habit: HabitEntity,
    isCompleted: Boolean,
    subtitleText: String,
    progressValue: Float? = null,
    isStacked: Boolean = false,
    isLocked: Boolean = false,
    isReorderMode: Boolean = false,
    onToggleCompleted: () -> Unit,
    onTap: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onMarkPastCompletion: (() -> Unit)? = null,
    enableDismissible: Boolean = true,
    margin: PaddingValues = AppTheme.listTileScreenMargin,
) {
    val color = Color(habit.colorValue)
    val lockedMessage = stringResource(R.string.focusSnackbarHabitLocked)
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val tile: @Composable () -> Unit = {
        if (isStacked) {
            val barColor = if (isCompleted) color.copy(alpha = AppTheme.OPACITY_LIGHT) else color
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .background(if (isLocked) Color.Transparent else MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(barColor)
                )
                Box(modifier = Modifier.weight(1f)) {
                    HabitTileContent(
                        habit = habit,
                        color = color,
                        isCompleted = isCompleted,
                        isLocked = isLocked,
                        isReorderMode = isReorderMode,
                        subtitleText = subtitleText,
                        progressValue = progressValue,
                        onToggleCompleted = onToggleCompleted,
                        onTap = onTap,
                        onMarkPastCompletion = onMarkPastCompletion,
                        onLockedTap = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(lockedMessage)
                            }
                        },
                    )
                }
            }
        } else {
            val borderColor = if (isCompleted) color.copy(alpha = AppTheme.OPACITY_LIGHT) else color
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(margin),
                shape = AppTheme.brMedium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(AppTheme.listTileBorderWidth, borderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                HabitTileContent(
                    habit = habit,
                    color = color,
                    isCompleted = isCompleted,
                    isLocked = isLocked,
                    isReorderMode = isReorderMode,
                    subtitleText = subtitleText,
                    progressValue = progressValue,
                    onToggleCompleted = onToggleCompleted,
                    onTap = onTap,
                    onMarkPastCompletion = onMarkPastCompletion,
                    onLockedTap = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(lockedMessage)
                        }
                    },
                )
            }
        }
    }

    if (isReorderMode || !enableDismissible || onDelete == null) {
        Box(modifier = modifier) { tile() }
        return
    }

    SwipeToDeleteBox(
        dialogTitle = stringResource(R.string.habitDeleteTitle),
        dialogContent = stringResource(R.string.habitDeleteContent),
        confirmLabel = stringResource(R.string.commonLabelRemove),
        onDelete = onDelete,
        modifier = modifier,
        margin = margin,
    ) {
        tile()
    }
}

@Composable
private fun HabitTileContent(
    habit: HabitEntity,
    color: Color,
    isCompleted: Boolean,
    isLocked: Boolean,
    isReorderMode: Boolean,
    subtitleText: String,
    progressValue: Float?,
    onToggleCompleted: () -> Unit,
    onTap: () -> Unit,
    onMarkPastCompletion: (() -> Unit)?,
    onLockedTap: () -> Unit,
) {
    val habitIcon = HabitIcons.iconAt(habit.iconCodePoint)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isReorderMode) { onTap() }
            .padding(AppTheme.spaceMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Leading circular completion toggle.
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = if (isCompleted) color else MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = CircleShape,
                )
                .border(
                    width = 2.dp,
                    color = if (isCompleted) color else (if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant else HabitColor),
                    shape = CircleShape,
                )
                .clickable(enabled = !isReorderMode) { if (isLocked) onLockedTap() else onToggleCompleted() },
            contentAlignment = Alignment.Center,
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            } else if (isLocked) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.width(AppTheme.spaceMedium))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = habitIcon,
                    contentDescription = null,
                    modifier = Modifier.size(AppTheme.iconSizeSmall),
                    tint = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else (if (isLocked) WifiOffColor else color),
                )
                Spacer(modifier = Modifier.width(AppTheme.spaceSmall))
                Text(
                    text = habit.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = AppTheme.fwBold,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                    color = if (isCompleted || isLocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }

            if (!habit.notes.isNullOrEmpty()) {
                Text(
                    text = habit.notes,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = AppTheme.fsLabel,
                )
            }

            Text(
                text = subtitleText,
                fontSize = AppTheme.fsLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (progressValue != null && !isCompleted) {
                Spacer(modifier = Modifier.height(AppTheme.spaceXSmall))
                TimetyProgressBar(
                    progress = { progressValue.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = color,
                    trackColor = color.copy(alpha = AppTheme.OPACITY_VERY_LIGHT),
                )
            }
        }

        if (isReorderMode) {
            Icon(
                imageVector = Icons.Filled.DragHandle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (onMarkPastCompletion != null) {
            IconButton(onClick = onMarkPastCompletion) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = stringResource(R.string.habitMarkPastCompletionTooltip),
                )
            }
        }
    }
}

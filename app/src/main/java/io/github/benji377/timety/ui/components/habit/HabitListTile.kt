package io.github.benji377.timety.ui.components.habit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check

import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.HabitColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitListTile(
    habit: HabitEntity,
    isCompleted: Boolean,
    subtitleText: String,
    progressValue: Float? = null,
    isStacked: Boolean = false,
    isLocked: Boolean = false,
    onToggleCompleted: () -> Unit,
    onTap: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onMarkPastCompletion: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val color = Color(habit.colorValue)

    val content = @Composable {
        val listTileContent = @Composable {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular check button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCompleted) color else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable(enabled = !isLocked) {
                            onToggleCompleted()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(Icons.Filled.Check, contentDescription = "Completed", tint = Color.White, modifier = Modifier.size(22.dp))
                    } else if (isLocked) {
                        Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                    // Border handled by Box decoration in Flutter, doing it via Surface below:
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // TODO: Map iconCodePoint to actual Icons
                        Icon(
                            imageVector = Icons.Filled.Star, // Placeholder for habit.iconData
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else (if (isLocked) Color.Gray else color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = habit.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                                color = if (isCompleted || isLocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!habit.notes.isNullOrEmpty()) {
                        Text(
                            text = habit.notes,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = subtitleText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (progressValue != null && !isCompleted) {
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progressValue.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = color,
                            trackColor = color.copy(alpha = 0.15f)
                        )
                    }
                }

                if (onMarkPastCompletion != null) {
                    IconButton(onClick = onMarkPastCompletion) {
                        Icon(Icons.Filled.Schedule, contentDescription = "History")
                    }
                }
            }
        }

        if (isStacked) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .clickable { onTap() }
            ) {
                // Left border for stack
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(if (isCompleted) color.copy(alpha = 0.5f) else color)
                )
                listTileContent()
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { onTap() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(2.dp, if (isCompleted) color.copy(alpha = 0.5f) else color)
            ) {
                listTileContent()
            }
        }
    }

    if (onDelete != null) {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = {
                if (it == SwipeToDismissBoxValue.EndToStart) {
                    onDelete()
                    true
                } else false
            }
        )

        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = false,
            backgroundContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ErrorColor)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White
                    )
                }
            },
            modifier = modifier
        ) {
            content()
        }
    } else {
        Box(modifier = modifier) {
            content()
        }
    }
}

package io.github.benji377.timety.ui.components.habit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.ui.components.common.NeoListTile
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.util.habit.HabitUtils
import java.time.LocalDate


/**
 * Renders [habits] for [targetDate], grouping any that share a stack name into a collapsible card
 * with a completion count, and rendering the rest standalone. Each habit is rendered via
 * [habitBuilder], which also receives whether the habit is locked (a stacked habit becomes
 * unlockable only once the previous one in the stack is completed) and whether reorder mode is
 * active. While [isReorderMode] is true, both standalone habits and each stack's members become
 * drag-reorderable (via [DraggableReorderColumn]); [onStandaloneReordered]/[onStackReordered]
 * commit the result.
 */
@Composable
fun GroupedHabitsSection(
    habits: List<HabitWithCompletions>,
    allHabitsForStacks: List<HabitWithCompletions>,
    targetDate: LocalDate,
    isReorderMode: Boolean = false,
    onStandaloneReordered: (List<HabitWithCompletions>) -> Unit = {},
    onStackReordered: (stackName: String, newOrder: List<HabitWithCompletions>) -> Unit = { _, _ -> },
    habitBuilder: @Composable (habit: HabitWithCompletions, isDone: Boolean, isStacked: Boolean, isLocked: Boolean, isReorderMode: Boolean) -> Unit,
) {
    val grouped = habits.filter { !it.habit.stackName.isNullOrBlank() }
        .groupBy { it.habit.stackName!!.trim() }
    val standalone = habits.filter { it.habit.stackName.isNullOrBlank() }

    Column {
        grouped.forEach { (stackName, stackHabitsList) ->
            val sortedStackHabits = remember(stackHabitsList) {
                stackHabitsList.sortedBy { it.habit.stackOrder ?: 99 }
            }

            val globalStack = allHabitsForStacks.filter { it.habit.stackName?.trim() == stackName }
            val completedCount = HabitUtils.getStackCompletionCount(globalStack, targetDate)
            val allDone = HabitUtils.isStackFullyCompleted(globalStack, targetDate)

            var isExpanded by rememberSaveable(stackName) { mutableStateOf(false) }

            NeoListTile(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.paddingScreenHorizontal)
                    .padding(vertical = AppTheme.spaceSmall),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                    alpha = if (isExpanded) 0.1f else 0.4f,
                ),
                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded }
                            .padding(AppTheme.spaceMedium),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Layers,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(AppTheme.spaceSmall))
                        Text(
                            text = stackName.uppercase(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(AppTheme.spaceSmall))
                        Text(
                            text = "$completedCount / ${globalStack.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (allDone) SuccessColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column {
                            if (isReorderMode) {
                                // Reorder mode temporarily suspends the completion-lock semantics
                                // for editing, so there's no lock/divider bookkeeping to keep live
                                // during a drag.
                                DraggableReorderColumn(
                                    items = sortedStackHabits,
                                    key = { it.habit.id },
                                    isReorderEnabled = true,
                                    onOrderChanged = { onStackReordered(stackName, it) },
                                ) { hwc, _ ->
                                    val isDone = HabitUtils.isCompletedOn(hwc, targetDate)
                                    habitBuilder(hwc, isDone, true, false, true)
                                }
                            } else {
                                sortedStackHabits.forEachIndexed { index, hwc ->
                                    val isDone = HabitUtils.isCompletedOn(hwc, targetDate)
                                    var isLocked = false
                                    if (index > 0) {
                                        val prevHwc = sortedStackHabits[index - 1]
                                        val isPrevDone = HabitUtils.isCompletedOn(prevHwc, targetDate)
                                        isLocked = HabitUtils.isHabitLocked(
                                            index = index,
                                            isCurrentHabitDone = isDone,
                                            isPreviousHabitDone = isPrevDone,
                                        )
                                    }

                                    if (index > 0) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = AppTheme.spaceLarge),
                                        )
                                    }

                                    habitBuilder(hwc, isDone, true, isLocked, false)
                                }
                            }
                        }
                    }
                }
            }
        }

        DraggableReorderColumn(
            items = standalone.sortedBy { it.habit.sortOrder },
            key = { it.habit.id },
            isReorderEnabled = isReorderMode,
            onOrderChanged = onStandaloneReordered,
        ) { hwc, _ ->
            val isDone = HabitUtils.isCompletedOn(hwc, targetDate)
            habitBuilder(hwc, isDone, false, false, isReorderMode)
        }
    }
}

package io.github.benji377.timety.ui.components.habit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.util.habit.HabitUtils
import java.time.LocalDate

/**
 * Displays a grouped view of habits organized by stacks. Mirrors `GroupedHabitsSection`
 * in `widgets/habit/grouped_habits_section.dart`.
 *
 * [allHabitsForStacks] mirrors the Flutter widget's separate `habitProvider` reference:
 * stack totals/completion counts are computed against the FULL habit list (every member of
 * a stack, regardless of which section - due/upcoming/done - [habits] itself was filtered to),
 * not just the [habits] passed in for rendering.
 */
@Composable
fun GroupedHabitsSection(
    habits: List<HabitWithCompletions>,
    allHabitsForStacks: List<HabitWithCompletions>,
    targetDate: LocalDate,
    habitBuilder: @Composable (habit: HabitWithCompletions, isDone: Boolean, isStacked: Boolean, isLocked: Boolean) -> Unit,
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

            var isExpanded by remember(stackName) { mutableStateOf(!allDone) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.spaceLarge, vertical = AppTheme.spaceSmall),
                shape = AppTheme.brMedium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                        alpha = if (isExpanded) 0.1f else 0.4f,
                    ),
                ),
                border = BorderStroke(AppTheme.listTileBorderWidth, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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

                                habitBuilder(hwc, isDone, true, isLocked)
                            }
                        }
                    }
                }
            }
        }

        standalone.forEach { hwc ->
            val isDone = HabitUtils.isCompletedOn(hwc, targetDate)
            habitBuilder(hwc, isDone, false, false)
        }
    }
}

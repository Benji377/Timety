import 'package:flutter/material.dart';
import '../data/habit/habit_models.dart';
import '../providers/habit_provider.dart';
import '../theme/app_theme.dart';
import '../utils/habit_utils.dart';
import 'list_tiles/habit_list_tile.dart';

/// Displays a grouped view of habits organized by stacks
///
/// This widget handles:
/// - Grouping habits by stack name
/// - Displaying stacked habits in ExpansionTiles
/// - Showing standalone habits
/// - Lock logic for sequential habit completion
class GroupedHabitsSection extends StatelessWidget {
  final List<Habit> habits;
  final HabitProvider habitProvider;
  final DateTime targetDate;
  final Function(Habit) onHabitTap;
  final Function(Habit) onToggleCompleted;

  const GroupedHabitsSection({
    super.key,
    required this.habits,
    required this.habitProvider,
    required this.targetDate,
    required this.onHabitTap,
    required this.onToggleCompleted,
  });

  @override
  Widget build(BuildContext context) {
    final grouped = <String, List<Habit>>{};
    final standalone = <Habit>[];

    // Group habits by stack
    for (var h in habits) {
      if (h.stackName != null && h.stackName!.trim().isNotEmpty) {
        grouped.putIfAbsent(h.stackName!.trim(), () => []).add(h);
      } else {
        standalone.add(h);
      }
    }

    return Column(
      children: [
        // Render stacks
        ...grouped.entries.map((entry) {
          final stackName = entry.key;
          final stackHabits = entry.value;

          // Sort by stack order
          stackHabits.sort(
            (a, b) => (a.stackOrder ?? 99).compareTo(b.stackOrder ?? 99),
          );

          // Get completion info for the entire global stack
          final globalStack = habitProvider.habits
              .where((h) => h.stackName?.trim() == stackName)
              .toList();
          final completedCount = HabitUtils.getStackCompletionCount(
            stackHabits: globalStack,
            provider: habitProvider,
            date: targetDate,
          );
          final allDone = HabitUtils.isStackFullyCompleted(
            stackHabits: globalStack,
            provider: habitProvider,
            date: targetDate,
          );

          return Card(
            margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            elevation: 0,
            clipBehavior: Clip.antiAlias,
            shape: RoundedRectangleBorder(
              side: BorderSide(
                color: Theme.of(context).dividerColor,
                width: AppTheme.neoBorderWidth,
              ),
              borderRadius: AppTheme.brNeo,
            ),
            child: Theme(
              data: Theme.of(
                context,
              ).copyWith(dividerColor: Colors.transparent),
              child: ExpansionTile(
                initiallyExpanded:
                    !allDone, // Auto-collapse if the whole stack is finished
                backgroundColor: Theme.of(
                  context,
                ).colorScheme.surfaceContainerHighest.withValues(alpha: 0.1),
                title: Row(
                  children: [
                    Icon(
                      Icons.layers,
                      size: 14,
                      color: Theme.of(context).colorScheme.onSurfaceVariant,
                    ),
                    const SizedBox(width: 8),
                    Text(
                      stackName.toUpperCase(),
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 11,
                        letterSpacing: 1,
                      ),
                    ),
                    const Spacer(),
                    Text(
                      '$completedCount / ${globalStack.length}',
                      style: TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.bold,
                        color: allDone
                            ? AppTheme.successColor
                            : Theme.of(context).colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
                children: stackHabits.asMap().entries.map((entry) {
                  final index = entry.key;
                  final habit = entry.value;
                  final isDone = habitProvider.isCompletedOn(habit, targetDate);

                  // Determine lock state
                  bool isLocked = false;
                  if (index > 0) {
                    final prevHabit = stackHabits[index - 1];
                    final isPrevDone = habitProvider.isCompletedOn(
                      prevHabit,
                      targetDate,
                    );
                    isLocked = HabitUtils.isHabitLocked(
                      index: index,
                      isCurrentHabitDone: isDone,
                      isPreviousHabitDone: isPrevDone,
                    );
                  }

                  return Column(
                    children: [
                      if (index > 0) const Divider(height: 1, indent: 56),
                      HabitListTile(
                        habit: habit,
                        isCompleted: isDone,
                        isStacked: true,
                        isLocked: isLocked,
                        enableDismissible: false,
                        subtitleText: HabitUtils.buildHabitSubtitle(
                          habit,
                          habitProvider,
                        ),
                        onToggleCompleted: () => onToggleCompleted(habit),
                        onTap: () => onHabitTap(habit),
                      ),
                    ],
                  );
                }).toList(),
              ),
            ),
          );
        }),
        // Render standalone habits
        ...standalone.map((habit) {
          final isDone = habitProvider.isCompletedOn(habit, targetDate);
          return HabitListTile(
            habit: habit,
            isCompleted: isDone,
            enableDismissible: false,
            margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
            subtitleText: HabitUtils.buildHabitSubtitle(habit, habitProvider),
            onToggleCompleted: () => onToggleCompleted(habit),
            onTap: () => onHabitTap(habit),
          );
        }),
      ],
    );
  }
}

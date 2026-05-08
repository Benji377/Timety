import '../data/habit/habit_models.dart';
import '../providers/habit_provider.dart';

/// Utility functions for habit-related logic
class HabitUtils {
  /// Builds a subtitle description for a habit based on its frequency
  static String buildHabitSubtitle(Habit habit, HabitProvider provider) {
    if (habit.frequency == HabitFrequency.daily) return 'Daily';
    if (habit.frequency == HabitFrequency.weeklyExact) return 'Specific Days';
    final doneThisWeek = provider.getCompletionsThisWeek(habit);
    return '${doneThisWeek} / ${habit.targetDaysPerWeek} this week';
  }

  /// Determines if a habit in a stack is locked based on previous habit completion
  ///
  /// A habit is locked if:
  /// - It's not the first habit in the stack (index > 0)
  /// - The current habit is not completed
  /// - The previous habit is not completed
  static bool isHabitLocked({
    required int index,
    required bool isCurrentHabitDone,
    required bool isPreviousHabitDone,
  }) {
    return index > 0 && !isCurrentHabitDone && !isPreviousHabitDone;
  }

  /// Checks if all habits in a stack are completed for a given date
  static bool isStackFullyCompleted({
    required List<Habit> stackHabits,
    required HabitProvider provider,
    required DateTime date,
  }) {
    if (stackHabits.isEmpty) return false;
    return stackHabits.every((habit) => provider.isCompletedOn(habit, date));
  }

  /// Gets completion count for a stack on a specific date
  static int getStackCompletionCount({
    required List<Habit> stackHabits,
    required HabitProvider provider,
    required DateTime date,
  }) {
    return stackHabits.where((h) => provider.isCompletedOn(h, date)).length;
  }
}

import '../../data/habit/habit_models.dart';
import '../../providers/habit_provider.dart';
import '../../l10n/app_localizations.dart';
import '../date/date_utils.dart';

/// Utility functions for habit-related logic
class HabitUtils {
  /// Builds a subtitle description for a habit based on its frequency
  /// Does not include time formatting - handles that separately in UI layer
  static String buildHabitSubtitle(
    Habit habit,
    AppLocalizations l10n,
    int completionsThisWeek,
  ) {
    if (habit.frequency == HabitFrequency.daily) {
      return l10n.habitFreqDaily;
    }
    if (habit.frequency == HabitFrequency.weeklyExact) {
      final days =
          habit.targetWeekdays
              ?.map((d) => AppDateUtils.weekdayToStringShort(d))
              .join(', ') ??
          '';
      return l10n.habitFreqWeekly(days);
    }
    if (habit.frequency == HabitFrequency.weeklyFlexible) {
      final target = habit.targetDaysPerWeek ?? 0;
      return l10n.habitFreqFlexible(completionsThisWeek, target);
    }
    return '';
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

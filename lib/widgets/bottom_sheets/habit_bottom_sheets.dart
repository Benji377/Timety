import 'package:flutter/material.dart';
import '../../data/habit/habit_models.dart';

import '../habit/habit_history_sheet.dart';

/// Reusable bottom sheet builders for habit-related UIs
class HabitBottomSheetBuilders {
  /// Shows a unified calendar and history sheet for a habit.
  ///
  /// Displays statistics, an interactive monthly calendar, and a timeline.
  /// Days with completions have a purple dot. Tapping them allows modification.
  /// Tapping empty days asks for a time and adds a completion.
  static void showUnifiedHistorySheet({
    required BuildContext context,
    required Habit habit,
    required Function(DateTime date) onDateSelected,
    required Function(DateTime date) onDateDeselected,
  }) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) {
        return FractionallySizedBox(
          heightFactor: 0.90, // Slightly taller to comfortably fit the timeline
          child: HabitHistorySheet(
            habit: habit,
            onDateSelected: onDateSelected,
            onDateDeselected: onDateDeselected,
          ),
        );
      },
    );
  }
}


import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../data/habit/habit_models.dart';
import '../../providers/habit_provider.dart';
import '../../providers/user_provider.dart';
import '../../providers/settings_provider.dart';
import '../../theme/app_theme.dart';
import '../../utils/habit/habit_utils.dart';
import '../../widgets/common/expansion_section.dart';
import '../../widgets/habit/grouped_habits_section.dart';
import '../../widgets/habit/habit_bottom_sheet.dart';
import '../../widgets/list_tiles/habit_list_tile.dart';
import '../../l10n/app_localizations.dart';
import 'habit_detail_screen.dart';

/// Displays a categorized list of all habits (Due Today, Upcoming, Done).
class HabitListScreen extends StatelessWidget {
  const HabitListScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context)!.habitsListTitle),
      ),
      body: Consumer<HabitProvider>(
        builder: (context, provider, child) {
          if (provider.habits.isEmpty) {
            return Center(
              child: Text(AppLocalizations.of(context)!.habitScreenEmpty),
            );
          }

          final now = DateTime.now();
          final today = DateTime(now.year, now.month, now.day);
          final dueToday = <Habit>[];
          final todo = <Habit>[];
          final done = <Habit>[];

          for (final habit in provider.habits) {
            if (provider.isCompletedOn(habit, today) ||
                _isWeeklyGoalMet(habit, provider)) {
              done.add(habit);
            } else if (_isHabitDueToday(habit, provider, today)) {
              dueToday.add(habit);
            } else {
              todo.add(habit);
            }
          }

          // --- HABITS SCROLLABLE LIST ---
          return ListView(
            padding: const EdgeInsets.only(bottom: 80),
            children: [
              _buildHabitSection(
                context,
                AppLocalizations.of(context)!.commonTimeDueToday,
                AppTheme.warningColor,
                dueToday,
                provider,
              ),
              _buildHabitSection(
                context,
                AppLocalizations.of(context)!.commonTimeUpcoming,
                AppTheme.infoColor,
                todo,
                provider,
                initiallyExpanded: false,
              ),
              _buildHabitSection(
                context,
                AppLocalizations.of(context)!.commonTimeDone,
                AppTheme.successColor,
                done,
                provider,
                initiallyExpanded: false,
              ),
            ],
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        heroTag: 'habit_list_add_button',
        backgroundColor: AppTheme.habitColor,
        onPressed: () => Navigator.push(
          context,
          MaterialPageRoute(builder: (_) => const HabitDetailScreen()),
        ),
        child: const Icon(Icons.add),
      ),
    );
  }

  bool _isHabitDueToday(Habit habit, HabitProvider provider, DateTime today) {
    if (habit.frequency == HabitFrequency.daily) return true;

    if (habit.frequency == HabitFrequency.weeklyExact) {
      return habit.targetWeekdays?.contains(today.weekday) ?? false;
    }

    if (habit.frequency == HabitFrequency.weeklyFlexible) {
      final doneThisWeek = provider.getCompletionsThisWeek(
        habit,
        includeToday: false,
      );
      return doneThisWeek < (habit.targetDaysPerWeek ?? 1);
    }

    return false;
  }

  bool _isWeeklyGoalMet(Habit habit, HabitProvider provider) {
    if (habit.frequency != HabitFrequency.weeklyFlexible) return false;

    final doneThisWeek = provider.getCompletionsThisWeek(
      habit,
      includeToday: false,
    );
    return doneThisWeek >= (habit.targetDaysPerWeek ?? 1);
  }

  String _buildSubtitle(
    BuildContext context,
    Habit habit,
    HabitProvider provider,
  ) {
    final l10n = AppLocalizations.of(context)!;
    final settings = context.read<SettingsProvider>();
    final completionsThisWeek = provider.getCompletionsThisWeek(habit);
    var subtitle = HabitUtils.buildHabitSubtitle(
      habit,
      l10n,
      completionsThisWeek,
    );

    if (habit.targetTime != null) {
      final time = habit.targetTime!;
      subtitle += ' | ${settings.getFormattedTimeOfDay(time)}';
    }
    return subtitle;
  }

  Widget _buildHabitTile(
    BuildContext context,
    Habit habit,
    HabitProvider provider, {
    required bool isDone,
    bool isStacked = false,
    bool isLocked = false,
  }) {
    void openInteractiveCalendar() {
      HabitBottomSheetBuilders.showUnifiedHistorySheet(
        context: context,
        habit: habit,
        onDateSelected: (date) => provider.markCompletionOnDate(
          habit,
          date,
          userProvider: context.read<UserProvider>(),
        ),
        onDateDeselected: (date) => provider.unmarkCompletionOnDate(
          habit,
          date,
          userProvider: context.read<UserProvider>(),
        ),
      );
    }

    return HabitListTile(
      habit: habit,
      isCompleted: isDone,
      isStacked: isStacked,
      isLocked: isLocked,
      subtitleText: _buildSubtitle(context, habit, provider),
      progressValue: habit.frequency == HabitFrequency.weeklyFlexible && !isDone
          ? provider.getCompletionsThisWeek(habit) /
                (habit.targetDaysPerWeek ?? 1)
          : null,
      onToggleCompleted: () => provider.toggleCompletionToday(
        habit,
        userProvider: context.read<UserProvider>(),
      ),
      onMarkPastCompletion: openInteractiveCalendar,
      onDelete: () => provider.removeHabit(habit.id),
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(builder: (_) => HabitDetailScreen(habit: habit)),
        );
      },
    );
  }

  Widget _buildHabitSection(
    BuildContext context,
    String title,
    Color color,
    List<Habit> habits,
    HabitProvider provider, {
    bool initiallyExpanded = true,
  }) {
    if (habits.isEmpty) return const SizedBox.shrink();

    return ExpansionSection(
      title: '$title (${habits.length})',
      color: color,
      initiallyExpanded: initiallyExpanded,
      children: [
        GroupedHabitsSection(
          habits: habits,
          habitProvider: provider,
          targetDate: DateTime.now(),
          habitBuilder: (habit, isDone, isStacked, isLocked) {
            return _buildHabitTile(
              context,
              habit,
              provider,
              isDone: isDone,
              isStacked: isStacked,
              isLocked: isLocked,
            );
          },
        ),
      ],
    );
  }
}

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../screens/statistics_screen.dart';
import '../../data/habit/habit_models.dart';
import '../../providers/habit_provider.dart';
import '../../providers/user_provider.dart';
import '../../providers/settings_provider.dart';
import '../../theme/app_theme.dart';
import '../../utils/habit_utils.dart';
import '../../widgets/expansion_section.dart';
import '../../widgets/habit_bottom_sheet_builders.dart';
import '../../widgets/list_tiles/habit_list_tile.dart';
import '../calendar_screen.dart';
import '../../l10n/app_localizations.dart';
import 'habit_detail_screen.dart';

class HabitListScreen extends StatelessWidget {
  const HabitListScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context)!.habitsListTitle),
        actions: [
          IconButton(
            icon: const Icon(Icons.bar_chart),
            tooltip: AppLocalizations.of(context)!.commonTooltipStats,
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) =>
                      const StatisticsScreen(initialTabIndex: 3),
                ),
              );
            },
          ),
          IconButton(
            icon: const Icon(Icons.calendar_today),
            tooltip: AppLocalizations.of(context)!.commonTooltipCalendar,
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (context) => const CalendarScreen()),
              );
            },
          ),
        ],
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
      onDelete: () => provider.deleteHabit(habit.id),
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (_) => HabitDetailScreen(habit: habit, isEditing: true),
          ),
        );
      },
    );
  }

  List<Widget> _buildGroupedHabits(
    BuildContext context,
    List<Habit> habits,
    HabitProvider provider,
  ) {
    final grouped = <String, List<Habit>>{};
    final standalone = <Habit>[];
    final today = DateTime.now(); // Get today for global completion checks

    // Sort into stacks vs standalone
    for (var h in habits) {
      if (h.stackName != null && h.stackName!.trim().isNotEmpty) {
        grouped.putIfAbsent(h.stackName!.trim(), () => []).add(h);
      } else {
        standalone.add(h);
      }
    }

    final widgets = <Widget>[];

    grouped.forEach((stackName, stackHabits) {
      stackHabits.sort(
        (a, b) => (a.stackOrder ?? 99).compareTo(b.stackOrder ?? 99),
      );

      // --- Global Stack Calculations for the X/Y indicator ---
      final globalStack = provider.habits
          .where((h) => h.stackName?.trim() == stackName)
          .toList();
      final total = globalStack.length;
      final completed = globalStack
          .where((h) => provider.isCompletedOn(h, today))
          .length;
      final allDone = total > 0 && total == completed;

      widgets.add(
        Card(
          margin: AppTheme.listTileScreenMargin,
          elevation: 0,
          clipBehavior: Clip
              .antiAlias, // Ensures ExpansionTile doesn't bleed out of rounded corners
          shape: RoundedRectangleBorder(
            side: BorderSide(
              color: Theme.of(context).dividerColor.withValues(alpha: 0.5),
              width: AppTheme.listTileBorderWidth,
            ),
            borderRadius: AppTheme.brMedium,
          ),
          // Theme wrapper hides the ugly default borders of ExpansionTile
          child: Theme(
            data: Theme.of(context).copyWith(dividerColor: Colors.transparent),
            child: ExpansionTile(
              initiallyExpanded: true,
              backgroundColor: Theme.of(
                context,
              ).colorScheme.surfaceContainerHighest.withValues(alpha: 0.1),
              collapsedBackgroundColor: Theme.of(
                context,
              ).colorScheme.surfaceContainerHighest.withValues(alpha: 0.4),
              // --- THE HEADER ROW ---
              title: Row(
                children: [
                  const Icon(Icons.layers, size: 16, color: Colors.grey),
                  const SizedBox(width: 8),
                  Text(
                    stackName.toUpperCase(),
                    style: const TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 12,
                      letterSpacing: 1.2,
                    ),
                  ),
                  const Spacer(),
                  Text(
                    '$completed / $total',
                    style: TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 12,
                      color: allDone ? AppTheme.successColor : Colors.grey,
                    ),
                  ),
                ],
              ),
              // --- THE HABITS INSIDE ---
              children: stackHabits.asMap().entries.map((entry) {
                final index = entry.key;
                final habit = entry.value;
                final isDone = provider.isCompletedOn(habit, today);

                bool isLocked = false;
                if (index > 0 && !isDone) {
                  final previousHabit = stackHabits[index - 1];
                  if (!provider.isCompletedOn(previousHabit, today)) {
                    isLocked = true;
                  }
                }

                return Column(
                  children: [
                    if (index > 0) const Divider(height: 1, indent: 56),
                    _buildHabitTile(
                      context,
                      habit,
                      provider,
                      isDone: isDone,
                      isStacked: true,
                      isLocked: isLocked,
                    ),
                  ],
                );
              }).toList(),
            ),
          ),
        ),
      );
    });

    // Build the Standalone Habits
    widgets.addAll(
      standalone.map((h) {
        final isDone = provider.isCompletedOn(h, today);
        return _buildHabitTile(context, h, provider, isDone: isDone);
      }),
    );

    return widgets;
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
      children: _buildGroupedHabits(context, habits, provider),
    );
  }
}

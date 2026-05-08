import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:timety/screens/statistics_screen.dart';
import '../../data/habit/habit_models.dart';
import '../../providers/habit_provider.dart';
import '../../theme/app_theme.dart';
import '../../widgets/expansion_section.dart';
import '../../widgets/list_tiles/habit_list_tile.dart';
import '../calendar_screen.dart';
import 'habit_detail_screen.dart';

class HabitListScreen extends StatelessWidget {
  const HabitListScreen({super.key});

  String _buildSubtitle(
    BuildContext context,
    Habit habit,
    HabitProvider provider,
  ) {
    var subtitle = '';
    if (habit.frequency == HabitFrequency.daily) {
      subtitle = 'Daily';
    }
    if (habit.frequency == HabitFrequency.weeklyExact) {
      subtitle = 'Specific Days';
    }
    if (habit.frequency == HabitFrequency.weeklyFlexible) {
      final doneThisWeek = provider.getCompletionsThisWeek(habit);
      subtitle = '$doneThisWeek / ${habit.targetDaysPerWeek} this week';
    }
    if (habit.targetTime != null) {
      subtitle += ' • ${habit.targetTime!.format(context)}';
    }
    return subtitle;
  }

  Widget _buildHabitTile(
    BuildContext context,
    Habit habit,
    HabitProvider provider, {
    required bool isDone,
  }) {
    return HabitListTile(
      habit: habit,
      isCompleted: isDone,
      subtitleText: _buildSubtitle(context, habit, provider),
      progressValue: habit.frequency == HabitFrequency.weeklyFlexible && !isDone
          ? provider.getCompletionsThisWeek(habit) /
                (habit.targetDaysPerWeek ?? 1)
          : null,
      onToggleCompleted: () => provider.toggleCompletionToday(habit),
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

  Widget _buildHabitSection(
    BuildContext context,
    String title,
    Color color,
    List<Habit> habits,
    HabitProvider provider, {
    bool initiallyExpanded = true,
    required bool isDone,
  }) {
    return ExpansionSection(
      title: '$title (${habits.length})',
      color: color,
      initiallyExpanded: initiallyExpanded,
      children: habits
          .map(
            (habit) =>
                _buildHabitTile(context, habit, provider, isDone: isDone),
          )
          .toList(),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('My Habits'),
        actions: [
          IconButton(
            icon: const Icon(Icons.bar_chart),
            tooltip: 'Insights',
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) => const StatisticsScreen(),
                ),
              );
            },
          ),
          IconButton(
            icon: const Icon(Icons.calendar_today),
            tooltip: 'Calendar View',
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
            return const Center(
              child: Text("No habits yet! Tap + to build a routine."),
            );
          }

          final today = DateTime.now();
          final todoToday = <Habit>[];
          final doneToday = <Habit>[];
          final weeklyGoalsMet = <Habit>[];

          for (var habit in provider.habits) {
            final isDoneToday = provider.isCompletedOn(habit, today);

            if (isDoneToday) {
              doneToday.add(habit);
              continue;
            }

            if (habit.frequency == HabitFrequency.daily) {
              todoToday.add(habit);
            } else if (habit.frequency == HabitFrequency.weeklyExact) {
              if (habit.targetWeekdays?.contains(today.weekday) ?? false) {
                todoToday.add(habit);
              }
            } else if (habit.frequency == HabitFrequency.weeklyFlexible) {
              final doneThisWeek = provider.getCompletionsThisWeek(habit);
              if (doneThisWeek >= (habit.targetDaysPerWeek ?? 1)) {
                weeklyGoalsMet.add(habit);
              } else {
                todoToday.add(habit);
              }
            }
          }

          return ListView(
            padding: const EdgeInsets.only(bottom: 80),
            children: [
              _buildHabitSection(
                context,
                'To Do Today',
                AppTheme.infoColor,
                todoToday,
                provider,
                isDone: false,
              ),
              _buildHabitSection(
                context,
                'Done Today',
                AppTheme.successColor,
                doneToday,
                provider,
                isDone: true,
              ),
              _buildHabitSection(
                context,
                'Weekly Goal Met',
                AppTheme.warningColor,
                weeklyGoalsMet,
                provider,
                initiallyExpanded: false,
                isDone: false,
              ),
            ],
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        heroTag: 'habit_list_add_button',
        onPressed: () => Navigator.push(
          context,
          MaterialPageRoute(builder: (_) => const HabitDetailScreen()),
        ),
        child: const Icon(Icons.add),
      ),
    );
  }
}

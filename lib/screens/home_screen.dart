import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:timety/screens/settings_screen.dart';
import '../data/habit/habit_models.dart';
import '../providers/habit_provider.dart';
import '../data/task/task.dart';
import '../providers/task_provider.dart';
import '../providers/focus_provider.dart';
import '../providers/settings_provider.dart';
import '../theme/app_theme.dart';
import '../widgets/interactive_gauge.dart';
import '../widgets/list_tiles/task_list_tile.dart';
import '../widgets/list_tiles/habit_list_tile.dart';
import '../widgets/list_section_header.dart';
import 'task/task_detail_screen.dart';
import 'habit/habit_detail_screen.dart';

class HomeScreen extends StatelessWidget {
  final VoidCallback onNavigateToFocus;

  const HomeScreen({super.key, required this.onNavigateToFocus});

  // --- GREETING HELPER ---
  String _getGreeting(String name) {
    final now = DateTime.now();
    final hour = now.hour;
    final weekday = now.weekday;

    String greeting;
    if (hour < 12) {
      greeting = "Good Morning, $name!";
    } else if (hour < 17) {
      greeting = "Good Afternoon, $name!";
    } else {
      greeting = "Good Evening, $name!";
    }

    if (weekday == DateTime.monday && hour < 12) {
      return "$greeting Let's crush this week!";
    } else if (weekday == DateTime.friday && hour > 15) {
      return "$greeting The weekend is almost here!";
    } else if (weekday == DateTime.sunday) {
      return "$greeting Take it easy today!";
    }

    return greeting;
  }

  @override
  Widget build(BuildContext context) {
    final userName = context.watch<SettingsProvider>().userName;

    final focusProvider = context.watch<FocusProvider>();
    final taskProvider = context.watch<TaskProvider>();
    final habitProvider = context.watch<HabitProvider>();
    final settings = context.watch<SettingsProvider>();

    // Focus Calculations
    int focusMinsToday = focusProvider.getMinutesFocusedToday();
    int dailyTarget = settings.dailyGoalMins;
    double focusProgress = (focusMinsToday / dailyTarget).clamp(0.0, 1.0);

    // Task & Habit Calculations
    final today = DateTime.now();

    List<Task> urgentTasks = taskProvider.tasks.where((task) {
      if (task.isCompleted || task.dueDate == null) return false;
      final dueDay = DateTime(
        task.dueDate!.year,
        task.dueDate!.month,
        task.dueDate!.day,
      );
      return dueDay.isBefore(today) || dueDay.isAtSameMomentAs(today);
    }).toList();
    urgentTasks.sort((a, b) => a.dueDate!.compareTo(b.dueDate!));

    List<Habit> todaysHabits = habitProvider.getHabitsForDay(today);

    // Sort uncompleted habits to the top
    todaysHabits.sort((a, b) {
      bool aDone = habitProvider.isCompletedOn(a, today);
      bool bDone = habitProvider.isCompletedOn(b, today);
      if (aDone && !bDone) return 1;
      if (!aDone && bDone) return -1;
      return 0;
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text("Timety"),
        actions: [
          IconButton(
            icon: const Icon(Icons.settings),
            tooltip: 'Settings',
            onPressed: () => Navigator.push(
              context,
              MaterialPageRoute(builder: (context) => const SettingsScreen()),
            ),
          ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: [
            // --- TOP GREETING ---
            Padding(
              padding: const EdgeInsets.all(AppTheme.spaceXLarge),
              child: Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  _getGreeting(userName),
                  style: const TextStyle(
                    fontSize: AppTheme.fsHeadingLarge,
                    fontWeight: AppTheme.fwExtraBold,
                    letterSpacing: AppTheme.lsTight,
                  ),
                ),
              ),
            ),

            // --- TOP HALF: FOCUS GAUGE ---
            Expanded(
              flex: 5,
              child: Padding(
                padding: const EdgeInsets.all(AppTheme.spaceMedium),
                child: Center(
                  child: GestureDetector(
                    onTap: onNavigateToFocus,
                    child: InteractiveGauge(
                      progress: focusProgress,
                      isInteractive: false,
                      label: "DAILY GOAL",
                      centerText: "${(focusProgress * 100).toInt()}%",
                      bottomText: "$focusMinsToday / $dailyTarget m",
                      bottomTextColor: Theme.of(context).colorScheme.primary,
                    ),
                  ),
                ),
              ),
            ),

            const SizedBox(height: 16),
            const Divider(height: 1),

            // --- BOTTOM HALF: ACTION REQUIRED ---
            Expanded(
              flex: 6,
              child: Container(
                color: Theme.of(
                  context,
                ).colorScheme.surfaceContainerHighest.withValues(alpha: 0.3),
                child: (urgentTasks.isEmpty && todaysHabits.isEmpty)
                    ? Center(
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(
                              Icons.check_circle_outline,
                              size:
                                  AppTheme.iconSizeLarge +
                                  AppTheme.iconSizeLarge,
                              color: Colors.green.shade300,
                            ),
                            const SizedBox(height: AppTheme.spaceLarge),
                            const Text(
                              "You're all caught up for today!",
                              style: TextStyle(color: AppTheme.phaseRestColor),
                            ),
                          ],
                        ),
                      )
                    : ListView(
                        padding: const EdgeInsets.only(
                          top: AppTheme.spaceLarge,
                          bottom: 80,
                        ),
                        children: [
                          if (todaysHabits.isNotEmpty) ...[
                            const ListSectionHeader(
                              title: 'Habits Today',
                              icon: Icons.repeat,
                              color: AppTheme.typeHabitColor,
                            ),
                            ...todaysHabits.map(
                              (habit) => HabitListTile(
                                habit: habit,
                                isCompleted: habitProvider.isCompletedOn(
                                  habit,
                                  today,
                                ),
                                subtitleText: () {
                                  if (habit.frequency == HabitFrequency.daily) {
                                    return 'Daily';
                                  }
                                  if (habit.frequency ==
                                      HabitFrequency.weeklyExact) {
                                    return 'Specific Days';
                                  }
                                  final doneThisWeek = habitProvider
                                      .getCompletionsThisWeek(habit);
                                  return '$doneThisWeek / ${habit.targetDaysPerWeek} this week';
                                }(),
                                progressValue:
                                    habit.frequency ==
                                            HabitFrequency.weeklyFlexible &&
                                        !habitProvider.isCompletedOn(
                                          habit,
                                          today,
                                        )
                                    ? habitProvider.getCompletionsThisWeek(
                                            habit,
                                          ) /
                                          (habit.targetDaysPerWeek ?? 1)
                                    : null,
                                enableDismissible: false,
                                margin: const EdgeInsets.symmetric(
                                  horizontal: 16,
                                  vertical: 4,
                                ),
                                onToggleCompleted: () =>
                                    habitProvider.toggleCompletionToday(habit),
                                onTap: () {
                                  Navigator.push(
                                    context,
                                    MaterialPageRoute(
                                      builder: (_) => HabitDetailScreen(
                                        habit: habit,
                                        isEditing: true,
                                      ),
                                    ),
                                  );
                                },
                              ),
                            ),
                            const SizedBox(height: AppTheme.spaceLarge),
                          ],

                          if (urgentTasks.isNotEmpty) ...[
                            const ListSectionHeader(
                              title: 'Tasks Due',
                              icon: Icons.assignment_late,
                              color: AppTheme.warningColor,
                            ),
                            ...urgentTasks.map(
                              (task) => TaskListTile(
                                task: task,
                                enableDismissible: false,
                                margin: const EdgeInsets.symmetric(
                                  horizontal: 16,
                                  vertical: 4,
                                ),
                                showDescription: false,
                                onToggleCompleted: () => context
                                    .read<TaskProvider>()
                                    .toggleTask(task.id),
                                onTap: () {
                                  Navigator.push(
                                    context,
                                    MaterialPageRoute(
                                      builder: (_) => TaskDetailScreen(
                                        task: task,
                                        isEditing: false,
                                      ),
                                    ),
                                  );
                                },
                              ),
                            ),
                          ],
                        ],
                      ),
              ),
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        heroTag: "home_fab",
        onPressed: () => Navigator.push(
          context,
          MaterialPageRoute(builder: (context) => const TaskDetailScreen()),
        ),
        child: const Icon(Icons.add),
      ),
    );
  }
}

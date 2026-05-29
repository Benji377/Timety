import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:timety/providers/user_provider.dart';
import 'package:timety/screens/settings_screen.dart';
import 'package:timety/utils/greeting_utils.dart';
import '../data/habit/habit_models.dart';
import '../providers/habit_provider.dart';
import '../data/task/task.dart';
import '../providers/task_provider.dart';
import '../providers/focus_provider.dart';
import '../providers/settings_provider.dart';
import '../theme/app_theme.dart';
import '../widgets/interactive_gauge.dart';
import '../widgets/grouped_habits_section.dart';
import '../widgets/list_tiles/task_list_tile.dart';
import 'task/task_detail_screen.dart';
import 'habit/habit_detail_screen.dart';

class HomeScreen extends StatelessWidget {
  final VoidCallback onNavigateToFocus;

  const HomeScreen({super.key, required this.onNavigateToFocus});

  @override
  Widget build(BuildContext context) {
    final userName = context.watch<UserProvider>().name;
    final focusProvider = context.watch<FocusProvider>();
    final taskProvider = context.watch<TaskProvider>();
    final habitProvider = context.watch<HabitProvider>();
    final settings = context.watch<SettingsProvider>();

    final int focusMinsToday = focusProvider.getMinutesFocusedToday();
    final int dailyTarget = settings.dailyGoalMins;
    final double focusProgress = (focusMinsToday / dailyTarget).clamp(0.0, 1.0);
    final today = DateTime.now();
    final todayDate = DateTime(today.year, today.month, today.day);
    final List<Habit> todaysHabits = habitProvider.getHabitsForDay(today).where(
      (habit) {
        final completionsThisWeek = habitProvider.getCompletionsThisWeek(
          habit,
          includeToday: false,
        );
        final targetDays = habit.targetDaysPerWeek;

        return targetDays == null || completionsThisWeek < targetDays;
      },
    ).toList();

    // Urgent Tasks
    final List<Task> urgentTasks = taskProvider.tasks.where((task) {
      if (task.isCompleted || task.dueDate == null) return false;
      final dueDay = DateTime(
        task.dueDate!.year,
        task.dueDate!.month,
        task.dueDate!.day,
      );
      return dueDay.isBefore(today) || dueDay.isAtSameMomentAs(today);
    }).toList();
    urgentTasks.sort((a, b) => a.dueDate!.compareTo(b.dueDate!));

    final upcomingWindowDays = settings.upcomingTasksDays;
    final upcomingEndDate = todayDate.add(Duration(days: upcomingWindowDays));
    final List<Task> upcomingTasks = taskProvider.tasks.where((task) {
      if (task.isCompleted || task.dueDate == null) return false;

      final dueDay = DateTime(
        task.dueDate!.year,
        task.dueDate!.month,
        task.dueDate!.day,
      );

      return dueDay.isAfter(todayDate) &&
          (dueDay.isBefore(upcomingEndDate) ||
              dueDay.isAtSameMomentAs(upcomingEndDate));
    }).toList();
    upcomingTasks.sort((a, b) => a.dueDate!.compareTo(b.dueDate!));

    return Scaffold(
      appBar: AppBar(
        title: const Text("Timety"),
        actions: [
          IconButton(
            icon: const Icon(Icons.settings),
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
            Padding(
              padding: const EdgeInsets.all(AppTheme.spaceXLarge),
              child: Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  GreetingUtils.getGreeting(userName),
                  style: const TextStyle(
                    fontSize: AppTheme.fsHeadingLarge,
                    fontWeight: AppTheme.fwExtraBold,
                  ),
                ),
              ),
            ),
            Expanded(
              flex: 5,
              child: Center(
                child: GestureDetector(
                  onTap: onNavigateToFocus,
                  child: InteractiveGauge(
                    progress: focusProgress,
                    isInteractive: false,
                    label: "DAILY GOAL",
                    centerText: "${(focusProgress * 100).toInt()}%",
                    centerTextColor: AppTheme.focusColor,
                    color: AppTheme.focusColor,
                    bottomText: "$focusMinsToday / $dailyTarget m",
                    bottomTextColor: AppTheme.focusColor,
                  ),
                ),
              ),
            ),
            const SizedBox(height: 16),
            const Divider(height: 1),
            Expanded(
              flex: 6,
              child: Material(
                color: Theme.of(
                  context,
                ).colorScheme.surfaceContainerHighest.withValues(alpha: 0.3),
                child: (urgentTasks.isEmpty && todaysHabits.isEmpty)
                    ? const Center(
                        child: Text("You're all caught up for today!"),
                      )
                    : ListView(
                        padding: const EdgeInsets.only(
                          top: AppTheme.spaceLarge,
                          bottom: 80,
                        ),
                        children: [
                          if (urgentTasks.isNotEmpty) ...[
                            Padding(
                              padding: const EdgeInsets.only(
                                bottom: AppTheme.spaceMedium,
                              ),
                              child: Theme(
                                data: Theme.of(
                                  context,
                                ).copyWith(dividerColor: Colors.transparent),
                                child: ExpansionTile(
                                  initiallyExpanded: true,
                                  title: Text(
                                    'Tasks Due (${urgentTasks.length})',
                                    style: const TextStyle(
                                      fontWeight: AppTheme.fwBold,
                                      color: AppTheme.warningColor,
                                    ),
                                  ),
                                  iconColor: AppTheme.warningColor,
                                  collapsedIconColor: AppTheme.warningColor,
                                  children: [
                                    ...urgentTasks.map(
                                      (task) => TaskListTile(
                                        task: task,
                                        isOverdue:
                                            task.dueDate != null &&
                                            task.dueDate!.isBefore(today),
                                        enableDismissible: false,
                                        showDescription: false,
                                        onToggleCompleted: () => context
                                            .read<TaskProvider>()
                                            .toggleTask(
                                              task.id,
                                              userProvider: context
                                                  .read<UserProvider>(),
                                            ),
                                        onTap: () => Navigator.push(
                                          context,
                                          MaterialPageRoute(
                                            builder: (_) =>
                                                TaskDetailScreen(task: task),
                                          ),
                                        ),
                                      ),
                                    ),
                                    const SizedBox(height: AppTheme.spaceSmall),
                                  ],
                                ),
                              ),
                            ),
                          ],
                          if (todaysHabits.isNotEmpty) ...[
                            Padding(
                              padding: const EdgeInsets.only(
                                bottom: AppTheme.spaceMedium,
                              ),
                              child: Theme(
                                data: Theme.of(
                                  context,
                                ).copyWith(dividerColor: Colors.transparent),
                                child: ExpansionTile(
                                  title: Text(
                                    'Habits Today (${todaysHabits.length})',
                                    style: const TextStyle(
                                      fontWeight: AppTheme.fwBold,
                                      color: AppTheme.typeHabitColor,
                                    ),
                                  ),
                                  iconColor: AppTheme.typeHabitColor,
                                  collapsedIconColor: AppTheme.typeHabitColor,
                                  children: [
                                    GroupedHabitsSection(
                                      habits: todaysHabits,
                                      habitProvider: habitProvider,
                                      targetDate: today,
                                      onHabitTap: (habit) => Navigator.push(
                                        context,
                                        MaterialPageRoute(
                                          builder: (_) => HabitDetailScreen(
                                            habit: habit,
                                            isEditing: true,
                                          ),
                                        ),
                                      ),
                                      onToggleCompleted: (habit) =>
                                          habitProvider.toggleCompletionToday(
                                            habit,
                                            userProvider: context
                                                .read<UserProvider>(),
                                          ),
                                    ),
                                    const SizedBox(height: AppTheme.spaceSmall),
                                  ],
                                ),
                              ),
                            ),
                          ],
                          if (upcomingTasks.isNotEmpty) ...[
                            Padding(
                              padding: const EdgeInsets.only(
                                bottom: AppTheme.spaceMedium,
                              ),
                              child: Theme(
                                data: Theme.of(
                                  context,
                                ).copyWith(dividerColor: Colors.transparent),
                                child: ExpansionTile(
                                  title: Text(
                                    'Upcoming Tasks ($upcomingWindowDays days)',
                                    style: const TextStyle(
                                      fontWeight: AppTheme.fwBold,
                                      color: AppTheme.typeTaskColor,
                                    ),
                                  ),
                                  iconColor: AppTheme.typeTaskColor,
                                  collapsedIconColor: AppTheme.typeTaskColor,
                                  children: [
                                    ...upcomingTasks.map(
                                      (task) => TaskListTile(
                                        task: task,
                                        enableDismissible: false,
                                        showDescription: false,
                                        onToggleCompleted: () => context
                                            .read<TaskProvider>()
                                            .toggleTask(
                                              task.id,
                                              userProvider: context
                                                  .read<UserProvider>(),
                                            ),
                                        onTap: () => Navigator.push(
                                          context,
                                          MaterialPageRoute(
                                            builder: (_) =>
                                                TaskDetailScreen(task: task),
                                          ),
                                        ),
                                      ),
                                    ),
                                    const SizedBox(height: AppTheme.spaceSmall),
                                  ],
                                ),
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

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

  // --- SUBTITLE HELPER ---
  String _buildHabitSubtitle(
    BuildContext context,
    Habit habit,
    HabitProvider provider,
  ) {
    if (habit.frequency == HabitFrequency.daily) return 'Daily';
    if (habit.frequency == HabitFrequency.weeklyExact) return 'Specific Days';
    final doneThisWeek = provider.getCompletionsThisWeek(habit);
    return '$doneThisWeek / ${habit.targetDaysPerWeek} this week';
  }

  // --- NEW: GROUPED HABITS BUILDER FOR HOME ---
  List<Widget> _buildGroupedHabits(
    BuildContext context,
    List<Habit> habits,
    HabitProvider provider,
    DateTime today,
  ) {
    final grouped = <String, List<Habit>>{};
    final standalone = <Habit>[];

    for (var h in habits) {
      if (h.stackName != null && h.stackName!.trim().isNotEmpty) {
        grouped.putIfAbsent(h.stackName!.trim(), () => []).add(h);
      } else {
        standalone.add(h);
      }
    }

    final widgets = <Widget>[];

    // 1. Stacks
    grouped.forEach((stackName, stackHabits) {
      stackHabits.sort(
        (a, b) => (a.stackOrder ?? 99).compareTo(b.stackOrder ?? 99),
      );

      final globalStack = provider.habits
          .where((h) => h.stackName?.trim() == stackName)
          .toList();
      final total = globalStack.length;
      final completedCount = globalStack
          .where((h) => provider.isCompletedOn(h, today))
          .length;
      final allDone = total > 0 && total == completedCount;

      widgets.add(
        Card(
          margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          elevation: 0,
          clipBehavior: Clip.antiAlias,
          shape: RoundedRectangleBorder(
            side: BorderSide(
              color: Theme.of(context).dividerColor.withValues(alpha: 0.3),
              width: 1,
            ),
            borderRadius: AppTheme.brMedium,
          ),
          child: Theme(
            data: Theme.of(context).copyWith(dividerColor: Colors.transparent),
            child: ExpansionTile(
              initiallyExpanded:
                  !allDone, // Auto-collapse if the whole stack is finished
              backgroundColor: Theme.of(
                context,
              ).colorScheme.surfaceContainerHighest.withValues(alpha: 0.1),
              title: Row(
                children: [
                  const Icon(Icons.layers, size: 14, color: Colors.grey),
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
                    '$completedCount / $total',
                    style: TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.bold,
                      color: allDone ? Colors.green : Colors.grey,
                    ),
                  ),
                ],
              ),
              children: stackHabits.asMap().entries.map((entry) {
                final index = entry.key;
                final habit = entry.value;
                final isDone = provider.isCompletedOn(habit, today);

                // Locking Logic
                bool isLocked = false;
                if (index > 0 && !isDone) {
                  final prev = stackHabits[index - 1];
                  if (!provider.isCompletedOn(prev, today)) isLocked = true;
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
                      subtitleText: _buildHabitSubtitle(
                        context,
                        habit,
                        provider,
                      ),
                      onToggleCompleted: () =>
                          provider.toggleCompletionToday(habit),
                      onTap: () => Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (_) =>
                              HabitDetailScreen(habit: habit, isEditing: true),
                        ),
                      ),
                    ),
                  ],
                );
              }).toList(),
            ),
          ),
        ),
      );
    });

    // 2. Standalone
    widgets.addAll(
      standalone.map((habit) {
        final isDone = provider.isCompletedOn(habit, today);
        return HabitListTile(
          habit: habit,
          isCompleted: isDone,
          enableDismissible: false,
          margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
          subtitleText: _buildHabitSubtitle(context, habit, provider),
          onToggleCompleted: () => provider.toggleCompletionToday(habit),
          onTap: () => Navigator.push(
            context,
            MaterialPageRoute(
              builder: (_) => HabitDetailScreen(habit: habit, isEditing: true),
            ),
          ),
        );
      }),
    );

    return widgets;
  }

  @override
  Widget build(BuildContext context) {
    final userName = context.watch<SettingsProvider>().userName;
    final focusProvider = context.watch<FocusProvider>();
    final taskProvider = context.watch<TaskProvider>();
    final habitProvider = context.watch<HabitProvider>();
    final settings = context.watch<SettingsProvider>();

    int focusMinsToday = focusProvider.getMinutesFocusedToday();
    int dailyTarget = settings.dailyGoalMins;
    double focusProgress = (focusMinsToday / dailyTarget).clamp(0.0, 1.0);
    final today = DateTime.now();

    // Urgent Tasks
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

    // Habits for Today
    List<Habit> todaysHabits = habitProvider.getHabitsForDay(today);

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
                  _getGreeting(userName),
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
                    bottomText: "$focusMinsToday / $dailyTarget m",
                    bottomTextColor: Theme.of(context).colorScheme.primary,
                  ),
                ),
              ),
            ),
            const SizedBox(height: 16),
            const Divider(height: 1),
            Expanded(
              flex: 6,
              child: Container(
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
                          if (todaysHabits.isNotEmpty) ...[
                            const ListSectionHeader(
                              title: 'Habits Today',
                              icon: Icons.repeat,
                              color: AppTheme.typeHabitColor,
                            ),
                            // --- USE THE NEW GROUPED BUILDER ---
                            ..._buildGroupedHabits(
                              context,
                              todaysHabits,
                              habitProvider,
                              today,
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
                                onTap: () => Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                    builder: (_) => TaskDetailScreen(
                                      task: task,
                                      isEditing: false,
                                    ),
                                  ),
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

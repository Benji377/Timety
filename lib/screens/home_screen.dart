import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:timety/screens/settings_screen.dart';
import '../data/habit/habit_models.dart';
import '../providers/habit_provider.dart';
import '../theme/app_theme.dart';
import '../data/task/task.dart';
import '../providers/task_provider.dart';
import '../providers/focus_provider.dart';
import '../providers/settings_provider.dart';
import '../utils/utils.dart';
import '../widgets/interactive_gauge.dart';
import 'task/task_detail_screen.dart';

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

  // --- TASK UI HELPERS ---
  Color _getTaskBorderColor(Task task) {
    if (task.isCompleted) return Colors.green;
    if (task.dueDate != null) {
      final now = DateTime.now();
      final today = DateTime(now.year, now.month, now.day);
      final dueDay = DateTime(
        task.dueDate!.year,
        task.dueDate!.month,
        task.dueDate!.day,
      );
      if (dueDay.isBefore(today)) return Colors.red;
      if (dueDay.isAtSameMomentAs(today)) return Colors.amber.shade600;
    }
    return Colors.blue;
  }

  Widget _buildTaskTile(BuildContext context, Task task) {
    final borderColor = _getTaskBorderColor(task);

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      elevation: 0,
      shape: RoundedRectangleBorder(
        side: BorderSide(color: borderColor, width: 2),
        borderRadius: AppTheme.brMedium,
      ),
      child: ListTile(
        leading: Checkbox(
          value: task.isCompleted,
          activeColor: Colors.green,
          onChanged: (_) => context.read<TaskProvider>().toggleTask(task.id),
        ),
        title: Text(
          task.title,
          style: TextStyle(
            decoration: task.isCompleted ? TextDecoration.lineThrough : null,
            color: task.isCompleted ? Colors.grey : null,
            fontWeight: FontWeight.bold,
          ),
        ),
        subtitle: task.dueDate != null
            ? Padding(
                padding: const EdgeInsets.only(top: 4.0),
                child: Row(
                  children: [
                    Icon(Icons.access_time, size: 14, color: borderColor),
                    const SizedBox(width: 4),
                    Text(
                      "${task.dueDate!.hour.toString().padLeft(2, '0')}:${task.dueDate!.minute.toString().padLeft(2, '0')}",
                      style: TextStyle(
                        fontSize: 12,
                        color: borderColor,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ],
                ),
              )
            : null,
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              AppUtils().getSizeEmoji(task.size),
              style: const TextStyle(fontSize: 18),
            ),
            const SizedBox(width: 8),
            AppUtils().getPriorityIcon(task.priority),
          ],
        ),
        onTap: () {
          Navigator.push(
            context,
            MaterialPageRoute(
              builder: (_) => TaskDetailScreen(task: task, isEditing: false),
            ),
          );
        },
      ),
    );
  }

  // --- HABIT UI HELPER ---
  Widget _buildHabitTile(BuildContext context, Habit habit) {
    final provider = context.read<HabitProvider>();
    final isCompleted = provider.isCompletedOn(habit, DateTime.now());
    final color = Color(habit.colorValue);

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      elevation: 0,
      shape: RoundedRectangleBorder(
        side: BorderSide(
          color: isCompleted ? color.withValues(alpha: 0.3) : color,
          width: 2,
        ),
        borderRadius: AppTheme.brMedium,
      ),
      child: ListTile(
        leading: Checkbox(
          value: isCompleted,
          activeColor: color,
          onChanged: (_) => provider.toggleCompletionToday(habit),
        ),
        title: Row(
          children: [
            Icon(
              habit.iconData ?? Icons.circle,
              size: 18,
              color: isCompleted ? Colors.grey : color,
            ),
            const SizedBox(width: AppTheme.spaceSmall),
            Expanded(
              child: Text(
                habit.name,
                style: TextStyle(
                  decoration: isCompleted ? TextDecoration.lineThrough : null,
                  color: isCompleted ? Colors.grey : null,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          ],
        ),
        subtitle: habit.targetTime != null
            ? Row(
                children: [
                  const Icon(Icons.access_time, size: 14, color: Colors.grey),
                  const SizedBox(width: 4),
                  Text(
                    habit.targetTime!.format(context),
                    style: const TextStyle(fontSize: 12, color: Colors.grey),
                  ),
                ],
              )
            : null,
      ),
    );
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
              padding: const EdgeInsets.all(24.0),
              child: Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  _getGreeting(userName),
                  style: const TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.w900,
                    letterSpacing: -0.5,
                  ),
                ),
              ),
            ),

            // --- TOP HALF: FOCUS GAUGE ---
            Expanded(
              flex: 5,
              child: Padding(
                padding: const EdgeInsets.all(15.0),
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
                              size: 48,
                              color: Colors.green.shade300,
                            ),
                            const SizedBox(height: 16),
                            const Text(
                              "You're all caught up for today!",
                              style: TextStyle(color: Colors.grey),
                            ),
                          ],
                        ),
                      )
                    : ListView(
                        padding: const EdgeInsets.only(top: 16, bottom: 80),
                        children: [
                          if (todaysHabits.isNotEmpty) ...[
                            Padding(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 16.0,
                                vertical: 8.0,
                              ),
                              child: Row(
                                children: [
                                  const Icon(
                                    Icons.repeat,
                                    color: Colors.purple,
                                  ),
                                  const SizedBox(width: 8),
                                  Text(
                                    "Habits Today",
                                    style: TextStyle(
                                      fontSize: 16,
                                      fontWeight: FontWeight.bold,
                                      color: Colors.purple.shade700,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            ...todaysHabits.map(
                              (h) => _buildHabitTile(context, h),
                            ),
                            const SizedBox(height: 16),
                          ],

                          if (urgentTasks.isNotEmpty) ...[
                            Padding(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 16.0,
                                vertical: 8.0,
                              ),
                              child: Row(
                                children: [
                                  Icon(
                                    Icons.assignment_late,
                                    color: Colors.amber.shade700,
                                  ),
                                  const SizedBox(width: 8),
                                  Text(
                                    "Tasks Due",
                                    style: TextStyle(
                                      fontSize: 16,
                                      fontWeight: FontWeight.bold,
                                      color: Colors.amber.shade700,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            ...urgentTasks.map(
                              (t) => _buildTaskTile(context, t),
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

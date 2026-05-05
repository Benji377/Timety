import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:timety/screens/settings_screen.dart';

import '../data/task/task.dart';
import '../providers/task_provider.dart';
import '../providers/focus_provider.dart';
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
      return "$greeting Let's crush this week! 🚀";
    } else if (weekday == DateTime.friday && hour > 15) {
      return "$greeting The weekend is almost here! 🎉";
    } else if (weekday == DateTime.sunday) {
      return "$greeting Take it easy today! ☕";
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
        borderRadius: BorderRadius.circular(8),
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

  @override
  Widget build(BuildContext context) {
    final userName = "Bobert"; // Hardcoded for now

    // Providers
    final focusProvider = context.watch<FocusProvider>();
    final taskProvider = context.watch<TaskProvider>();

    // Focus Calculations
    int focusMinsToday = focusProvider.getMinutesFocusedToday();
    int dailyTarget = focusProvider.dailyTargetMinutes;
    double focusProgress = (focusMinsToday / dailyTarget).clamp(0.0, 1.0);

    // Task Calculations (Due Today + Overdue)
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);

    List<Task> urgentTasks = taskProvider.tasks.where((task) {
      if (task.isCompleted || task.dueDate == null) return false;
      final dueDay = DateTime(
        task.dueDate!.year,
        task.dueDate!.month,
        task.dueDate!.day,
      );
      return dueDay.isBefore(today) || dueDay.isAtSameMomentAs(today);
    }).toList();

    // Sort: Overdue first, then by time
    urgentTasks.sort((a, b) => a.dueDate!.compareTo(b.dueDate!));

    return Scaffold(
      appBar: AppBar(
        title: const Text("Timety"),
        actions: [
          IconButton(
            icon: const Icon(Icons.settings),
            tooltip: 'Settings',
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (context) => const SettingsScreen()),
              );
            },
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
              child: Center(
                child: GestureDetector(
                  onTap: onNavigateToFocus,
                  child: InteractiveGauge(
                    progress: focusProgress,
                    isInteractive: false, // Read-only on the home screen
                    label: "DAILY GOAL",
                    centerText: "${(focusProgress * 100).toInt()}%",
                    bottomText: "$focusMinsToday / $dailyTarget m",
                    bottomTextColor: Theme.of(context).colorScheme.primary,
                  ),
                ),
              ),
            ),

            const Divider(height: 1),

            // --- BOTTOM HALF: URGENT TASKS ---
            Expanded(
              flex: 5,
              child: Container(
                color: Theme.of(
                  context,
                ).colorScheme.surfaceContainerHighest.withValues(alpha: 0.3),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (urgentTasks.isNotEmpty)
                      Padding(
                        padding: const EdgeInsets.all(16.0),
                        child: Row(
                          children: [
                            Icon(
                              Icons.assignment_late,
                              color: Colors.amber.shade700,
                            ),
                            const SizedBox(width: 8),
                            const Text(
                              "Action Required Today",
                              style: TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                      ),
                    Expanded(
                      child: urgentTasks.isEmpty
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
                          : ListView.builder(
                              itemCount: urgentTasks.length,
                              itemBuilder: (context, index) {
                                return _buildTaskTile(
                                  context,
                                  urgentTasks[index],
                                );
                              },
                            ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
      // FAB to quickly jump into a new task from the home screen
      floatingActionButton: FloatingActionButton(
        heroTag: "home_fab",
        onPressed: () {
          Navigator.push(
            context,
            MaterialPageRoute(builder: (context) => const TaskDetailScreen()),
          );
        },
        child: const Icon(Icons.add),
      ),
    );
  }
}

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../providers/task_provider.dart';
import '../providers/user_provider.dart';
import '../data/task.dart';
import 'add_task_screen.dart';
import 'focus_screen.dart';

class TaskDetailScreen extends StatelessWidget {
  final int taskId;
  const TaskDetailScreen({super.key, required this.taskId});

  @override
  Widget build(BuildContext context) {
    final taskProvider = context.watch<TaskProvider>();
    final task = taskProvider.allTasks.firstWhere((t) => t.id == taskId);

    final dueDate = task.dueDateTime;
    final dateStr = dueDate != null
        ? DateFormat('EEEE, MMM d, yyyy').format(dueDate)
        : 'No due date';
    final timeStr = task.dueTimeDateTime != null
        ? DateFormat('h:mm a').format(task.dueTimeDateTime!)
        : 'No time set';

    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            // Header with close button
            Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(
                    child: Text(
                      task.title,
                      style: Theme.of(context).textTheme.headlineSmall,
                      overflow: TextOverflow.ellipsis,
                      maxLines: 2,
                    ),
                  ),
                  IconButton(
                    onPressed: Navigator.of(context).pop,
                    icon: const Icon(Icons.close),
                  ),
                ],
              ),
            ),

            // Content
            Expanded(
              child: ListView(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                children: [
                  // Status checkbox
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(12),
                      child: CheckboxListTile(
                        title: const Text('Mark as complete'),
                        value: task.status == TaskStatus.done,
                        onChanged: (_) async {
                          final newStatus = task.status == TaskStatus.done
                              ? TaskStatus.todo
                              : TaskStatus.done;
                          taskProvider.updateTaskStatus(
                            taskId,
                            newStatus,
                            onXpGain: (xp) =>
                                context.read<UserProvider>().addXp(xp),
                          );

                          // Update streak if task is being completed
                          if (newStatus == TaskStatus.done) {
                            await context
                                .read<UserProvider>()
                                .checkAndUpdateStreak(
                                  todayFocusMinutes: 0,
                                  completedTaskToday: true,
                                );
                          }
                        },
                      ),
                    ),
                  ),
                  const SizedBox(height: 24),

                  // Description
                  if (task.description != null &&
                      task.description!.isNotEmpty) ...[
                    Text(
                      'Description',
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    const SizedBox(height: 8),
                    Card(
                      child: Padding(
                        padding: const EdgeInsets.all(12),
                        child: Text(
                          task.description!,
                          style: Theme.of(context).textTheme.bodyMedium,
                        ),
                      ),
                    ),
                    const SizedBox(height: 24),
                  ],

                  // Details
                  Text(
                    'Details',
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const SizedBox(height: 12),
                  _buildInfoRow(
                    context,
                    Icons.calendar_today,
                    'Due Date',
                    dateStr,
                  ),
                  _buildInfoRow(context, Icons.schedule, 'Due Time', timeStr),
                  _buildInfoRow(
                    context,
                    task.priority.icon,
                    'Priority',
                    task.priority.label,
                  ),
                  _buildInfoRow(
                    context,
                    task.size.icon,
                    'Size',
                    task.size.label,
                  ),

                  // Location
                  if (task.location != null && task.location!.isNotEmpty)
                    _buildInfoRow(
                      context,
                      Icons.location_on,
                      'Location',
                      task.location!,
                    ),

                  // Reminders
                  if (task.reminders.isNotEmpty) ...[
                    const SizedBox(height: 16),
                    Text(
                      'Reminders',
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    const SizedBox(height: 8),
                    ...task.reminderDateTimes.map(
                      (reminder) => Padding(
                        padding: const EdgeInsets.only(bottom: 8),
                        child: Chip(
                          label: Text(
                            DateFormat('MMM d, h:mm a').format(reminder),
                          ),
                          avatar: const Icon(Icons.notifications, size: 18),
                        ),
                      ),
                    ),
                  ],
                  const SizedBox(height: 24),
                ],
              ),
            ),

            // Action buttons
            Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                children: [
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: () =>
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (_) => AddTaskScreen(initialTask: task),
                            ),
                          ).then((_) {
                            // Refresh after edit
                            if (context.mounted) {
                              context.read<TaskProvider>().refreshAll();
                            }
                          }),
                      icon: const Icon(Icons.edit),
                      label: const Text('Edit'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: ElevatedButton.icon(
                      onPressed: () => Navigator.push(
                        context,
                        MaterialPageRoute(builder: (_) => const FocusScreen()),
                      ),
                      icon: const Icon(Icons.timer),
                      label: const Text('Focus'),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildInfoRow(
    BuildContext context,
    IconData icon,
    String label,
    String value,
  ) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Row(
        children: [
          Icon(icon, size: 20, color: Theme.of(context).colorScheme.primary),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label, style: Theme.of(context).textTheme.labelSmall),
                Text(value, style: Theme.of(context).textTheme.bodyLarge),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

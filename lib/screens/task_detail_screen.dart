import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../providers/task_provider.dart';
import '../providers/user_provider.dart';
import '../data/task.dart';

class TaskDetailScreen extends StatelessWidget {
  final int taskId;
  const TaskDetailScreen({super.key, required this.taskId});

  @override
  Widget build(BuildContext context) {
    final taskProvider = context.watch<TaskProvider>();
    final task = taskProvider.allTasks.firstWhere((t) => t.id == taskId);

    final dueDate = task.dueDate != null ? DateTime.fromMillisecondsSinceEpoch(task.dueDate!) : null;
    final dateStr = dueDate != null ? DateFormat('EEEE, MMM d, yyyy').format(dueDate) : 'No due date';

    return Scaffold(
      appBar: AppBar(
        title: const Text('Task Details'),
        actions: [
          IconButton(
            icon: const Icon(Icons.delete),
            onPressed: () => _confirmDelete(context, taskProvider),
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Row(
            children: [
              Checkbox(
                value: task.status == TaskStatus.done,
                onChanged: (_) {
                  final newStatus = task.status == TaskStatus.done ? TaskStatus.todo : TaskStatus.done;
                  taskProvider.updateTaskStatus(taskId, newStatus, onXpGain: (xp) => context.read<UserProvider>().addXp(xp));
                },
              ),
              Expanded(
                child: Text(
                  task.title,
                  style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    decoration: task.status == TaskStatus.done ? TextDecoration.lineThrough : null,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          if (task.description != null) ...[
            Text('Description', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 8),
            Text(task.description!, style: Theme.of(context).textTheme.bodyLarge),
            const SizedBox(height: 24),
          ],
          _buildInfoRow(context, Icons.calendar_today, 'Due Date', dateStr),
          _buildInfoRow(context, task.priority.icon, 'Priority', task.priority.label),
          _buildInfoRow(context, task.size.icon, 'Size', task.size.label),
          if (task.location != null)
            _buildInfoRow(context, Icons.location_on, 'Location', task.location!),
        ],
      ),
    );
  }

  Widget _buildInfoRow(BuildContext context, IconData icon, String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Row(
        children: [
          Icon(icon, size: 20, color: Theme.of(context).colorScheme.primary),
          const SizedBox(width: 16),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label, style: Theme.of(context).textTheme.labelSmall),
              Text(value, style: Theme.of(context).textTheme.bodyLarge),
            ],
          ),
        ],
      ),
    );
  }

  void _confirmDelete(BuildContext context, TaskProvider provider) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete Task?'),
        content: const Text('Are you sure you want to delete this task? This action cannot be undone.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('CANCEL')),
          TextButton(
            onPressed: () {
              provider.deleteTask(taskId);
              Navigator.pop(context); // Close dialog
              Navigator.pop(context); // Close detail screen
            },
            child: const Text('DELETE', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
  }
}

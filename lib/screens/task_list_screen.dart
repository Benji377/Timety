import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../data/task.dart';
import '../utils/utils.dart';
import '../providers/task_provider.dart';
import '../widgets/app_dialogs.dart';
import 'add_task_screen.dart';
import 'task_detail_screen.dart';

class TodoListScreen extends StatefulWidget {
  const TodoListScreen({super.key});

  @override
  State<TodoListScreen> createState() => _TodoListScreenState();
}

class _TodoListScreenState extends State<TodoListScreen> {
  @override
  void initState() {
    super.initState();
    // Use a post-frame callback. This ensures the widget is fully
    // built before we try to access the provider.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return; // The safety check
      // Load tasks when the screen initializes
      context.read<TaskProvider>().loadTasks();
    });
  }

  // Helper to determine the border color based on task status
  Color _getTaskBorderColor(Task task) {
    if (task.isCompleted) return Colors.green; // Done

    if (task.dueDate != null) {
      final now = DateTime.now();
      final today = DateTime(now.year, now.month, now.day);
      final dueDay = DateTime(
        task.dueDate!.year,
        task.dueDate!.month,
        task.dueDate!.day,
      );

      if (dueDay.isBefore(today)) {
        return Colors.red; // Overdue
      } else if (dueDay.isAtSameMomentAs(today)) {
        // Amber is used instead of pure yellow for better contrast/readability
        return Colors.amber.shade600; // Due today
      }
    }

    return Colors.blue; // Todo (Future or no due date)
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('My Tasks')),
      body: Consumer<TaskProvider>(
        builder: (context, provider, child) {
          if (provider.tasks.isEmpty) {
            return const Center(child: Text("No tasks yet!"));
          }
          return ListView.builder(
            itemCount: provider.tasks.length,
            itemBuilder: (context, index) {
              final task = provider.tasks[index];
              final borderColor = _getTaskBorderColor(task);

              return Dismissible(
                key: Key(task.id), // CRITICAL: Dismissible needs a unique key
                background: Container(
                  color: Colors.red,
                  alignment: Alignment.centerRight,
                  padding: const EdgeInsets.only(right: 20),
                  child: const Icon(Icons.delete, color: Colors.white),
                ),
                direction: DismissDirection.endToStart,
                onDismissed: (direction) {
                  // Handle deletion via provider
                  context.read<TaskProvider>().removeTask(task.id);
                },
                confirmDismiss: (direction) async {
                  return await AppDialogs.showConfirmation(
                        context: context,
                        title: 'Delete Task',
                        content: 'Are you sure you want to delete this task?',
                      ) ??
                      false; // Fallback to false if the user taps outside the dialog
                },
                child: Card(
                  margin: const EdgeInsets.symmetric(
                    horizontal: 8,
                    vertical: 4,
                  ),
                  elevation: 0, // Keep it flat for a modern look
                  shape: RoundedRectangleBorder(
                    side: BorderSide(color: borderColor, width: 2),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: ListTile(
                    leading: Checkbox(
                      value: task.isCompleted,
                      activeColor:
                          Colors.green, // Match the border color visually
                      onChanged: (_) =>
                          context.read<TaskProvider>().toggleTask(task.id),
                    ),
                    title: Text(
                      task.title,
                      style: TextStyle(
                        decoration: task.isCompleted
                            ? TextDecoration.lineThrough
                            : null,
                        color: task.isCompleted ? Colors.grey : null,
                      ),
                    ),
                    subtitle: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        if (task.description.isNotEmpty)
                          Text(
                            task.description,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(fontSize: 12),
                          ),
                        if (task.dueDate != null)
                          Padding(
                            padding: const EdgeInsets.only(top: 4.0),
                            child: Row(
                              children: [
                                Icon(
                                  Icons.access_time,
                                  size: 14,
                                  color: borderColor,
                                ),
                                const SizedBox(width: 4),
                                Text(
                                  "${task.dueDate!.month.toString().padLeft(2, '0')}/${task.dueDate!.day.toString().padLeft(2, '0')} ${task.dueDate!.hour.toString().padLeft(2, '0')}:${task.dueDate!.minute.toString().padLeft(2, '0')}",
                                  style: TextStyle(
                                    fontSize: 12,
                                    color: borderColor,
                                    fontWeight: FontWeight.w500,
                                  ),
                                ),
                              ],
                            ),
                          ),
                      ],
                    ),
                    // Trailing row for our priority icon and size emoji
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
                          builder: (_) =>
                              TaskDetailScreen(task: task, isEditing: false),
                        ),
                      );
                    },
                    onLongPress: () {
                      // Future implementation: Enter Selection Mode
                    },
                  ),
                ),
              );
            },
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          Navigator.push(
            context,
            MaterialPageRoute(builder: (context) => const AddTaskScreen()),
          );
        },
        child: const Icon(Icons.add),
      ),
    );
  }
}

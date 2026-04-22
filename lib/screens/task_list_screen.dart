import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/task_provider.dart';
import '../widgets/add_task_dialog.dart';
import '../widgets/app_dialogs.dart';
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
          ) ?? false; // Fallback to false if the user taps outside the dialog
      },
      child: ListTile(
        leading: Checkbox(
          value: task.isCompleted,
          onChanged: (_) => context.read<TaskProvider>().toggleTask(task.id),
        ),
        title: Text(task.title),
        subtitle: Text(task.description),
        onTap: () {
          // Open detail screen for viewing/editing
          Navigator.push(
            context,
            MaterialPageRoute(
              builder: (_) => TaskDetailScreen(task: task, isEditing: false),
            ),
          );
        },
        onLongPress: () {
          // Future implementation: Enter Selection Mode
        },
      ),
    );
            },
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
// Show the custom dialog
          showDialog(
            context: context,
            builder: (context) => AddTaskDialog(
              onAdd: (title, description) {
                context.read<TaskProvider>().addTask(title, description);
              },
            ),
          );
        },
        child: const Icon(Icons.add),
      ),
    );
  }
}
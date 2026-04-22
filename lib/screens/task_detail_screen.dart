import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../data/task.dart';
import '../providers/task_provider.dart';
import '../widgets/app_dialogs.dart';

class TaskDetailScreen extends StatefulWidget {
  final Task task;
  final bool isEditing;

  const TaskDetailScreen({super.key, required this.task, required this.isEditing});

  @override
  State<TaskDetailScreen> createState() => _TaskDetailScreenState();
}

class _TaskDetailScreenState extends State<TaskDetailScreen> {
  late TextEditingController _titleController;
  late TextEditingController _descController;
  late bool _isEditing;

  @override
  void initState() {
    super.initState();
    _titleController = TextEditingController(text: widget.task.title);
    _descController = TextEditingController(text: widget.task.description);
    _isEditing = widget.isEditing;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_isEditing ? "Edit Task" : "View Task"),
        actions: [
          if (!_isEditing)
            IconButton(
              icon: const Icon(Icons.edit),
              onPressed: () => setState(() => _isEditing = true),
            ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            TextField(
              controller: _titleController,
              enabled: _isEditing,
              decoration: const InputDecoration(labelText: 'Title'),
            ),
            TextField(
              controller: _descController,
              enabled: _isEditing,
              decoration: const InputDecoration(labelText: 'Description'),
            ),
            if (_isEditing)
              ElevatedButton(
                onPressed: () async {
                  final confirm = await AppDialogs.showConfirmation(
                    context: context,
                    title: 'Save Changes',
                    content: 'Are you sure you want to save changes to this task?',
                  );

                  if (confirm != true) return; // User cancelled

                  final updated = Task(
                    id: widget.task.id,
                    title: _titleController.text,
                    description: _descController.text,
                    isCompleted: widget.task.isCompleted,
                  );
                  WidgetsBinding.instance.addPostFrameCallback((_) {
                    if (!mounted) return; // Safety check
                    context.read<TaskProvider>().updateTask(updated);
                    Navigator.pop(context);
                  });
                },
                child: const Text('Save Changes'),
              ),
          ],
        ),
      ),
    );
  }
}
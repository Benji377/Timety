import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/task_provider.dart';
import '../data/task.dart';
import '../data/category.dart';

class AddTaskScreen extends StatefulWidget {
  const AddTaskScreen({super.key});

  @override
  State<AddTaskScreen> createState() => _AddTaskScreenState();
}

class _AddTaskScreenState extends State<AddTaskScreen> {
  final _formKey = GlobalKey<FormState>();
  final _titleController = TextEditingController();
  final _descriptionController = TextEditingController();
  
  DateTime? _dueDate = DateTime.now();
  TaskPriority _priority = TaskPriority.medium;
  TaskSize _size = TaskSize.medium;
  Category? _selectedCategory;

  @override
  Widget build(BuildContext context) {
    final taskProvider = context.watch<TaskProvider>();
    final categories = taskProvider.categories;

    return Scaffold(
      appBar: AppBar(
        title: const Text('New Task'),
        actions: [
          IconButton(
            icon: const Icon(Icons.check),
            onPressed: _saveTask,
          ),
        ],
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            TextFormField(
              controller: _titleController,
              decoration: const InputDecoration(
                labelText: 'Title',
                border: OutlineInputBorder(),
              ),
              validator: (value) => value == null || value.isEmpty ? 'Title is required' : null,
            ),
            const SizedBox(height: 16),
            TextFormField(
              controller: _descriptionController,
              decoration: const InputDecoration(
                labelText: 'Description (optional)',
                border: OutlineInputBorder(),
              ),
              maxLines: 3,
            ),
            const SizedBox(height: 24),
            ListTile(
              title: const Text('Due Date'),
              subtitle: Text(_dueDate == null ? 'No date set' : '${_dueDate!.year}-${_dueDate!.month}-${_dueDate!.day}'),
              leading: const Icon(Icons.calendar_today),
              onTap: _pickDate,
            ),
            const Divider(),
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 8),
              child: Text('Priority', style: Theme.of(context).textTheme.titleSmall),
            ),
            SegmentedButton<TaskPriority>(
              segments: TaskPriority.values.map((p) => ButtonSegment(value: p, label: Text(p.label))).toList(),
              selected: {_priority},
              onSelectionChanged: (set) => setState(() => _priority = set.first),
            ),
            const SizedBox(height: 16),
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 8),
              child: Text('Size', style: Theme.of(context).textTheme.titleSmall),
            ),
            SegmentedButton<TaskSize>(
              segments: TaskSize.values.map((s) => ButtonSegment(value: s, label: Text(s.badgeText))).toList(),
              selected: {_size},
              onSelectionChanged: (set) => setState(() => _size = set.first),
            ),
            const SizedBox(height: 24),
            DropdownButtonFormField<Category>(
              decoration: const InputDecoration(labelText: 'Category', border: OutlineInputBorder()),
              initialValue: _selectedCategory,
              items: categories.map((c) => DropdownMenuItem(value: c, child: Text(c.name))).toList(),
              onChanged: (val) => setState(() => _selectedCategory = val),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _pickDate() async {
    final date = await showDatePicker(
      context: context,
      initialDate: _dueDate ?? DateTime.now(),
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 365)),
    );
    if (date != null) setState(() => _dueDate = date);
  }

  void _saveTask() {
    if (_formKey.currentState!.validate()) {
      final task = Task(
        title: _titleController.text,
        description: _descriptionController.text.isEmpty ? null : _descriptionController.text,
        iconName: 'default',
        dueDate: _dueDate?.millisecondsSinceEpoch,
        categoryId: _selectedCategory?.id,
        priority: _priority,
        size: _size,
        status: TaskStatus.todo,
      );
      context.read<TaskProvider>().addTask(task);
      Navigator.pop(context);
    }
  }
}

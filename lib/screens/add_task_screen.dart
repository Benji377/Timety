// lib/screens/add_task_screen.dart
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../data/task.dart';
import '../providers/task_provider.dart';
import '../widgets/location_picker.dart';

class AddTaskScreen extends StatefulWidget {
  const AddTaskScreen({super.key});

  @override
  State<AddTaskScreen> createState() => _AddTaskScreenState();
}

class _AddTaskScreenState extends State<AddTaskScreen> {
  final _titleController = TextEditingController();
  final _descController = TextEditingController();
  final _locationController = TextEditingController();
  final _categoryController = TextEditingController();

  DateTime? _dueDate;
  Priority _priority = Priority.low;
  final List<DateTime> _reminders = [];
  String _category = "";

  @override
  void dispose() {
    _titleController.dispose();
    _descController.dispose();
    _locationController.dispose();
    _categoryController.dispose();
    super.dispose();
  }

  Future<void> _pickDueDate() async {
    final pickedDate = await showDatePicker(
      context: context,
      initialDate: DateTime.now(),
      firstDate: DateTime.now(),
      lastDate: DateTime(2100),
    );
    if (pickedDate != null) {
      setState(() => _dueDate = pickedDate);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Create New Task'),
        actions: [
          IconButton(
            icon: const Icon(Icons.check),
            onPressed: () {
              if (_titleController.text.trim().isEmpty) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('Title is required!')),
                );
                return;
              }

              // Create the task (Id is generated here or in provider)
              final newTask = Task(
                id: DateTime.now().toString(),
                title: _titleController.text.trim(),
                description: _descController.text.trim(),
                dueDate: _dueDate,
                location: _locationController.text.trim(),
                priority: _priority,
                reminders: _reminders,
                category: _category,
              );

              context.read<TaskProvider>().addTask(newTask);
              Navigator.pop(context);
            },
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16.0),
        children: [
          TextField(
            controller: _titleController,
            decoration: const InputDecoration(
              labelText: 'Task Title *',
              border: OutlineInputBorder(),
            ),
            autofocus: true,
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _descController,
            decoration: const InputDecoration(
              labelText: 'Description',
              border: OutlineInputBorder(),
            ),
            maxLines: 3,
          ),
          const SizedBox(height: 16),

          // Priority Dropdown
          DropdownButtonFormField<Priority>(
            initialValue: _priority,
            decoration: const InputDecoration(
              labelText: 'Priority',
              border: OutlineInputBorder(),
            ),
            items: Priority.values.map((Priority p) {
              return DropdownMenuItem<Priority>(
                value: p,
                child: Text(p.name.toUpperCase()),
              );
            }).toList(),
            onChanged: (Priority? newValue) {
              if (newValue != null) setState(() => _priority = newValue);
            },
          ),
          const SizedBox(height: 16),

          // Due Date Picker
          ListTile(
            shape: RoundedRectangleBorder(
              side: BorderSide(color: Colors.grey.shade400),
              borderRadius: BorderRadius.circular(4),
            ),
            title: Text(
              _dueDate == null
                  ? 'No Due Date Set'
                  : 'Due: ${_dueDate!.toLocal().toString().split(' ')[0]}',
            ),
            trailing: const Icon(Icons.calendar_today),
            onTap: _pickDueDate,
          ),
          const SizedBox(height: 16),

          // Category Chips Input
          TextField(
            controller: _categoryController,
            decoration: InputDecoration(
              labelText: 'Category',
              border: const OutlineInputBorder(),
              suffixIcon: IconButton(
                icon: const Icon(Icons.add),
                onPressed: () {
                  if (_categoryController.text.isNotEmpty) {
                    setState(() {
                      _category = _categoryController.text.trim();
                      _categoryController.clear();
                    });
                  }
                },
              ),
            ),
          ),
          if (_category.isNotEmpty)
            Padding(
              padding: const EdgeInsets.only(top: 8.0),
              child: Align(
                alignment: Alignment.centerLeft,
                child: Chip(
                  label: Text(_category),
                  onDeleted: () => setState(() => _category = ""),
                ),
              ),
            ),

          const SizedBox(height: 16),
          TextField(
            controller: _locationController,
            decoration: InputDecoration(
              labelText: 'Location (Address, URL, or Coordinates)',
              border: const OutlineInputBorder(),
              prefixIcon: const Icon(Icons.location_on),
              suffixIcon: IconButton(
                icon: const Icon(Icons.map),
                tooltip: 'Pick on Map / Get GPS',
                onPressed: () async {
                  // Navigate to our smart picker
                  final String? result = await Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) => const LocationPicker(),
                    ),
                  );

                  if (result != null) {
                    setState(() {
                      _locationController.text = result;
                    });
                  }
                },
              ),
            ),
          ),
        ],
      ),
    );
  }
}

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

  final List<String> _reminderOptions = [
    'On time',
    '30 minutes before',
    '1 hour before',
    '1 day before',
    'Custom',
  ];
  String _selectedReminderOption = '30 minutes before';

  @override
  void dispose() {
    _titleController.dispose();
    _descController.dispose();
    _locationController.dispose();
    _categoryController.dispose();
    super.dispose();
  }

  Future<void> _pickDueDate() async {
    // 1. Pick the Date
    final pickedDate = await showDatePicker(
      context: context,
      initialDate: _dueDate ?? DateTime.now(),
      firstDate: DateTime.now(),
      lastDate: DateTime(2100),
    );

    // Stop if user canceled
    if (pickedDate == null || !mounted) return;

    // 2. Pick the Time
    final pickedTime = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.fromDateTime(_dueDate ?? DateTime.now()),
    );

    // Stop if user canceled time
    if (pickedTime == null) return;

    // 3. Merge them together
    setState(() {
      _dueDate = DateTime(
        pickedDate.year,
        pickedDate.month,
        pickedDate.day,
        pickedTime.hour,
        pickedTime.minute,
      );
    });
  }

  Future<void> _addReminder() async {
    DateTime? reminderTime;

    if (_selectedReminderOption == 'Custom') {
      reminderTime = await _pickCustomReminderTime();
    } else {
      // Relative reminders require a due date!
      if (_dueDate == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'Please set a Due Date first to use relative reminders.',
            ),
          ),
        );
        return;
      }

      // Calculate relative times
      if (_selectedReminderOption == 'On time') {
        reminderTime = _dueDate!;
      } else if (_selectedReminderOption == '30 minutes before') {
        reminderTime = _dueDate!.subtract(const Duration(minutes: 30));
      } else if (_selectedReminderOption == '1 hour before') {
        reminderTime = _dueDate!.subtract(const Duration(hours: 1));
      } else if (_selectedReminderOption == '1 day before') {
        reminderTime = _dueDate!.subtract(const Duration(days: 1));
      }
    }

    if (reminderTime != null) {
      setState(() {
        // Prevent exact duplicates
        if (!_reminders.contains(reminderTime)) {
          _reminders.add(reminderTime!);
          _reminders.sort(); // Keep them in chronological order
        }
      });
    }
  }

  // Function to handle the 'Custom' option
  Future<DateTime?> _pickCustomReminderTime() async {
    final pickedDate = await showDatePicker(
      context: context,
      initialDate: DateTime.now(),
      firstDate: DateTime.now(),
      // Max date is the due date (if it exists), otherwise year 2100
      lastDate: _dueDate ?? DateTime(2100),
    );
    if (pickedDate == null || !mounted) return null;

    final pickedTime = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.now(),
    );
    if (pickedTime == null) return null;

    final customDateTime = DateTime(
      pickedDate.year,
      pickedDate.month,
      pickedDate.day,
      pickedTime.hour,
      pickedTime.minute,
    );

    // Final safety check if Due Date is set
    if (_dueDate != null && customDateTime.isAfter(_dueDate!)) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Custom reminder must be before the Due Date.'),
          ),
        );
      }
      return null;
    }

    return customDateTime;
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

          // Reminders Section
          const Text(
            'Reminders',
            style: TextStyle(fontWeight: FontWeight.bold),
          ),
          Row(
            children: [
              Expanded(
                child: DropdownButtonFormField<String>(
                  initialValue: _selectedReminderOption,
                  decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                  ),
                  items: _reminderOptions.map((String option) {
                    return DropdownMenuItem(value: option, child: Text(option));
                  }).toList(),
                  onChanged: (val) {
                    if (val != null) {
                      setState(() => _selectedReminderOption = val);
                    }
                  },
                ),
              ),
              const SizedBox(width: 8),
              ElevatedButton.icon(
                onPressed: _addReminder,
                icon: const Icon(Icons.add_alarm),
                label: const Text('Add'),
              ),
            ],
          ),

          // Display the list of added reminders as Chips
          if (_reminders.isNotEmpty)
            Padding(
              padding: const EdgeInsets.only(top: 8.0),
              child: Wrap(
                spacing: 8.0,
                children: _reminders.map((reminder) {
                  // Format: YYYY-MM-DD HH:MM
                  final formattedString =
                      "${reminder.year}-${reminder.month.toString().padLeft(2, '0')}-${reminder.day.toString().padLeft(2, '0')} ${reminder.hour.toString().padLeft(2, '0')}:${reminder.minute.toString().padLeft(2, '0')}";

                  return Chip(
                    label: Text(
                      formattedString,
                      style: const TextStyle(fontSize: 12),
                    ),
                    deleteIcon: const Icon(Icons.close, size: 16),
                    onDeleted: () {
                      setState(() => _reminders.remove(reminder));
                    },
                  );
                }).toList(),
              ),
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

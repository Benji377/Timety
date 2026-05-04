// lib/screens/add_task_screen.dart
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';
import 'package:timety/utils/utils.dart';
import '../data/task.dart';
import '../providers/task_provider.dart';
import '../utils/date_time_picker.dart';
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
  Priority _priority = Priority.medium;
  Size _size = Size.medium;
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

  LatLng? _parseLocation() {
    if (_locationController.text.isEmpty) return null;
    final parts = _locationController.text.split(',');
    if (parts.length == 2) {
      final lat = double.tryParse(parts[0].trim());
      final lng = double.tryParse(parts[1].trim());
      if (lat != null && lng != null) return LatLng(lat, lng);
    }
    return null;
  }

  Future<void> _pickDueDate() async {
    final newDueDate = await AppDatePickers.pickDateTime(
      context: context,
      initialDate: _dueDate,
      // Default to 23:59 if they skip the time picker
      fallbackTime: const TimeOfDay(hour: 23, minute: 59),
    );

    if (newDueDate != null && mounted) {
      setState(() {
        // Clear reminders if the due date has fundamentally changed
        if (_dueDate != newDueDate) {
          _reminders.clear();
        }
        _dueDate = newDueDate;
      });
    }
  }

  Future<void> _addReminder() async {
    DateTime? reminderTime;

    if (_selectedReminderOption == 'Custom') {
      // 1. Summon our reusable picker
      reminderTime = await AppDatePickers.pickDateTime(
        context: context,
        lastDate: _dueDate ?? DateTime(2100),
      );

      // 2. Validate it's before the due date
      if (reminderTime != null &&
          _dueDate != null &&
          reminderTime.isAfter(_dueDate!)) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('Custom reminder must be before the Due Date.'),
            ),
          );
        }
        return; // Stop execution
      }
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

    // 3. Add it to the list
    if (reminderTime != null) {
      setState(() {
        if (!_reminders.contains(reminderTime)) {
          _reminders.add(reminderTime!);
          _reminders.sort();
        }
      });
    }
  }

@override
  Widget build(BuildContext context) {
    final mapCenter = _parseLocation();

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

              final newTask = Task(
                id: DateTime.now().toString(),
                title: _titleController.text.trim(),
                description: _descController.text.trim(),
                dueDate: _dueDate,
                location: _locationController.text.trim(),
                priority: _priority,
                size: _size,
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
          // 1. TITLE
          TextField(
            controller: _titleController,
            decoration: const InputDecoration(
              labelText: 'Task Title *',
              prefixIcon: Icon(Icons.title),
              border: OutlineInputBorder(),
            ),
            autofocus: true,
          ),
          const SizedBox(height: 16),

          // 2. CATEGORY
          TextField(
            controller: _categoryController,
            decoration: InputDecoration(
              labelText: 'Category',
              prefixIcon: const Icon(Icons.label_outline),
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
              padding: const EdgeInsets.only(top: 8.0, left: 12.0),
              child: Align(
                alignment: Alignment.centerLeft,
                child: Chip(
                  label: Text(_category),
                  onDeleted: () => setState(() => _category = ""),
                ),
              ),
            ),
          const SizedBox(height: 16),

          // 3. PRIORITY & SIZE (Fixed with Expanded)
          Row(
            children: [
              Expanded(
                child: DropdownButtonFormField<Priority>(
                  initialValue: _priority,
                  decoration: const InputDecoration(
                    labelText: 'Priority',
                    border: OutlineInputBorder(),
                  ),
                  items: Priority.values.map((Priority p) {
                    return DropdownMenuItem<Priority>(
                      value: p,
                      child: Row(
                        children: [
                          AppUtils().getPriorityIcon(p),
                          const SizedBox(width: 8),
                          Text(p.name.toUpperCase().replaceAll("VERYHIGH", "VERY HIGH")),
                        ],
                      ),
                    );
                  }).toList(),
                  onChanged: (val) {
                    if (val != null) setState(() => _priority = val);
                  },
                ),
              ),
              const SizedBox(width: 16), // Spacing between the two dropdowns
              Expanded(
                child: DropdownButtonFormField<Size>(
                  initialValue: _size,
                  decoration: const InputDecoration(
                    labelText: 'Size',
                    border: OutlineInputBorder(),
                  ),
                  items: Size.values.map((Size s) {
                    return DropdownMenuItem<Size>(
                      value: s,
                      child: Row(
                        children: [
                          Text(AppUtils().getSizeEmoji(s), style: const TextStyle(fontSize: 18)),
                          const SizedBox(width: 8),
                          Text(s.name.toUpperCase().replaceAll('VERYLARGE', 'VERY LARGE')),
                        ],
                      ),
                    );
                  }).toList(),
                  onChanged: (val) {
                    if (val != null) setState(() => _size = val);
                  },
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),

          // 4. DUE DATE & REMINDERS
          ListTile(
            shape: RoundedRectangleBorder(
              side: BorderSide(color: Colors.grey.shade400),
              borderRadius: BorderRadius.circular(4),
            ),
            leading: const Icon(Icons.event),
            title: Text(
              _dueDate == null
                  ? 'No Due Date Set'
                  : 'Due: ${_dueDate!.toLocal().toString().split(' ')[0]} ${_dueDate!.hour.toString().padLeft(2, '0')}:${_dueDate!.minute.toString().padLeft(2, '0')}',
            ),
            trailing: const Icon(Icons.edit_calendar),
            onTap: _pickDueDate,
          ),
          const SizedBox(height: 16),

          const Text('Reminders', style: TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          Row(
            children: [
              Expanded(
                child: DropdownButtonFormField<String>(
                  initialValue: _selectedReminderOption,
                  decoration: const InputDecoration(
                    prefixIcon: Icon(Icons.alarm),
                    border: OutlineInputBorder(),
                  ),
                  items: _reminderOptions.map((String option) {
                    return DropdownMenuItem(value: option, child: Text(option));
                  }).toList(),
                  onChanged: (val) {
                    if (val != null) setState(() => _selectedReminderOption = val);
                  },
                ),
              ),
              const SizedBox(width: 8),
              ElevatedButton.icon(
                onPressed: _addReminder,
                icon: const Icon(Icons.add),
                label: const Text('Add'),
              ),
            ],
          ),

          if (_reminders.isNotEmpty)
            Padding(
              padding: const EdgeInsets.only(top: 8.0),
              child: Wrap(
                spacing: 8.0,
                children: _reminders.map((reminder) {
                  final formattedString =
                      "${reminder.month.toString().padLeft(2, '0')}/${reminder.day.toString().padLeft(2, '0')} ${reminder.hour.toString().padLeft(2, '0')}:${reminder.minute.toString().padLeft(2, '0')}";
                  return Chip(
                    avatar: const Icon(Icons.notifications_active, size: 16),
                    label: Text(formattedString, style: const TextStyle(fontSize: 12)),
                    deleteIcon: const Icon(Icons.close, size: 16),
                    onDeleted: () => setState(() => _reminders.remove(reminder)),
                  );
                }).toList(),
              ),
            ),
          const SizedBox(height: 16),

          // 5. DESCRIPTION
          TextField(
            controller: _descController,
            decoration: const InputDecoration(
              labelText: 'Description',
              prefixIcon: Icon(Icons.notes),
              border: OutlineInputBorder(),
            ),
            maxLines: 4,
          ),
          const SizedBox(height: 16),

          // 6. LOCATION & MAP PREVIEW
          TextField(
            controller: _locationController,
            decoration: InputDecoration(
              labelText: 'Location',
              prefixIcon: const Icon(Icons.location_on),
              border: const OutlineInputBorder(),
              suffixIcon: IconButton(
                icon: const Icon(Icons.map),
                tooltip: 'Pick on Map',
                onPressed: () async {
                  final String? result = await Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) => LocationPicker(
                        initialLocation: _locationController.text,
                      ),
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

          // The read-only mini map preview
          if (mapCenter != null)
            Container(
              height: 150,
              margin: const EdgeInsets.only(top: 8),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(4),
                border: Border.all(color: Colors.grey.shade400),
              ),
              child: ClipRRect(
                borderRadius: BorderRadius.circular(4),
                child: FlutterMap(
                  options: MapOptions(
                    initialCenter: mapCenter,
                    initialZoom: 15.0,
                    // Disable all interactions so it acts purely as a preview image
                    interactionOptions: const InteractionOptions(
                      flags: InteractiveFlag.none,
                    ),
                  ),
                  children: [
                    TileLayer(
                      urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                      userAgentPackageName: 'io.github.benji377.timety',
                    ),
                    MarkerLayer(
                      markers: [
                        Marker(
                          point: mapCenter,
                          child: const Icon(Icons.location_pin, color: Colors.red, size: 40),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            
          const SizedBox(height: 40), // Bottom padding
        ],
      ),
    );
  }
}

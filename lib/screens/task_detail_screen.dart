// lib/screens/task_detail_screen.dart
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';
import '../utils/utils.dart';
import '../data/task.dart';
import '../providers/task_provider.dart';
import '../utils/date_time_picker.dart';
import '../widgets/location_picker.dart';

class TaskDetailScreen extends StatefulWidget {
  final Task? task; // NULL means "Create New Task", NOT NULL means "View/Edit"
  final bool isEditing;

  const TaskDetailScreen({
    super.key, 
    this.task, 
    this.isEditing = false, // Default to false, but we'll override if task is null
  });

  @override
  State<TaskDetailScreen> createState() => _TaskDetailScreenState();
}

class _TaskDetailScreenState extends State<TaskDetailScreen> {
  late bool _isEditing;
  late bool _isNewTask;
  
  // Controllers
  late TextEditingController _titleController;
  late TextEditingController _descController;
  late TextEditingController _locationController;
  final _categoryController = TextEditingController();
  
  // State variables
  late Priority _priority;
  late Size _size;
  DateTime? _dueDate;
  late String _category;
  late List<DateTime> _reminders;

  final List<String> _reminderOptions = [
    'On time', '30 minutes before', '1 hour before', '1 day before', 'Custom',
  ];
  String _selectedReminderOption = '30 minutes before';

  @override
  void initState() {
    super.initState();
    _isNewTask = widget.task == null;
    // If it's a new task, force edit mode to true
    _isEditing = _isNewTask ? true : widget.isEditing;
    
    // Initialize with existing data OR defaults
    _titleController = TextEditingController(text: widget.task?.title ?? '');
    _descController = TextEditingController(text: widget.task?.description ?? '');
    _locationController = TextEditingController(text: widget.task?.location ?? '');
    
    _priority = widget.task?.priority ?? Priority.medium;
    _size = widget.task?.size ?? Size.medium;
    _dueDate = widget.task?.dueDate;
    _category = widget.task?.category ?? "";
    _reminders = widget.task != null ? List.from(widget.task!.reminders) : []; 
  }

  @override
  void dispose() {
    _titleController.dispose();
    _descController.dispose();
    _locationController.dispose();
    _categoryController.dispose();
    super.dispose();
  }

  // --- LOGIC HELPERS (Now shared for both Add and Edit!) ---
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
      fallbackTime: const TimeOfDay(hour: 23, minute: 59),
    );

    if (newDueDate != null && mounted) {
      setState(() {
        if (_dueDate != newDueDate) _reminders.clear();
        _dueDate = newDueDate;
      });
    }
  }

  Future<void> _addReminder() async {
    DateTime? reminderTime;

    if (_selectedReminderOption == 'Custom') {
      reminderTime = await AppDatePickers.pickDateTime(
        context: context,
        lastDate: _dueDate ?? DateTime(2100),
      );

      if (reminderTime != null && _dueDate != null && reminderTime.isAfter(_dueDate!)) {
        if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Custom reminder must be before the Due Date.')));
        return;
      }
    } else {
      if (_dueDate == null) {
        if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Please set a Due Date first.')));
        return;
      }
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
        if (!_reminders.contains(reminderTime)) {
          _reminders.add(reminderTime!);
          _reminders.sort();
        }
      });
    }
  }

  // Master Save Logic
  void _saveTask() {
    if (_titleController.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Title is required!')));
      return;
    }

    final taskToSave = Task(
      id: _isNewTask ? DateTime.now().toString() : widget.task!.id, // Generate ID if new, keep if old
      title: _titleController.text.trim(),
      description: _descController.text.trim(),
      location: _locationController.text.trim(),
      priority: _priority,
      size: _size,
      dueDate: _dueDate,
      category: _category,
      reminders: _reminders,
      isCompleted: _isNewTask ? false : widget.task!.isCompleted,
      completedAt: _isNewTask ? null : widget.task!.completedAt,
      createdAt: _isNewTask ? DateTime.now() : widget.task!.createdAt,
    );

    if (_isNewTask) {
      context.read<TaskProvider>().addTask(taskToSave);
      Navigator.pop(context); // Leave screen entirely after adding
    } else {
      context.read<TaskProvider>().updateTask(taskToSave);
      setState(() => _isEditing = false); // Just exit edit mode
    }
  }

  @override
  Widget build(BuildContext context) {
    final mapCenter = _parseLocation();
    
    // Dynamic App Bar Title
    String appBarTitle = _isNewTask ? "Create New Task" : (_isEditing ? "Edit Task" : "Task Details");

    return Scaffold(
      appBar: AppBar(
        title: Text(appBarTitle),
        actions: [
          // If viewing an existing task, show the edit button.
          if (!_isEditing)
            IconButton(
              icon: const Icon(Icons.edit),
              onPressed: () => setState(() => _isEditing = true),
            ),
          // If we are creating a brand new task, put the checkmark at the top right for convenience
          if (_isNewTask)
            IconButton(
              icon: const Icon(Icons.check),
              onPressed: _saveTask,
            )
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16.0),
        children: [
          // 1. TITLE
          TextField(
            controller: _titleController,
            enabled: _isEditing,
            style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            decoration: InputDecoration(
              labelText: _isEditing ? 'Task Title *' : null,
              prefixIcon: const Icon(Icons.title),
              border: _isEditing ? const OutlineInputBorder() : InputBorder.none,
            ),
          ),
          const SizedBox(height: 16),

          // 2. CATEGORY
          if (_isEditing) ...[
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
            const SizedBox(height: 8),
          ],
          
          if (_category.isNotEmpty)
            Align(
              alignment: Alignment.centerLeft,
              child: Chip(
                label: Text(_category),
                onDeleted: _isEditing ? () => setState(() => _category = "") : null,
              ),
            ),
          const SizedBox(height: 16),

          // 3. PRIORITY & SIZE
          if (_isEditing)
            Row(
              children: [
                Expanded(
                  child: DropdownButtonFormField<Priority>(
                    initialValue: _priority,
                    decoration: const InputDecoration(labelText: 'Priority', border: OutlineInputBorder()),
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
                const SizedBox(width: 16),
                Expanded(
                  child: DropdownButtonFormField<Size>(
                    initialValue: _size,
                    decoration: const InputDecoration(labelText: 'Size', border: OutlineInputBorder()),
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
            )
          else
            Row(
              children: [
                Expanded(
                  child: ListTile(
                    leading: AppUtils().getPriorityIcon(_priority),
                    title: const Text('Priority', style: TextStyle(fontSize: 12, color: Colors.grey)),
                    subtitle: Text(_priority.name.toUpperCase().replaceAll("VERYHIGH", "VERY HIGH")),
                    contentPadding: EdgeInsets.zero,
                  ),
                ),
                Expanded(
                  child: ListTile(
                    leading: Text(AppUtils().getSizeEmoji(_size), style: const TextStyle(fontSize: 24)),
                    title: const Text('Size', style: TextStyle(fontSize: 12, color: Colors.grey)),
                    subtitle: Text(_size.name.toUpperCase().replaceAll('VERYLARGE', 'VERY LARGE')),
                    contentPadding: EdgeInsets.zero,
                  ),
                ),
              ],
            ),
          const SizedBox(height: 16),

          // 4. DUE DATE & REMINDERS
          ListTile(
            shape: RoundedRectangleBorder(
              side: BorderSide(color: _isEditing ? Colors.grey.shade400 : Colors.transparent),
              borderRadius: BorderRadius.circular(4),
            ),
            contentPadding: _isEditing ? const EdgeInsets.symmetric(horizontal: 12) : EdgeInsets.zero,
            leading: const Icon(Icons.event),
            title: Text(
              _dueDate == null
                  ? 'No Due Date Set'
                  : 'Due: ${_dueDate!.toLocal().toString().split(' ')[0]} ${_dueDate!.hour.toString().padLeft(2, '0')}:${_dueDate!.minute.toString().padLeft(2, '0')}',
            ),
            trailing: _isEditing ? const Icon(Icons.edit_calendar) : null,
            onTap: _isEditing ? _pickDueDate : null,
          ),
          const SizedBox(height: 16),

          // Reminders
          if (_isEditing || _reminders.isNotEmpty) ...[
            const Text('Reminders', style: TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            
            if (_isEditing)
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
                      deleteIcon: _isEditing ? const Icon(Icons.close, size: 16) : null,
                      onDeleted: _isEditing ? () => setState(() => _reminders.remove(reminder)) : null,
                    );
                  }).toList(),
                ),
              ),
            const SizedBox(height: 16),
          ],

          // 5. DESCRIPTION
          TextField(
            controller: _descController,
            enabled: _isEditing,
            decoration: InputDecoration(
              labelText: _isEditing ? 'Description' : null,
              prefixIcon: const Icon(Icons.notes),
              border: _isEditing ? const OutlineInputBorder() : InputBorder.none,
            ),
            maxLines: 4,
          ),
          const SizedBox(height: 16),

          // 6. LOCATION & MAP PREVIEW
          TextField(
            controller: _locationController,
            enabled: _isEditing,
            decoration: InputDecoration(
              labelText: _isEditing ? 'Location' : null,
              prefixIcon: const Icon(Icons.location_on),
              border: _isEditing ? const OutlineInputBorder() : InputBorder.none,
              suffixIcon: _isEditing ? IconButton(
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
              ) : null,
            ),
          ),

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
                    interactionOptions: const InteractionOptions(flags: InteractiveFlag.none),
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
            
          const SizedBox(height: 32),

          // 7. SAVE BUTTON (Only show dialog if editing an existing task, otherwise just save)
          if (_isEditing && !_isNewTask)
            ElevatedButton.icon(
              icon: const Icon(Icons.save),
              onPressed: () {
                showDialog(
                  context: context,
                  builder: (ctx) => AlertDialog(
                    title: const Text('Confirm Save'),
                    content: const Text('Are you sure you want to save changes to this task?'),
                    actions: [
                      TextButton(onPressed: () => Navigator.of(ctx).pop(), child: const Text('Cancel')),
                      ElevatedButton(
                        onPressed: () {
                          Navigator.of(ctx).pop(); 
                          _saveTask();
                        },
                        child: const Text('Save'),
                      ),
                    ],
                  ),
                );
              },
              label: const Text('Save Changes'),
              style: ElevatedButton.styleFrom(padding: const EdgeInsets.all(16)),
            ),
            
          const SizedBox(height: 40), 
        ],
      ),
    );
  }
}
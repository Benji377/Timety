import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../providers/task_provider.dart';
import '../data/task.dart';
import '../data/category.dart';
import '../utils/notification_helper.dart';

class AddTaskScreen extends StatefulWidget {
  final Task? initialTask;

  const AddTaskScreen({super.key, this.initialTask});

  @override
  State<AddTaskScreen> createState() => _AddTaskScreenState();
}

class _AddTaskScreenState extends State<AddTaskScreen> {
  final _formKey = GlobalKey<FormState>();
  final _titleController = TextEditingController();
  final _descriptionController = TextEditingController();
  final _locationController = TextEditingController();

  DateTime? _dueDate;
  TimeOfDay? _dueTime;
  List<DateTime> _reminders = [];
  TaskPriority _priority = TaskPriority.medium;
  TaskSize _size = TaskSize.medium;
  Category? _selectedCategory;

  @override
  void initState() {
    super.initState();
    if (widget.initialTask != null) {
      final task = widget.initialTask!;
      _titleController.text = task.title;
      _descriptionController.text = task.description ?? '';
      _locationController.text = task.location ?? '';
      _dueDate = task.dueDateTime;
      _dueTime = task.dueTimeDateTime != null
          ? TimeOfDay.fromDateTime(task.dueTimeDateTime!)
          : null;
      _reminders = task.reminderDateTimes;
      _priority = task.priority;
      _size = task.size;
      _selectedCategory = null; // Will be set from provider
    }
  }

  @override
  void dispose() {
    _titleController.dispose();
    _descriptionController.dispose();
    _locationController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final taskProvider = context.watch<TaskProvider>();
    final categories = taskProvider.categories;

    // Set initial category if not already set
    if (_selectedCategory == null && widget.initialTask?.categoryId != null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        try {
          setState(() {
            _selectedCategory = categories.firstWhere(
              (c) => c.id == widget.initialTask!.categoryId,
            );
          });
        } catch (_) {}
      });
    }

    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            // Header
            Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    widget.initialTask == null ? 'New Task' : 'Edit Task',
                    style: Theme.of(context).textTheme.headlineSmall,
                  ),
                  IconButton(
                    onPressed: Navigator.of(context).pop,
                    icon: const Icon(Icons.close),
                  ),
                ],
              ),
            ),
            // Form
            Expanded(
              child: Form(
                key: _formKey,
                child: ListView(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  children: [
                    // Title
                    TextFormField(
                      controller: _titleController,
                      decoration: const InputDecoration(
                        labelText: 'Title *',
                        border: OutlineInputBorder(),
                        prefixIcon: Icon(Icons.task),
                      ),
                      validator: (value) => value == null || value.isEmpty
                          ? 'Title is required'
                          : null,
                    ),
                    const SizedBox(height: 16),

                    // Description
                    TextFormField(
                      controller: _descriptionController,
                      decoration: const InputDecoration(
                        labelText: 'Description (optional)',
                        border: OutlineInputBorder(),
                        prefixIcon: Icon(Icons.description),
                      ),
                      maxLines: 3,
                    ),
                    const SizedBox(height: 16),

                    // Location
                    TextFormField(
                      controller: _locationController,
                      decoration: const InputDecoration(
                        labelText: 'Location (optional)',
                        border: OutlineInputBorder(),
                        prefixIcon: Icon(Icons.location_on),
                        hintText: 'Type address or place name',
                      ),
                    ),
                    const SizedBox(height: 24),

                    // Due Date & Time
                    Text(
                      'Due Date & Time',
                      style: Theme.of(context).textTheme.titleSmall,
                    ),
                    const SizedBox(height: 12),
                    Row(
                      children: [
                        Expanded(
                          child: ListTile(
                            title: Text(
                              _dueDate == null
                                  ? 'Select date'
                                  : DateFormat('MMM d, yyyy').format(_dueDate!),
                            ),
                            leading: const Icon(Icons.calendar_today),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(8),
                              side: BorderSide(
                                color: Theme.of(context).colorScheme.outline,
                              ),
                            ),
                            onTap: _pickDate,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: ListTile(
                            title: Text(
                              _dueTime == null
                                  ? 'Select time'
                                  : _dueTime!.format(context),
                            ),
                            leading: const Icon(Icons.schedule),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(8),
                              side: BorderSide(
                                color: Theme.of(context).colorScheme.outline,
                              ),
                            ),
                            onTap: _pickTime,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 24),

                    // Reminders
                    Text(
                      'Reminders',
                      style: Theme.of(context).textTheme.titleSmall,
                    ),
                    const SizedBox(height: 12),
                    ..._buildReminderChips(),
                    const SizedBox(height: 12),
                    ElevatedButton.icon(
                      onPressed: _addReminder,
                      icon: const Icon(Icons.add),
                      label: const Text('Add Reminder'),
                    ),
                    const SizedBox(height: 24),

                    // Priority
                    Text(
                      'Priority',
                      style: Theme.of(context).textTheme.titleSmall,
                    ),
                    const SizedBox(height: 12),
                    SegmentedButton<TaskPriority>(
                      segments: TaskPriority.values
                          .map(
                            (p) =>
                                ButtonSegment(value: p, label: Text(p.label)),
                          )
                          .toList(),
                      selected: {_priority},
                      onSelectionChanged: (set) =>
                          setState(() => _priority = set.first),
                    ),
                    const SizedBox(height: 24),

                    // Size
                    Text('Size', style: Theme.of(context).textTheme.titleSmall),
                    const SizedBox(height: 12),
                    SegmentedButton<TaskSize>(
                      segments: TaskSize.values
                          .map(
                            (s) => ButtonSegment(
                              value: s,
                              label: Text(s.badgeText),
                            ),
                          )
                          .toList(),
                      selected: {_size},
                      onSelectionChanged: (set) =>
                          setState(() => _size = set.first),
                    ),
                    const SizedBox(height: 24),

                    // Category
                    DropdownButtonFormField<Category>(
                      decoration: const InputDecoration(
                        labelText: 'Category',
                        border: OutlineInputBorder(),
                        prefixIcon: Icon(Icons.category),
                      ),
                      value: _selectedCategory,
                      items: categories
                          .map(
                            (c) =>
                                DropdownMenuItem(value: c, child: Text(c.name)),
                          )
                          .toList(),
                      onChanged: (val) =>
                          setState(() => _selectedCategory = val),
                    ),
                    const SizedBox(height: 32),
                  ],
                ),
              ),
            ),

            // Action Buttons
            Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                children: [
                  if (widget.initialTask != null)
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: _deleteTask,
                        icon: const Icon(Icons.delete),
                        label: const Text('Delete'),
                        style: OutlinedButton.styleFrom(
                          foregroundColor: Theme.of(context).colorScheme.error,
                        ),
                      ),
                    ),
                  if (widget.initialTask != null) const SizedBox(width: 12),
                  Expanded(
                    child: ElevatedButton.icon(
                      onPressed: _saveTask,
                      icon: const Icon(Icons.check),
                      label: Text(
                        widget.initialTask == null ? 'Create' : 'Update',
                      ),
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

  List<Widget> _buildReminderChips() {
    return _reminders.map((reminder) {
      return Padding(
        padding: const EdgeInsets.only(right: 8, bottom: 8),
        child: Chip(
          label: Text(DateFormat('MMM d, h:mm a').format(reminder)),
          onDeleted: () =>
              setState(() => _reminders.removeWhere((r) => r == reminder)),
          deleteIcon: const Icon(Icons.close, size: 18),
        ),
      );
    }).toList();
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

  Future<void> _pickTime() async {
    final time = await showTimePicker(
      context: context,
      initialTime: _dueTime ?? TimeOfDay.now(),
    );
    if (time != null) setState(() => _dueTime = time);
  }

  Future<void> _addReminder() async {
    final date = await showDatePicker(
      context: context,
      initialDate: _dueDate ?? DateTime.now(),
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 365)),
    );

    if (date != null && mounted) {
      final time = await showTimePicker(
        context: context,
        initialTime: TimeOfDay.now(),
      );

      if (time != null) {
        final reminderDateTime = DateTime(
          date.year,
          date.month,
          date.day,
          time.hour,
          time.minute,
        );

        setState(() {
          _reminders.add(reminderDateTime);
          _reminders.sort();
        });
      }
    }
  }

  void _saveTask() async {
    if (_formKey.currentState!.validate()) {
      final dueDateTime = _dueDate != null && _dueTime != null
          ? DateTime(
              _dueDate!.year,
              _dueDate!.month,
              _dueDate!.day,
              _dueTime!.hour,
              _dueTime!.minute,
            )
          : null;

      final task = Task(
        id: widget.initialTask?.id,
        title: _titleController.text,
        description: _descriptionController.text.isEmpty
            ? null
            : _descriptionController.text,
        location: _locationController.text.isEmpty
            ? null
            : _locationController.text,
        dueDate: _dueDate?.millisecondsSinceEpoch,
        dueTime: dueDateTime?.millisecondsSinceEpoch,
        reminders: _reminders.map((r) => r.millisecondsSinceEpoch).toList(),
        categoryId: _selectedCategory?.id,
        priority: _priority,
        size: _size,
        status: widget.initialTask?.status ?? TaskStatus.todo,
        xpAwarded: widget.initialTask?.xpAwarded ?? false,
      );

      // Schedule notifications for reminders
      for (int i = 0; i < _reminders.length; i++) {
        await NotificationHelper.showTaskReminder(
          (task.id ?? 0) * 1000 + i,
          'Reminder: ${task.title}',
          task.description ?? 'Task reminder',
          _reminders[i],
        );
      }

      if (widget.initialTask != null) {
        context.read<TaskProvider>().updateTask(task);
      } else {
        context.read<TaskProvider>().addTask(task);
      }

      Navigator.pop(context);
    }
  }

  void _deleteTask() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete Task'),
        content: const Text('Are you sure you want to delete this task?'),
        actions: [
          TextButton(
            onPressed: Navigator.of(context).pop,
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () {
              context.read<TaskProvider>().deleteTask(widget.initialTask!.id!);
              Navigator.of(context).pop();
              Navigator.of(context).pop();
            },
            child: const Text('Delete'),
          ),
        ],
      ),
    );
  }
}

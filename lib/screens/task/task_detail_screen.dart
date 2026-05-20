import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../utils/priority_utils.dart';
import '../../data/task/task.dart';
import '../../providers/task_provider.dart';
import '../../theme/app_theme.dart';
import '../../utils/date_time_utils.dart';
import '../../widgets/location_picker.dart';

class TaskDetailScreen extends StatefulWidget {
  final Task? task; // NULL means "Create New Task", NOT NULL means "View/Edit"
  final bool isEditing;

  const TaskDetailScreen({super.key, this.task, this.isEditing = false});

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
  final _newCategoryController = TextEditingController();
  final _newSubtaskController = TextEditingController();

  // State variables
  late Priority _priority;
  late Size _size;
  DateTime? _dueDate;
  late String _category;
  late List<DateTime> _reminders;
  late List<Subtask> _subtasks;

  bool _isAddingNewCategory = false;

  final List<String> _reminderOptions = [
    'On time',
    '30 minutes before',
    '1 hour before',
    '1 day before',
    'Custom',
  ];
  String _selectedReminderOption = '30 minutes before';

  @override
  void initState() {
    super.initState();
    _isNewTask = widget.task == null;
    _isEditing = _isNewTask ? true : widget.isEditing;

    _titleController = TextEditingController(text: widget.task?.title ?? '');
    _descController = TextEditingController(
      text: widget.task?.description ?? '',
    );
    _locationController = TextEditingController(
      text: widget.task?.location ?? '',
    );

    _priority = widget.task?.priority ?? Priority.medium;
    _size = widget.task?.size ?? Size.medium;
    _dueDate = widget.task?.dueDate;
    _category = widget.task?.category ?? "";
    _reminders = widget.task != null ? List.from(widget.task!.reminders) : [];

    // Deep copy subtasks so we don't accidentally mutate Hive objects before saving
    _subtasks = widget.task != null
        ? widget.task!.subtasks
              .map(
                (s) => Subtask(
                  id: s.id,
                  title: s.title,
                  isCompleted: s.isCompleted,
                ),
              )
              .toList()
        : [];
  }

  @override
  void dispose() {
    _titleController.dispose();
    _descController.dispose();
    _locationController.dispose();
    _newCategoryController.dispose();
    _newSubtaskController.dispose();
    super.dispose();
  }

  Widget _buildSectionHeader(String title, IconData icon) {
    final colorScheme = Theme.of(context).colorScheme;

    return Padding(
      padding: const EdgeInsets.only(
        top: AppTheme.spaceXLarge,
        bottom: AppTheme.spaceMedium,
      ),
      child: Row(
        children: [
          Icon(icon, size: AppTheme.iconSizeSmall, color: colorScheme.primary),
          const SizedBox(width: AppTheme.spaceSmall),
          Text(
            title.toUpperCase(),
            style: TextStyle(
              fontSize: AppTheme.fsBodySmall,
              fontWeight: AppTheme.fwBold,
              color: colorScheme.primary.withValues(alpha: 0.8),
              letterSpacing: AppTheme.lsWide,
            ),
          ),
          const SizedBox(width: AppTheme.spaceSmall),
          const Expanded(child: Divider()),
        ],
      ),
    );
  }

  Widget _buildCategoryPicker() {
    final dividerColor = Theme.of(context).dividerColor.withValues(alpha: 0.6);

    // Get unique categories from existing tasks
    final List<String> existingCategories = context
        .read<TaskProvider>()
        .tasks
        .map((t) => t.category.trim())
        .where((c) => c.isNotEmpty)
        .toSet()
        .toList();

    if (!_isEditing) {
      return TextField(
        controller: TextEditingController(
          text: _category.isEmpty ? "None" : _category,
        ),
        enabled: false,
        decoration: InputDecoration(
          labelText: 'Category',
          prefixIcon: const Icon(Icons.label_outline),
          border: const OutlineInputBorder(),
          disabledBorder: OutlineInputBorder(
            borderSide: BorderSide(color: dividerColor),
          ),
        ),
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (!_isAddingNewCategory)
          DropdownButtonFormField<String>(
            initialValue: existingCategories.contains(_category)
                ? _category
                : null,
            hint: Text(_category.isEmpty ? "Select Category" : _category),
            decoration: const InputDecoration(
              labelText: 'Category',
              prefixIcon: Icon(Icons.label_outline),
              border: OutlineInputBorder(),
            ),
            items: [
              const DropdownMenuItem(value: "", child: Text("None")),
              ...existingCategories.map(
                (cat) => DropdownMenuItem(value: cat, child: Text(cat)),
              ),
              const DropdownMenuItem(
                value: "__ADD_NEW__",
                child: Row(
                  children: [
                    Icon(
                      Icons.add,
                      size: AppTheme.iconSizeSmall,
                      color: AppTheme.infoColor,
                    ),
                    SizedBox(width: AppTheme.spaceSmall),
                    Text(
                      "Add New Category...",
                      style: TextStyle(color: AppTheme.infoColor),
                    ),
                  ],
                ),
              ),
            ],
            onChanged: (val) {
              if (val == "__ADD_NEW__") {
                setState(() => _isAddingNewCategory = true);
              } else if (val != null) {
                setState(() => _category = val);
              }
            },
          )
        else
          TextField(
            controller: _newCategoryController,
            autofocus: true,
            decoration: InputDecoration(
              labelText: 'New Category Name',
              prefixIcon: const Icon(Icons.label_important_outline),
              border: const OutlineInputBorder(),
              suffixIcon: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  IconButton(
                    icon: const Icon(Icons.check, color: AppTheme.successColor),
                    onPressed: () {
                      if (_newCategoryController.text.trim().isNotEmpty) {
                        setState(() {
                          _category = _newCategoryController.text.trim();
                          _isAddingNewCategory = false;
                          _newCategoryController.clear();
                        });
                      }
                    },
                  ),
                  IconButton(
                    icon: const Icon(Icons.close, color: AppTheme.errorColor),
                    onPressed: () =>
                        setState(() => _isAddingNewCategory = false),
                  ),
                ],
              ),
            ),
          ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final disabledBorderColor = theme.dividerColor.withValues(alpha: 0.6);

    final String appBarTitle = _isNewTask
        ? "Create New Task"
        : (_isEditing ? "Edit Task" : "Task Details");

    return Scaffold(
      appBar: AppBar(
        title: Text(appBarTitle),
        actions: [
          if (!_isEditing)
            IconButton(
              icon: const Icon(Icons.edit),
              onPressed: () => setState(() => _isEditing = true),
            ),
          if (_isNewTask)
            IconButton(icon: const Icon(Icons.check), onPressed: _saveTask),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.symmetric(
          horizontal: AppTheme.spaceLarge,
          vertical: AppTheme.spaceSmall,
        ),
        children: [
          // --- SECTION: THE BASICS ---
          _buildSectionHeader("Task Info", Icons.info_outline),
          TextField(
            controller: _titleController,
            enabled: _isEditing,
            style: const TextStyle(
              fontSize: AppTheme.fsHeadingSmall,
              fontWeight: AppTheme.fwBold,
            ),
            decoration: InputDecoration(
              labelText: 'Task Title *',
              prefixIcon: const Icon(Icons.title),
              border: const OutlineInputBorder(),
              disabledBorder: OutlineInputBorder(
                borderSide: BorderSide(color: disabledBorderColor),
              ),
            ),
          ),
          const SizedBox(height: AppTheme.spaceLarge),

          _buildCategoryPicker(),

          const SizedBox(height: AppTheme.spaceLarge),
          TextField(
            controller: _descController,
            enabled: _isEditing,
            ignorePointers: _isEditing ? null : false,
            keyboardType: TextInputType.multiline,
            maxLines: 3,
            scrollPhysics: const BouncingScrollPhysics(),
            decoration: InputDecoration(
              labelText: 'Description',
              prefixIcon: const Icon(Icons.notes),
              alignLabelWithHint: true,
              border: const OutlineInputBorder(),
              disabledBorder: OutlineInputBorder(
                borderSide: BorderSide(color: disabledBorderColor),
              ),
            ),
          ),

          // --- SECTION: PRIORITY & SIZE ---
          _buildSectionHeader("Priority & Effort", Icons.bar_chart),
          Row(
            children: [
              Expanded(
                child: _isEditing
                    ? DropdownButtonFormField<Priority>(
                        initialValue: _priority,
                        decoration: const InputDecoration(
                          labelText: 'Priority',
                          border: OutlineInputBorder(),
                        ),
                        items: Priority.values
                            .map(
                              (p) => DropdownMenuItem(
                                value: p,
                                child: Text(p.name.toUpperCase()),
                              ),
                            )
                            .toList(),
                        onChanged: (val) => setState(() => _priority = val!),
                      )
                    : ListTile(
                        title: const Text(
                          "Priority",
                          style: TextStyle(fontSize: AppTheme.fsBodySmall),
                        ),
                        subtitle: Text(_priority.name.toUpperCase()),
                        leading: AppUtils().getPriorityIcon(_priority),
                        contentPadding: EdgeInsets.zero,
                      ),
              ),
              const SizedBox(width: AppTheme.spaceLarge),
              Expanded(
                child: _isEditing
                    ? DropdownButtonFormField<Size>(
                        initialValue: _size,
                        decoration: const InputDecoration(
                          labelText: 'Size',
                          border: OutlineInputBorder(),
                        ),
                        items: Size.values
                            .map(
                              (s) => DropdownMenuItem(
                                value: s,
                                child: Text(s.name.toUpperCase()),
                              ),
                            )
                            .toList(),
                        onChanged: (val) => setState(() => _size = val!),
                      )
                    : ListTile(
                        title: const Text(
                          "Size",
                          style: TextStyle(fontSize: AppTheme.fsBodySmall),
                        ),
                        subtitle: Text(_size.name.toUpperCase()),
                        leading: Text(
                          AppUtils().getSizeEmoji(_size),
                          style: const TextStyle(fontSize: 20),
                        ),
                        contentPadding: EdgeInsets.zero,
                      ),
              ),
            ],
          ),

          // --- SECTION: TIME ---
          _buildSectionHeader("Schedule", Icons.calendar_today),
          ListTile(
            tileColor: _isEditing
                ? colorScheme.primaryContainer.withValues(alpha: 0.45)
                : null,
            shape: RoundedRectangleBorder(
              borderRadius: AppTheme.brMedium,
              side: BorderSide(
                color: _isEditing
                    ? colorScheme.primary.withValues(alpha: 0.4)
                    : theme.dividerColor.withValues(alpha: 0.6),
              ),
            ),
            leading: const Icon(Icons.event),
            title: Text(
              _dueDate == null
                  ? 'No Due Date Set'
                  : 'Due: ${_dueDate!.toLocal()}'.split('.')[0],
            ),
            trailing: _isEditing ? const Icon(Icons.edit, size: 20) : null,
            onTap: _isEditing ? _pickDueDate : null,
          ),

          // Reminders
          if (_isEditing || _reminders.isNotEmpty) ...[
            const SizedBox(height: AppTheme.spaceMedium),
            if (_isEditing) _buildReminderInput(),
            const SizedBox(height: AppTheme.spaceSmall),
            Wrap(
              spacing: AppTheme.spaceSmall,
              children: _reminders
                  .map(
                    (r) => Chip(
                      backgroundColor: colorScheme.surfaceContainerHighest
                          .withValues(alpha: 0.7),
                      label: Text(
                        "${r.day}/${r.month} ${r.hour}:${r.minute.toString().padLeft(2, '0')}",
                        style: const TextStyle(fontSize: AppTheme.fsBodySmall),
                      ),
                      onDeleted: _isEditing
                          ? () => setState(() => _reminders.remove(r))
                          : null,
                    ),
                  )
                  .toList(),
            ),
          ],

          // --- SECTION: LOCATION ---
          _buildSectionHeader("Location", Icons.location_on_outlined),
          TextField(
            controller: _locationController,
            enabled: _isEditing,
            decoration: InputDecoration(
              labelText: 'Location',
              prefixIcon: const Icon(Icons.map_outlined),
              border: const OutlineInputBorder(),
              disabledBorder: OutlineInputBorder(
                borderSide: BorderSide(color: disabledBorderColor),
              ),
              suffixIcon: _isEditing
                  ? IconButton(
                      icon: const Icon(Icons.search),
                      onPressed: _pickLocation,
                    )
                  : null,
            ),
          ),

          // --- SUBTASKS ---
          _buildSectionHeader("Checklist", Icons.checklist),

          // Display existing subtasks
          ..._subtasks.map(
            (subtask) => CheckboxListTile(
              contentPadding: EdgeInsets.zero,
              controlAffinity: ListTileControlAffinity.leading,
              fillColor: WidgetStateProperty.resolveWith((states) {
                if (states.contains(WidgetState.selected)) {
                  return AppTheme.successColor;
                }
                return Colors.transparent;
              }),
              checkColor: Colors.white,
              side: const BorderSide(color: AppTheme.taskColor, width: 2),
              value: subtask.isCompleted,
              title: Text(
                subtask.title,
                style: TextStyle(
                  decoration: subtask.isCompleted
                      ? TextDecoration.lineThrough
                      : null,
                  color: subtask.isCompleted ? Colors.grey : null,
                ),
              ),
              secondary: _isEditing
                  ? IconButton(
                      icon: const Icon(
                        Icons.close,
                        color: AppTheme.errorColor,
                        size: 20,
                      ),
                      onPressed: () =>
                          setState(() => _subtasks.remove(subtask)),
                    )
                  : null,
              onChanged: (val) {
                setState(() => subtask.isCompleted = val ?? false);
                // Quick-save so checking off subtasks works outside of edit mode!
                if (!_isEditing && !_isNewTask) {
                  _saveTask(); // Auto-saves changes in the background
                  setState(() => _isEditing = false); // keeps it in view mode
                }
              },
            ),
          ),

          // Add Subtask Field (Only in edit mode)
          if (_isEditing)
            Padding(
              padding: const EdgeInsets.only(top: AppTheme.spaceSmall),
              child: Row(
                children: [
                  const Icon(
                    Icons.subdirectory_arrow_right,
                    color: Colors.grey,
                  ),
                  const SizedBox(width: AppTheme.spaceMedium),
                  Expanded(
                    child: TextField(
                      controller: _newSubtaskController,
                      decoration: InputDecoration(
                        hintText: "Add subtask...",
                        border: InputBorder.none,
                        focusedBorder: InputBorder.none,
                        enabledBorder: InputBorder.none,
                        disabledBorder: InputBorder.none,
                        contentPadding: EdgeInsets.zero,
                        suffixIcon: IconButton(
                          icon: const Icon(
                            Icons.add_circle,
                            color: AppTheme.infoColor,
                          ),
                          onPressed: () {
                            if (_newSubtaskController.text.trim().isNotEmpty) {
                              setState(() {
                                _subtasks.add(
                                  Subtask(
                                    id: DateTime.now().toString(),
                                    title: _newSubtaskController.text.trim(),
                                  ),
                                );
                                _newSubtaskController.clear();
                              });
                            }
                          },
                        ),
                      ),
                      onSubmitted: (val) {
                        if (val.trim().isNotEmpty) {
                          setState(() {
                            _subtasks.add(
                              Subtask(
                                id: DateTime.now().toString(),
                                title: val.trim(),
                              ),
                            );
                            _newSubtaskController.clear();
                          });
                        }
                      },
                    ),
                  ),
                ],
              ),
            ),

          const SizedBox(height: AppTheme.space3XLarge),

          if (_isEditing && !_isNewTask)
            ElevatedButton.icon(
              icon: const Icon(Icons.save),
              onPressed: _saveTask,
              label: const Text('Save Changes'),
              style: ElevatedButton.styleFrom(
                minimumSize: const ui.Size.fromHeight(54),
                shape: const RoundedRectangleBorder(
                  borderRadius: AppTheme.brLarge,
                ),
              ),
            ),
          const SizedBox(height: AppTheme.space3XLarge),
        ],
      ),
    );
  }

  // --- Logic Helpers ---
  Future<void> _pickDueDate() async {
    final picked = await AppDatePickers.pickDateTime(
      context: context,
      initialDate: _dueDate,
    );
    if (picked != null) setState(() => _dueDate = picked);
  }

  Future<void> _pickLocation() async {
    final String? result = await Navigator.push(
      context,
      MaterialPageRoute(builder: (c) => const LocationPicker()),
    );

    if (result != null && result.isNotEmpty) {
      setState(() {
        _locationController.text = result;
      });
    }
  }

  Widget _buildReminderInput() {
    return Row(
      children: [
        Expanded(
          child: DropdownButtonFormField<String>(
            initialValue: _selectedReminderOption,
            items: _reminderOptions
                .map((o) => DropdownMenuItem(value: o, child: Text(o)))
                .toList(),
            onChanged: (v) => setState(() => _selectedReminderOption = v!),
            decoration: const InputDecoration(
              labelText: "Set Reminder",
              border: OutlineInputBorder(),
              contentPadding: EdgeInsets.symmetric(
                horizontal: AppTheme.spaceMedium,
              ),
            ),
          ),
        ),
        const SizedBox(width: AppTheme.spaceSmall),
        ElevatedButton(onPressed: _addReminder, child: const Text("Add")),
      ],
    );
  }

  Future<void> _addReminder() async {
    DateTime? reminderTime;

    if (_selectedReminderOption == 'Custom') {
      reminderTime = await AppDatePickers.pickDateTime(
        context: context,
        lastDate: _dueDate ?? DateTime(2100),
      );

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
        return;
      }
    } else {
      if (_dueDate == null) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Please set a Due Date first.')),
          );
        }
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
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Title is required!')));
      return;
    }

    final taskToSave = Task(
      id: _isNewTask
          ? DateTime.now().toString()
          : widget.task!.id, // Generate ID if new, keep if old
      title: _titleController.text.trim(),
      description: _descController.text.trim(),
      location: _locationController.text.trim(),
      priority: _priority,
      size: _size,
      dueDate: _dueDate,
      category: _category,
      reminders: _reminders,
      subtasks: _subtasks,
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
}

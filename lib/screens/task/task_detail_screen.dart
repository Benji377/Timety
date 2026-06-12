import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../l10n/app_localizations.dart';
import '../../utils/common/app_utils.dart';
import '../../data/task/task.dart';
import '../../providers/task_provider.dart';
import '../../theme/app_theme.dart';
import '../../utils/datetime/date_time_pickers.dart';
import '../../widgets/location/location_picker.dart';
import '../../providers/settings_provider.dart';

enum CategoryAction { none, addNew }

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
  ReminderOption _selectedReminderOption = ReminderOption.minutes30Before;

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

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final disabledBorderColor = theme.dividerColor.withValues(alpha: 0.6);
    final settings = context.watch<SettingsProvider>();
    final l10n = AppLocalizations.of(context)!;

    final String appBarTitle = _isNewTask
        ? l10n.taskDetailsTitleNew
        : (_isEditing
              ? l10n.taskDetailsTitleEdit
              : l10n.taskDetailsTitleDetails);

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
          _buildSectionHeader(l10n.taskDetailsSectionInfo, Icons.info_outline),
          TextField(
            controller: _titleController,
            enabled: _isEditing,
            style: const TextStyle(
              fontSize: AppTheme.fsHeadingSmall,
              fontWeight: AppTheme.fwBold,
            ),
            decoration: InputDecoration(
              labelText: l10n.taskDetailsLabelTitle,
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
              labelText: l10n.taskDetailsLabelDescription,
              prefixIcon: const Icon(Icons.notes),
              alignLabelWithHint: true,
              border: const OutlineInputBorder(),
              disabledBorder: OutlineInputBorder(
                borderSide: BorderSide(color: disabledBorderColor),
              ),
            ),
          ),

          // --- SECTION: PRIORITY & SIZE ---
          _buildSectionHeader(
            l10n.taskDetailsSectionPriorityAndEffort,
            Icons.bar_chart,
          ),
          Row(
            children: [
              Expanded(
                child: _isEditing
                    ? DropdownButtonFormField<Priority>(
                        initialValue: _priority,
                        decoration: InputDecoration(
                          labelText: l10n.taskDetailsLabelPriority,
                          border: const OutlineInputBorder(),
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
                        title: Text(
                          l10n.taskDetailsLabelPriority,
                          style: const TextStyle(
                            fontSize: AppTheme.fsBodySmall,
                          ),
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
                        decoration: InputDecoration(
                          labelText: l10n.taskDetailsLabelEffort,
                          border: const OutlineInputBorder(),
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
                        title: Text(
                          l10n.taskDetailsLabelEffort,
                          style: const TextStyle(
                            fontSize: AppTheme.fsBodySmall,
                          ),
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
          _buildSectionHeader(
            l10n.taskDetailsSectionScheduling,
            Icons.calendar_today,
          ),
          _isEditing
              ? InkWell(
                  onTap: _pickDueDate,
                  borderRadius: const BorderRadius.all(Radius.circular(4.0)),
                  child: InputDecorator(
                    isEmpty: _dueDate == null,
                    decoration: const InputDecoration(
                      prefixIcon: Icon(Icons.event),
                      suffixIcon: Icon(Icons.edit),
                      border: OutlineInputBorder(),
                      filled: true,
                    ),
                    child: Text(
                      _dueDate == null
                          ? l10n.taskDetailsLabelDueDateNone
                          : l10n.taskDetailsLabelDueDate(
                              settings.getFormattedDate(_dueDate!),
                              settings.getFormattedTime(_dueDate!),
                            ),
                    ),
                  ),
                )
              : ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.event),
                  title: Text(
                    _dueDate == null
                        ? l10n.taskDetailsLabelDueDateNone
                        : l10n.taskDetailsLabelDueDate(
                            settings.getFormattedDate(_dueDate!),
                            settings.getFormattedTime(_dueDate!),
                          ),
                  ),
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
                        "${settings.getFormattedDate(r)} - ${settings.getFormattedTime(r)}",
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
          _buildSectionHeader(
            l10n.taskDetailsSectionLocation,
            Icons.location_on_outlined,
          ),
          TextField(
            controller: _locationController,
            enabled: _isEditing,
            decoration: InputDecoration(
              labelText: l10n.taskDetailsLabelLocation,
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
          _buildSectionHeader(
            l10n.taskDetailsSectionChecklist,
            Icons.checklist,
          ),

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
                        hintText: l10n.taskDetailsLabelSubtaskHint,
                        alignLabelWithHint: true,
                        border: InputBorder.none,
                        focusedBorder: InputBorder.none,
                        enabledBorder: InputBorder.none,
                        disabledBorder: InputBorder.none,
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
              label: Text(l10n.taskDetailsLabelSaveEdit),
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
    final l10n = AppLocalizations.of(context)!;

    if (!_isEditing) {
      return TextFormField(
        initialValue: _category.isEmpty
            ? l10n.taskDetailsLabelCategoryEmpty
            : _category,
        enabled: false,
        decoration: InputDecoration(
          labelText: l10n.taskDetailsLabelCategory,
          prefixIcon: const Icon(Icons.label_outline),
          border: const OutlineInputBorder(),
          disabledBorder: OutlineInputBorder(
            borderSide: BorderSide(color: dividerColor),
          ),
        ),
      );
    }

    final List<String> existingCategories = context
        .read<TaskProvider>()
        .tasks
        .map((t) => t.category.trim())
        .where((c) => c.isNotEmpty)
        .toSet()
        .toList();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (!_isAddingNewCategory)
          DropdownButtonFormField<Object?>(
            initialValue: _category.isEmpty
                ? CategoryAction.none
                : (existingCategories.contains(_category) ? _category : null),
            hint: Text(
              _category.isEmpty
                  ? l10n.taskDetailsLabelCategorySelect
                  : _category,
            ),
            decoration: InputDecoration(
              labelText: l10n.taskDetailsLabelCategory,
              prefixIcon: const Icon(Icons.label_outline),
              border: const OutlineInputBorder(),
            ),
            items: [
              DropdownMenuItem(
                value: CategoryAction.none,
                child: Text(l10n.taskDetailsLabelCategoryEmpty),
              ),
              ...existingCategories.map(
                (cat) => DropdownMenuItem(value: cat, child: Text(cat)),
              ),
              DropdownMenuItem(
                value: CategoryAction.addNew,
                child: Row(
                  children: [
                    const Icon(
                      Icons.add,
                      size: AppTheme.iconSizeSmall,
                      color: AppTheme.infoColor,
                    ),
                    const SizedBox(width: AppTheme.spaceSmall),
                    Text(
                      l10n.taskDetailsLabelCategoryAddNew,
                      style: const TextStyle(color: AppTheme.infoColor),
                    ),
                  ],
                ),
              ),
            ],
            onChanged: (val) {
              if (val == CategoryAction.addNew) {
                setState(() => _isAddingNewCategory = true);
              } else if (val == CategoryAction.none) {
                setState(() => _category = "");
              } else if (val is String) {
                setState(() => _category = val);
              }
            },
          )
        else
          TextField(
            controller: _newCategoryController,
            autofocus: true,
            decoration: InputDecoration(
              labelText: l10n.taskDetailsLabelCategoryNewName,
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
    final l10n = AppLocalizations.of(context)!;

    return Row(
      children: [
        Expanded(
          child: DropdownButtonFormField<ReminderOption>(
            initialValue: _selectedReminderOption,
            items: ReminderOption.values.map((option) {
              return DropdownMenuItem<ReminderOption>(
                value: option,
                // Pass the context into the Enum here!
                child: Text(option.getLocalizedLabel(context)),
              );
            }).toList(),
            onChanged: (v) => setState(() => _selectedReminderOption = v!),
            decoration: InputDecoration(
              labelText: l10n.taskDetailsLabelReminderSet,
              border: const OutlineInputBorder(),
              contentPadding: const EdgeInsets.symmetric(
                horizontal: AppTheme.spaceMedium,
              ),
            ),
          ),
        ),
        const SizedBox(width: AppTheme.spaceSmall),
        ElevatedButton(
          onPressed: _addReminder,
          child: Text(l10n.commonLabelAdd),
        ),
      ],
    );
  }

  Future<void> _addReminder() async {
    final l10n = AppLocalizations.of(context)!;
    DateTime? reminderTime;

    if (_selectedReminderOption == ReminderOption.custom) {
      reminderTime = await AppDatePickers.pickDateTime(
        context: context,
        lastDate: _dueDate ?? DateTime(2100),
      );

      if (reminderTime != null &&
          _dueDate != null &&
          reminderTime.isAfter(_dueDate!)) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(l10n.taskDetailsLabelReminderTooEarly)),
          );
        }
        return;
      }
    } else {
      if (_dueDate == null) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(l10n.taskDetailsSnackbarReminderNoDue)),
          );
        }
        return;
      }
      if (_selectedReminderOption == ReminderOption.onTime) {
        reminderTime = _dueDate!;
      } else if (_selectedReminderOption == ReminderOption.minutes30Before) {
        reminderTime = _dueDate!.subtract(const Duration(minutes: 30));
      } else if (_selectedReminderOption == ReminderOption.hour1Before) {
        reminderTime = _dueDate!.subtract(const Duration(hours: 1));
      } else if (_selectedReminderOption == ReminderOption.day1Before) {
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
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            AppLocalizations.of(context)!.taskDetailsSnackbarTitleRequired,
          ),
        ),
      );
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

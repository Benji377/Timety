import 'package:flutter/material.dart';
import '../data/focus/focus_models.dart';
import '../data/habit/habit_models.dart';
import '../data/task/task.dart';
import '../theme/app_theme.dart';

/// Reusable bottom sheet builders for focus-related UIs
class FocusBottomSheetBuilders {
  /// Shows a bottom sheet for logging distraction events
  ///
  /// Returns after user selects an event, which is then logged via the [onEventSelected] callback
  static void showDistractionSheet({
    required BuildContext context,
    required Function(String eventName) onEventSelected,
  }) {
    const events = [
      {
        'name': 'Distracted',
        'icon': Icons.warning_amber,
        'color': AppTheme.errorColor,
      },
      {
        'name': 'Hydrated / Drink',
        'icon': Icons.water_drop,
        'color': AppTheme.taskColor,
      },
      {
        'name': 'Stretched',
        'icon': Icons.accessibility_new,
        'color': AppTheme.warningColor,
      },
      {
        'name': 'Snack',
        'icon': Icons.restaurant,
        'color': AppTheme.successColor,
      },
      {'name': 'Restroom', 'icon': Icons.wc, 'color': Colors.grey},
    ];

    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) {
        return SafeArea(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Padding(
                padding: EdgeInsets.all(16.0),
                child: Text(
                  "Log an Event",
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
              ),
              ...events.map(
                (e) => ListTile(
                  leading: Icon(
                    e['icon'] as IconData,
                    color: e['color'] as Color,
                  ),
                  title: Text(
                    e['name'] as String,
                    style: const TextStyle(fontWeight: FontWeight.w500),
                  ),
                  onTap: () {
                    final eventName = e['name'] as String;
                    onEventSelected(eventName);
                    Navigator.pop(context);
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                        content: Text('Logged: $eventName'),
                        duration: const Duration(seconds: 2),
                      ),
                    );
                  },
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  /// Shows a bottom sheet for selecting the linked focus target.
  static void showTargetSelector({
    required BuildContext context,
    required List<FocusTag> tags,
    required List<Task> tasks,
    required List<Habit> habits,
    required FocusTargetType selectedType,
    required String? selectedId,
    required Function(FocusTag tag) onTagSelected,
    required Function(Task task) onTaskSelected,
    required Function(Habit habit) onHabitSelected,
    required Function() onCreateNewTag,
  }) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) {
        return FractionallySizedBox(
          heightFactor: 0.9,
          child: DefaultTabController(
            length: 3,
            child: SafeArea(
              child: Column(
                children: [
                  const SizedBox(height: 10),
                  Container(
                    width: 42,
                    height: 4,
                    decoration: BoxDecoration(
                      color: Theme.of(
                        context,
                      ).colorScheme.onSurfaceVariant.withValues(alpha: 0.2),
                      borderRadius: BorderRadius.circular(999),
                    ),
                  ),
                  const Padding(
                    padding: EdgeInsets.fromLTRB(16, 16, 16, 12),
                    child: Text(
                      'Select Focus Target',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  const TabBar(
                    tabs: [
                      Tab(text: 'Tags'),
                      Tab(text: 'Tasks'),
                      Tab(text: 'Habits'),
                    ],
                  ),
                  Expanded(
                    child: TabBarView(
                      children: [
                        _buildTagTab(
                          context: context,
                          tags: tags,
                          selectedType: selectedType,
                          selectedId: selectedId,
                          onTagSelected: onTagSelected,
                          onCreateNewTag: onCreateNewTag,
                        ),
                        _buildTaskTab(
                          context: context,
                          tasks: tasks,
                          selectedType: selectedType,
                          selectedId: selectedId,
                          onTaskSelected: onTaskSelected,
                        ),
                        _buildHabitTab(
                          context: context,
                          habits: habits,
                          selectedType: selectedType,
                          selectedId: selectedId,
                          onHabitSelected: onHabitSelected,
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }

  static Widget _buildTagTab({
    required BuildContext context,
    required List<FocusTag> tags,
    required FocusTargetType selectedType,
    required String? selectedId,
    required Function(FocusTag tag) onTagSelected,
    required Function() onCreateNewTag,
  }) {
    return Column(
      children: [
        Expanded(
          child: tags.isEmpty
              ? const Center(child: Text('No focus tags yet.'))
              : ListView.separated(
                  padding: const EdgeInsets.symmetric(vertical: 8),
                  itemCount: tags.length,
                  separatorBuilder: (_, _) => const Divider(height: 1),
                  itemBuilder: (context, index) {
                    final tag = tags[index];
                    final isSelected =
                        selectedType == FocusTargetType.tag &&
                        selectedId == tag.id;
                    return ListTile(
                      leading: CircleAvatar(
                        backgroundColor: Color(tag.colorValue),
                        radius: 12,
                      ),
                      title: Text(
                        tag.name,
                        style: TextStyle(
                          fontWeight: isSelected
                              ? FontWeight.bold
                              : FontWeight.normal,
                        ),
                      ),
                      trailing: isSelected
                          ? const Icon(
                              Icons.check,
                              color: AppTheme.successColor,
                            )
                          : null,
                      onTap: () {
                        onTagSelected(tag);
                        Navigator.pop(context);
                      },
                    );
                  },
                ),
        ),
        const Divider(height: 1),
        ListTile(
          leading: const Icon(Icons.add_circle_outline),
          title: const Text('Create New Tag'),
          onTap: () {
            Navigator.pop(context);
            onCreateNewTag();
          },
        ),
      ],
    );
  }

  static Widget _buildTaskTab({
    required BuildContext context,
    required List<Task> tasks,
    required FocusTargetType selectedType,
    required String? selectedId,
    required Function(Task task) onTaskSelected,
  }) {
    return tasks.isEmpty
        ? const Center(child: Text('No tasks available.'))
        : ListView.separated(
            padding: const EdgeInsets.symmetric(vertical: 8),
            itemCount: tasks.length,
            separatorBuilder: (_, _) => const Divider(height: 1),
            itemBuilder: (context, index) {
              final task = tasks[index];
              final isSelected =
                  selectedType == FocusTargetType.task && selectedId == task.id;
              return ListTile(
                leading: Icon(
                  task.isCompleted ? Icons.check_circle : Icons.task_alt,
                  color: task.isCompleted
                      ? AppTheme.successColor
                      : AppTheme.taskColor,
                ),
                title: Text(
                  task.title,
                  style: TextStyle(
                    fontWeight: isSelected
                        ? FontWeight.bold
                        : FontWeight.normal,
                    decoration: task.isCompleted
                        ? TextDecoration.lineThrough
                        : null,
                  ),
                ),
                subtitle: Text(
                  task.category.isNotEmpty
                      ? task.category
                      : (task.isCompleted ? 'Completed task' : 'Task'),
                ),
                trailing: isSelected
                    ? const Icon(Icons.check, color: AppTheme.successColor)
                    : null,
                onTap: () {
                  onTaskSelected(task);
                  Navigator.pop(context);
                },
              );
            },
          );
  }

  static Widget _buildHabitTab({
    required BuildContext context,
    required List<Habit> habits,
    required FocusTargetType selectedType,
    required String? selectedId,
    required Function(Habit habit) onHabitSelected,
  }) {
    return habits.isEmpty
        ? const Center(child: Text('No habits available.'))
        : ListView.separated(
            padding: const EdgeInsets.symmetric(vertical: 8),
            itemCount: habits.length,
            separatorBuilder: (_, _) => const Divider(height: 1),
            itemBuilder: (context, index) {
              final habit = habits[index];
              final isSelected =
                  selectedType == FocusTargetType.habit &&
                  selectedId == habit.id;
              return ListTile(
                leading: Icon(
                  habit.iconData ?? Icons.radio_button_unchecked,
                  color: Color(habit.colorValue),
                ),
                title: Text(
                  habit.name,
                  style: TextStyle(
                    fontWeight: isSelected
                        ? FontWeight.bold
                        : FontWeight.normal,
                  ),
                ),
                subtitle: Text(habit.frequency.name),
                trailing: isSelected
                    ? const Icon(Icons.check, color: AppTheme.successColor)
                    : null,
                onTap: () {
                  onHabitSelected(habit);
                  Navigator.pop(context);
                },
              );
            },
          );
  }

  /// Shows a dialog for creating a new focus tag
  ///
  /// [onTagCreated] is called with the tag name when user confirms.
  static void showCreateTagDialog({
    required BuildContext context,
    required Function(String tagName) onTagCreated,
  }) {
    final TextEditingController controller = TextEditingController();

    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text("New Tag"),
          content: TextField(
            controller: controller,
            decoration: const InputDecoration(
              hintText: "Tag Name (e.g. Reading)",
            ),
            autofocus: true,
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text("Cancel"),
            ),
            ElevatedButton(
              onPressed: () {
                if (controller.text.trim().isNotEmpty) {
                  onTagCreated(controller.text.trim());
                  Navigator.pop(context);
                }
              },
              child: const Text("Save"),
            ),
          ],
        );
      },
    );
  }
}

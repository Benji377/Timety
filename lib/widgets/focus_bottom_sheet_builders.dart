import 'package:flutter/material.dart';
import '../data/focus/focus_models.dart';
import '../data/habit/habit_models.dart';
import '../providers/habit_provider.dart';
import '../data/task/task.dart';
import '../theme/app_theme.dart';
import '../utils/habit_utils.dart';
import '../l10n/app_localizations.dart';
import 'dialogs.dart';

/// Reusable bottom sheet builders for focus-related UIs
class FocusBottomSheetBuilders {
  /// Shows a bottom sheet for logging distraction events
  ///
  /// Returns after user selects an event, which is then logged via the [onEventSelected] callback
  static void showDistractionSheet({
    required BuildContext context,
    required Function(String eventName) onEventSelected,
  }) {

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
              Padding(
                padding: const EdgeInsets.all(16.0),
                child: Text(
                  AppLocalizations.of(context)!.distractionSheetTitle,
                  style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
              ),
              ...DistractionType.values.map(
                (type) => ListTile(
                  leading: Icon(
                    type.icon,
                    color: type.color,
                  ),
                  title: Text(
                    type.getLocalizedName(context),
                    style: const TextStyle(fontWeight: FontWeight.w500),
                  ),
                  onTap: () {
                    final eventName = type.getLocalizedName(context);
                    onEventSelected(type.dbId);
                    Navigator.pop(context);
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                        content: Text(AppLocalizations.of(context)!.distractionLogged(eventName)),
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
    required HabitProvider habitProvider,
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
                  Padding(
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 12),
                    child: Text(
                      AppLocalizations.of(context)!.targetSheetTitle,
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  TabBar(
                    tabs: [
                      Tab(text: AppLocalizations.of(context)!.globalLabelTags),
                      Tab(text: AppLocalizations.of(context)!.globalLabelTasks),
                      Tab(text: AppLocalizations.of(context)!.globalLabelHabits),
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
                          habitProvider: habitProvider,
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
              ? Center(child: Text(AppLocalizations.of(context)!.focusTagsLabelEmpty))
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
          title: Text(AppLocalizations.of(context)!.focusTagsLabelAdd),
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
        ? Center(child: Text(AppLocalizations.of(context)!.taskSheetEmpty))
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
                      : (task.isCompleted ? AppLocalizations.of(context)!.taskLabelCompleted : AppLocalizations.of(context)!.globalLabelTask),
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
    required HabitProvider habitProvider,
    required FocusTargetType selectedType,
    required String? selectedId,
    required Function(Habit habit) onHabitSelected,
  }) {
    if (habits.isEmpty) {
      return Center(child: Text(AppLocalizations.of(context)!.habitsSheetEmpty));
    }

    final grouped = <String, List<Habit>>{};
    final standalone = <Habit>[];

    for (final habit in habits) {
      if (habit.stackName != null && habit.stackName!.trim().isNotEmpty) {
        grouped.putIfAbsent(habit.stackName!.trim(), () => []).add(habit);
      } else {
        standalone.add(habit);
      }
    }

    final today = DateTime.now();
    final children = <Widget>[];

    void addHabitTile(Habit habit, {required bool isLocked}) {
      final isSelected =
          selectedType == FocusTargetType.habit && selectedId == habit.id;

      final statusText = habit.frequency == HabitFrequency.daily
          ? AppLocalizations.of(context)!.habitLabelFreqDaily
          : AppLocalizations.of(context)!.habitLabelFreqWeekly;

      children.add(
        ListTile(
          enabled: !isLocked,
          leading: Icon(
            isLocked
                ? Icons.lock_outline
                : (habit.iconData ?? Icons.radio_button_unchecked),
            color: isLocked ? Colors.grey : Color(habit.colorValue),
          ),
          title: Text(
            habit.name,
            style: TextStyle(
              fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
              color: isLocked ? Colors.grey : null,
            ),
          ),
          subtitle: Text(
            statusText,
            style: TextStyle(
              color: isLocked
                  ? Colors.grey
                  : Theme.of(context).colorScheme.onSurfaceVariant,
            ),
          ),
          trailing: isLocked
              ? const Icon(Icons.lock, color: Colors.grey)
              : isSelected
              ? const Icon(Icons.check, color: AppTheme.successColor)
              : null,
          onTap: isLocked
              ? null
              : () {
                  onHabitSelected(habit);
                  Navigator.pop(context);
                },
        ),
      );
    }

    grouped.forEach((stackName, stackHabits) {
      stackHabits.sort(
        (a, b) => (a.stackOrder ?? 99).compareTo(b.stackOrder ?? 99),
      );

      if (children.isNotEmpty) {
        children.add(const Divider(height: 1));
      }

      children.add(
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
          child: Text(
            stackName.toUpperCase(),
            style: const TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.bold,
              letterSpacing: 1.1,
              color: Colors.grey,
            ),
          ),
        ),
      );

      for (var index = 0; index < stackHabits.length; index++) {
        final habit = stackHabits[index];
        final isDone = habitProvider.isCompletedOn(habit, today);
        final isPreviousHabitDone = index > 0
            ? habitProvider.isCompletedOn(stackHabits[index - 1], today)
            : true;

        addHabitTile(
          habit,
          isLocked: HabitUtils.isHabitLocked(
            index: index,
            isCurrentHabitDone: isDone,
            isPreviousHabitDone: isPreviousHabitDone,
          ),
        );
      }
    });

    if (standalone.isNotEmpty) {
      if (children.isNotEmpty) {
        children.add(const Divider(height: 1));
      }

      for (final habit in standalone) {
        addHabitTile(habit, isLocked: false);
      }
    }

    return ListView(
      padding: const EdgeInsets.symmetric(vertical: 8),
      children: children,
    );
  }

  /// Shows a dialog for creating a new focus tag
  ///
  /// [onTagCreated] is called with the tag name when user confirms.
  static void showCreateTagDialog({
    required BuildContext context,
    required Function(String tagName) onTagCreated,
  }) {
    AppDialogs.showTextInputDialog(
      context: context,
      title: AppLocalizations.of(context)!.focusTagsDialogTitleAdd,
      labelText: AppLocalizations.of(context)!.focusTagsDialogLabelName,
      hintText: AppLocalizations.of(context)!.focusTagsDialogLabelHint,
    ).then((tagName) {
      if (tagName != null) {
        onTagCreated(tagName);
      }
    });
  }
}

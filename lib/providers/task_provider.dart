import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import '../data/task/task.dart';
import '../data/task/task_repository.dart';
import '../services/notification_service.dart';
import '../services/android_widgets/task_widget_service.dart';
import '../utils/ui/l10n_utils.dart';
import '../utils/stats/xp_calculator.dart';
import 'settings_provider.dart';
import 'user_provider.dart';

class TaskProvider extends ChangeNotifier {
  final TaskRepository repository;
  List<Task> _tasks = [];
  SettingsProvider? _settings;

  List<Task> get tasks => _tasks;

  TaskProvider({required this.repository});

  void updateSettings(SettingsProvider settings) {
    _settings = settings;
    _notifyAndSync();
  }

  // Helper to notify listeners, save to repository, and update home widget
  Future<void> _notifyAndSync() async {
    notifyListeners();

    final locale =
        _settings?.appLocale ?? ui.PlatformDispatcher.instance.locale;

    TaskWidgetService.updateTaskWidget(_tasks, locale);
  }

  // Helper to generate a unique integer ID for a specific reminder
  int _generateNotificationId(String taskId, DateTime reminderTime) {
    // Combines task ID and time to ensure each reminder has a unique Int ID
    return "$taskId${reminderTime.toIso8601String()}".hashCode;
  }

  int _generateDueDateNotificationId(String taskId, DateTime dueDate) {
    return _generateNotificationId('${taskId}_due', dueDate);
  }

  String _buildReminderBody(
    Task task,
    DateTime reminderTime, {
    DateTime? dueDate,
    bool exactDueDate = false,
  }) {
    final l10n = getL10n(settings: _settings);
    if (exactDueDate) {
      return l10n.taskReminderBodyExact(task.title);
    }

    final referenceTime = dueDate ?? reminderTime;
    final timeUntilDue = referenceTime.difference(reminderTime);

    String reminderPrefix;
    if (timeUntilDue.inMinutes <= 0) {
      reminderPrefix = l10n.taskReminderPrefixNow;
    } else if (timeUntilDue.inMinutes < 60) {
      reminderPrefix = l10n.nTaskReminderPrefixMinutes(timeUntilDue.inMinutes);
    } else if (timeUntilDue.inHours < 24) {
      reminderPrefix = l10n.nTaskReminderPrefixHours(timeUntilDue.inHours);
    } else {
      final days = timeUntilDue.inDays;
      reminderPrefix = l10n.nTaskReminderPrefixDays(days);
    }

    return l10n.taskReminderBody(reminderPrefix, task.title);
  }

  Future<void> _cancelTaskNotifications(Task task) async {
    for (var reminder in task.reminders) {
      final notifId = _generateNotificationId(task.id, reminder);
      await NotificationService.instance.cancelNotification(notifId);
    }

    if (task.reminders.isEmpty && task.dueDate != null) {
      await NotificationService.instance.cancelNotification(
        _generateDueDateNotificationId(task.id, task.dueDate!),
      );
    }
  }

  // Master synchronization function
  Future<void> _syncTaskReminders(Task task, {Task? previousTask}) async {
    final l10n = getL10n(settings: _settings);

    if (previousTask != null) {
      await _cancelTaskNotifications(previousTask);
    } else {
      await _cancelTaskNotifications(task);
    }

    // If the task is completed, we don't reschedule them. We just stop here.
    if (task.isCompleted) return;

    // If not completed, schedule all future reminders and a due-date fallback.
    for (var reminder in task.reminders) {
      if (reminder.isAfter(DateTime.now())) {
        final notifId = _generateNotificationId(task.id, reminder);
        await NotificationService.instance.scheduleTaskReminder(
          notificationId: notifId,
          title: l10n.taskReminderTitle(task.title),
          body: _buildReminderBody(task, reminder, dueDate: task.dueDate),
          scheduledTime: reminder,
          l10n: l10n,
        );
      }
    }

    if (task.reminders.isEmpty && task.dueDate != null) {
      final dueDate = task.dueDate!;
      if (dueDate.isAfter(DateTime.now())) {
        await NotificationService.instance.scheduleTaskReminder(
          notificationId: _generateDueDateNotificationId(task.id, dueDate),
          title: l10n.taskReminderTitle(task.title),
          body: _buildReminderBody(task, dueDate, exactDueDate: true),
          scheduledTime: dueDate,
          l10n: l10n,
        );
      }
    }
  }

  // Load data initially
  Future<void> loadTasks() async {
    _tasks = await repository.fetchTasks();
    await _notifyAndSync();
  }

  Future<void> addTask(Task task) async {
    _tasks.add(task);
    _syncTaskReminders(task);
    await repository.saveTask(task);
    await _notifyAndSync();
  }

  Future<void> toggleTask(String id, {UserProvider? userProvider}) async {
    final index = _tasks.indexWhere((t) => t.id == id);
    if (index != -1) {
      _tasks[index].isCompleted = !_tasks[index].isCompleted;

      if (_tasks[index].isCompleted) {
        _tasks[index].completedAt = DateTime.now();
        userProvider?.addXp(ExperienceEngine.xpPerTask);
      } else {
        _tasks[index].completedAt = null;
        userProvider?.addXp(-ExperienceEngine.xpPerTask);
      }

      _syncTaskReminders(_tasks[index]);
      await repository.saveTask(_tasks[index]);
      await _notifyAndSync();
    }
  }

  Future<void> markTaskCompleted(
    String id, {
    UserProvider? userProvider,
  }) async {
    final index = _tasks.indexWhere((task) => task.id == id);
    if (index == -1 || _tasks[index].isCompleted) return;
    await toggleTask(id, userProvider: userProvider);
  }

  Task? getTaskById(String id) {
    for (final task in _tasks) {
      if (task.id == id) return task;
    }
    return null;
  }

  Future<void> removeTask(String id) async {
    final task = _tasks.firstWhere((t) => t.id == id);
    // Cancel all before removing
    await _cancelTaskNotifications(task);
    _tasks.removeWhere((task) => task.id == id);
    await repository.deleteTask(id);
    await _notifyAndSync();
  }

  Future<void> updateTask(Task updatedTask) async {
    final index = _tasks.indexWhere((t) => t.id == updatedTask.id);
    if (index != -1) {
      final previousTask = _tasks[index];
      _tasks[index] = updatedTask;
      _syncTaskReminders(updatedTask, previousTask: previousTask);
      await repository.saveTask(updatedTask);
      await _notifyAndSync();
    }
  }

  List<String> getAllCategories() {
    final Set<String> categories = {};
    for (var task in _tasks) {
      if (task.category.isNotEmpty) {
        categories.add(task.category);
      }
    }
    return categories.toList()..sort();
  }

  Future<void> renameCategory(String oldName, String newName) async {
    if (oldName == newName) return;

    for (var i = 0; i < _tasks.length; i++) {
      if (_tasks[i].category == oldName) {
        _tasks[i] = Task(
          id: _tasks[i].id,
          title: _tasks[i].title,
          description: _tasks[i].description,
          dueDate: _tasks[i].dueDate,
          location: _tasks[i].location,
          priority: _tasks[i].priority,
          size: _tasks[i].size,
          reminders: _tasks[i].reminders,
          category: newName,
          isCompleted: _tasks[i].isCompleted,
          completedAt: _tasks[i].completedAt,
          createdAt: _tasks[i].createdAt,
          subtasks: _tasks[i].subtasks,
        );
        await repository.saveTask(_tasks[i]);
      }
    }
    await _notifyAndSync();
  }

  Future<void> deleteCategory(String categoryName) async {
    for (var i = 0; i < _tasks.length; i++) {
      if (_tasks[i].category == categoryName) {
        _tasks[i] = Task(
          id: _tasks[i].id,
          title: _tasks[i].title,
          description: _tasks[i].description,
          dueDate: _tasks[i].dueDate,
          location: _tasks[i].location,
          priority: _tasks[i].priority,
          size: _tasks[i].size,
          reminders: _tasks[i].reminders,
          isCompleted: _tasks[i].isCompleted,
          completedAt: _tasks[i].completedAt,
          createdAt: _tasks[i].createdAt,
          subtasks: _tasks[i].subtasks,
        );
        await repository.saveTask(_tasks[i]);
      }
    }
    await _notifyAndSync();
  }
}

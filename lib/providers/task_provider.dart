import 'package:flutter/material.dart';
import '../data/task/task.dart';
import '../data/task/task_repository.dart';
import '../services/notification_service.dart';
import '../utils/xp_calculator.dart';
import 'user_provider.dart';

class TaskProvider extends ChangeNotifier {
  final TaskRepository repository;
  List<Task> _tasks = [];

  List<Task> get tasks => _tasks;

  TaskProvider({required this.repository});

  // Helper to generate a unique integer ID for a specific reminder
  int _generateNotificationId(String taskId, DateTime reminderTime) {
    // Combines task ID and time to ensure each reminder has a unique Int ID
    return "$taskId${reminderTime.toIso8601String()}".hashCode;
  }

  // Master synchronization function
  Future<void> _syncTaskReminders(Task task) async {
    // Always cancel existing reminders for this task first to prevent duplicates
    for (var reminder in task.reminders) {
      final notifId = _generateNotificationId(task.id, reminder);
      await NotificationService.instance.cancelNotification(notifId);
    }

    // If the task is completed, we don't reschedule them. We just stop here.
    if (task.isCompleted) return;

    // If not completed, schedule all future reminders
    for (var reminder in task.reminders) {
      if (reminder.isAfter(DateTime.now())) {
        final notifId = _generateNotificationId(task.id, reminder);
        await NotificationService.instance.scheduleTaskReminder(
          notificationId: notifId,
          title: 'Reminder: ${task.title}',
          body: task.description.isNotEmpty
              ? task.description
              : 'It is time for your task!',
          scheduledTime: reminder,
        );
      }
    }
  }

  // Load data initially
  Future<void> loadTasks() async {
    _tasks = await repository.fetchTasks();
    notifyListeners();
  }

  Future<void> addTask(Task task) async {
    _tasks.add(task);
    _syncTaskReminders(task);
    await repository.saveTasks(_tasks);
    notifyListeners();
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
      await repository.saveTasks(_tasks);
      notifyListeners();
    }
  }

  Future<void> removeTask(String id) async {
    final task = _tasks.firstWhere((t) => t.id == id);
    // Cancel all before removing
    for (var reminder in task.reminders) {
      NotificationService.instance.cancelNotification(
        _generateNotificationId(task.id, reminder),
      );
    }
    _tasks.removeWhere((task) => task.id == id);
    await repository.saveTasks(_tasks);
    notifyListeners();
  }

  Future<void> updateTask(Task updatedTask) async {
    final index = _tasks.indexWhere((t) => t.id == updatedTask.id);
    if (index != -1) {
      _tasks[index] = updatedTask;
      _syncTaskReminders(updatedTask);
      await repository.saveTasks(_tasks);
      notifyListeners();
    }
  }
}

import 'package:hive/hive.dart';
import 'package:flutter/material.dart';
import '../../l10n/app_localizations.dart';

part 'task.g.dart';

@HiveType(typeId: 11)
enum Priority {
  @HiveField(0)
  low,
  @HiveField(1)
  medium,
  @HiveField(2)
  high,
  @HiveField(3)
  veryHigh,
}

@HiveType(typeId: 12)
enum Size {
  @HiveField(0)
  small,
  @HiveField(1)
  medium,
  @HiveField(2)
  large,
  @HiveField(3)
  veryLarge,
}

@HiveType(typeId: 13)
class Subtask {
  @HiveField(0)
  final String id;
  @HiveField(1)
  final String title;
  @HiveField(2)
  bool isCompleted;

  Subtask({required this.id, required this.title, this.isCompleted = false});
}

@HiveType(typeId: 10)
class Task {
  @HiveField(0)
  final String id;
  @HiveField(1)
  final String title;
  @HiveField(2)
  final String description;
  @HiveField(3)
  final DateTime? dueDate;
  @HiveField(4)
  final String location;
  @HiveField(5)
  final Priority priority;
  @HiveField(6)
  final List<DateTime> reminders;
  @HiveField(7)
  final String category;
  @HiveField(8)
  final Size size;
  @HiveField(9)
  bool isCompleted;
  @HiveField(10)
  DateTime? completedAt;
  @HiveField(11)
  final DateTime createdAt;
  @HiveField(12)
  final List<Subtask> subtasks;

  Task({
    required this.id,
    required this.title,
    this.description = "",
    this.dueDate,
    this.location = "",
    this.priority = Priority.medium,
    this.size = Size.medium,
    this.reminders = const [],
    this.category = "",
    this.isCompleted = false,
    this.completedAt,
    required this.createdAt,
    this.subtasks = const [],
  });
}

/// Options for sorting tasks
enum TaskSortOption {
  dueDate,
  priority,
  size,
  alphabetical,
  category;

  String getLocalizedLabel(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    switch (this) {
      case TaskSortOption.dueDate:
        return l10n.taskListSortDueDate;
      case TaskSortOption.priority:
        return l10n.taskListSortPriority;
      case TaskSortOption.size:
        return l10n.taskListSortSize;
      case TaskSortOption.alphabetical:
        return l10n.taskListSortAlphabetical;
      case TaskSortOption.category:
        return l10n.taskListSortCategory;
    }
  }
}

/// Reminder options for tasks
enum ReminderOption {
  onTime,
  minutes30Before,
  hour1Before,
  day1Before,
  custom;

  String getLocalizedLabel(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    switch (this) {
      case ReminderOption.onTime:
        return l10n.taskDetailsReminderOptionOnce;
      case ReminderOption.minutes30Before:
        return l10n.taskDetailsReminderOptionHalfHour;
      case ReminderOption.hour1Before:
        return l10n.taskDetailsReminderOptionHour;
      case ReminderOption.day1Before:
        return l10n.taskDetailsReminderOptionDay;
      case ReminderOption.custom:
        return l10n.taskDetailsReminderOptionCustom;
    }
  }
}

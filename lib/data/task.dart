import 'dart:convert';
import 'package:flutter/material.dart';

enum TaskStatus { todo, done, overdue }

enum TaskPriority {
  urgent('Urgent'),
  high('High'),
  medium('Medium'),
  low('Low');

  final String label;
  const TaskPriority(this.label);

  IconData get icon {
    switch (this) {
      case TaskPriority.urgent:
        return Icons.keyboard_double_arrow_up;
      case TaskPriority.high:
        return Icons.keyboard_arrow_up;
      case TaskPriority.medium:
        return Icons.remove;
      case TaskPriority.low:
        return Icons.keyboard_arrow_down;
    }
  }
}

enum TaskSize {
  tiny('Tiny', 15, 'XS'),
  small('Small', 30, 'S'),
  medium('Medium', 60, 'M'),
  large('Large', 120, 'L'),
  xlarge('X-Large', 240, 'XL');

  final String label;
  final int estimatedMinutes;
  final String badgeText;
  const TaskSize(this.label, this.estimatedMinutes, this.badgeText);

  IconData get icon {
    switch (this) {
      case TaskSize.tiny:
        return Icons.crop;
      case TaskSize.small:
        return Icons.crop_square;
      case TaskSize.medium:
        return Icons.fullscreen;
      case TaskSize.large:
        return Icons.aspect_ratio;
      case TaskSize.xlarge:
        return Icons.fit_screen;
    }
  }
}

class Task {
  final int? id;
  final String title;
  final String? description;
  final String iconName;
  final String? location;
  final int? dueDate; // Unix epoch milliseconds (date only, midnight)
  final int?
  dueTime; // Unix epoch milliseconds (time only, as if same day) - ADDED
  final List<int> reminders; // List of Unix epoch milliseconds
  final int? categoryId;
  final int? durationEst; // Milliseconds
  final TaskStatus status;
  final TaskPriority priority;
  final TaskSize size;
  final bool xpAwarded; // Track if XP was already awarded - ADDED

  Task({
    this.id,
    required this.title,
    this.description,
    this.iconName = 'default',
    this.location,
    this.dueDate,
    this.dueTime,
    this.reminders = const [],
    this.categoryId,
    this.durationEst,
    this.status = TaskStatus.todo,
    this.priority = TaskPriority.medium,
    this.size = TaskSize.medium,
    this.xpAwarded = false,
  });

  Map<String, dynamic> toMap() {
    return {
      if (id != null) 'id': id,
      'title': title,
      'description': description,
      'iconName': iconName,
      'location': location,
      'dueDate': dueDate,
      'dueTime': dueTime,
      'reminders': jsonEncode(reminders),
      'categoryId': categoryId,
      'durationEst': durationEst,
      'status': status.index,
      'priority': priority.index,
      'size': size.index,
      'xpAwarded': xpAwarded ? 1 : 0,
    };
  }

  factory Task.fromMap(Map<String, dynamic> map) {
    return Task(
      id: map['id'],
      title: map['title'],
      description: map['description'],
      iconName: map['iconName'] ?? 'default',
      location: map['location'],
      dueDate: map['dueDate'],
      dueTime: map['dueTime'],
      reminders: map['reminders'] != null
          ? (jsonDecode(map['reminders']) as List).cast<int>()
          : [],
      categoryId: map['categoryId'],
      durationEst: map['durationEst'],
      status: TaskStatus.values[map['status'] ?? 0],
      priority: TaskPriority.values[map['priority'] ?? 2],
      size: TaskSize.values[map['size'] ?? 2],
      xpAwarded: (map['xpAwarded'] ?? 0) == 1,
    );
  }

  DateTime? get dueDateTime =>
      dueDate != null ? DateTime.fromMillisecondsSinceEpoch(dueDate!) : null;
  DateTime? get dueTimeDateTime =>
      dueTime != null ? DateTime.fromMillisecondsSinceEpoch(dueTime!) : null;
  List<DateTime> get reminderDateTimes =>
      reminders.map((r) => DateTime.fromMillisecondsSinceEpoch(r)).toList();

  Task copyWith({
    int? id,
    String? title,
    String? description,
    String? iconName,
    String? location,
    int? dueDate,
    int? dueTime,
    List<int>? reminders,
    int? categoryId,
    int? durationEst,
    TaskStatus? status,
    TaskPriority? priority,
    TaskSize? size,
    bool? xpAwarded,
  }) {
    return Task(
      id: id ?? this.id,
      title: title ?? this.title,
      description: description ?? this.description,
      iconName: iconName ?? this.iconName,
      location: location ?? this.location,
      dueDate: dueDate ?? this.dueDate,
      dueTime: dueTime ?? this.dueTime,
      reminders: reminders ?? this.reminders,
      categoryId: categoryId ?? this.categoryId,
      durationEst: durationEst ?? this.durationEst,
      status: status ?? this.status,
      priority: priority ?? this.priority,
      size: size ?? this.size,
      xpAwarded: xpAwarded ?? this.xpAwarded,
    );
  }
}

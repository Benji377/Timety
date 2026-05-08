import 'package:hive/hive.dart';

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

  Subtask({
    required this.id,
    required this.title,
    this.isCompleted = false,
  });
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

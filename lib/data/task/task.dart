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

  Map<String, dynamic> toJson() => {
        'id': id,
        'title': title,
        'isCompleted': isCompleted,
      };

  factory Subtask.fromJson(Map<String, dynamic> json) => Subtask(
        id: json['id'],
        title: json['title'],
        isCompleted: json['isCompleted'] ?? false,
      );
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

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'description': description,
      // DateTime must be converted to ISO8601 strings for JSON
      'dueDate': dueDate != null ? dueDate!.toIso8601String() : "",
      'location': location,
      'priority': priority.name,
      'reminders': reminders.map((e) => e.toIso8601String()).toList(),
      'category': category,
      'isCompleted': isCompleted,
      'size': size.name,
      'completedAt': completedAt != null ? completedAt!.toIso8601String() : "",
      'createdAt': createdAt.toIso8601String(),
      'subtasks': subtasks.map((s) => s.toJson()).toList(),
    };
  }

  factory Task.fromJson(Map<String, dynamic> json) => Task(
    id: json['id'],
    title: json['title'],
    description: json['description'],
    dueDate: json['dueDate'] != "" ? DateTime.parse(json['dueDate']) : null,
    location: json['location'],
    priority: Priority.values.firstWhere((p) => p.name == json['priority']),
    size: Size.values.firstWhere((s) => s.name == json['size']),
    // Cast to List and map back to DateTime
    reminders: (json['reminders'] as List)
        .map((e) => DateTime.parse(e as String))
        .toList(),
    category: json['category'] ?? "",
    isCompleted: json['isCompleted'] ?? false,
    completedAt: json['completedAt'] != ""
        ? DateTime.parse(json['completedAt'])
        : null,
    createdAt: DateTime.parse(json['createdAt']),
    subtasks: json['subtasks'] != null
        ? (json['subtasks'] as List)
            .map((e) => Subtask.fromJson(e as Map<String, dynamic>))
            .toList()
        : const [],
  );
}

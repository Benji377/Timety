enum Priority { low, medium, high }

class Task {
  final String id;
  final String title;
  final String description;
  final DateTime? dueDate;
  final String location;
  final Priority priority;
  final List<DateTime> reminders;
  final String category; 
  bool isCompleted;

  Task({
    required this.id,
    required this.title,
    this.description = "",
    this.dueDate,
    this.location = "",
    this.priority = Priority.low,
    this.reminders = const [],
    this.category = "",
    this.isCompleted = false,
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
    };
  }

  factory Task.fromJson(Map<String, dynamic> json) => Task(
        id: json['id'],
        title: json['title'],
        description: json['description'],
        dueDate: json['dueDate'] != "" ? DateTime.parse(json['dueDate']) : null,
        location: json['location'],
        priority: Priority.values.firstWhere((p) => p.name == json['priority']),
        // Cast to List and map back to DateTime
        reminders: (json['reminders'] as List)
            .map((e) => DateTime.parse(e as String))
            .toList(),
        category: json['category'] ?? "",
        isCompleted: json['isCompleted'] ?? false,
      );
}
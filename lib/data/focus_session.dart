enum FocusRating { great, okay, distracted }

class FocusSession {
  final int? id;
  final int categoryId;
  final int? taskId;
  final int startTime; // Unix epoch milliseconds
  final int endTime; // Unix epoch milliseconds
  final int duration; // Milliseconds
  final FocusRating? rating;
  final String? note;

  FocusSession({
    this.id,
    required this.categoryId,
    this.taskId,
    required this.startTime,
    required this.endTime,
    required this.duration,
    this.rating,
    this.note,
  });

  Map<String, dynamic> toMap() {
    return {
      if (id != null) 'id': id,
      'categoryId': categoryId,
      'taskId': taskId,
      'startTime': startTime,
      'endTime': endTime,
      'duration': duration,
      'rating': rating?.index,
      'note': note,
    };
  }

  factory FocusSession.fromMap(Map<String, dynamic> map) {
    return FocusSession(
      id: map['id'],
      categoryId: map['categoryId'] ?? 0,
      taskId: map['taskId'],
      startTime: map['startTime'] ?? 0,
      endTime: map['endTime'] ?? 0,
      duration: map['duration'] ?? 0,
      rating: map['rating'] != null ? FocusRating.values[map['rating']] : null,
      note: map['note'],
    );
  }
}

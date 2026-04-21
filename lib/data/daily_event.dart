class DailyEvent {
  final int? id;
  final int timestamp; // Milliseconds
  final String type;
  final String? description;

  DailyEvent({
    this.id,
    required this.timestamp,
    required this.type,
    this.description,
  });

  Map<String, dynamic> toMap() {
    return {
      if (id != null) 'id': id,
      'timestamp': timestamp,
      'type': type,
      'description': description,
    };
  }

  factory DailyEvent.fromMap(Map<String, dynamic> map) {
    return DailyEvent(
      id: map['id'],
      timestamp: map['timestamp'] ?? 0,
      type: map['type'] ?? 'OTHER',
      description: map['description'],
    );
  }
}

import 'package:flutter_test/flutter_test.dart';
import 'package:timety/data/task/task.dart';

void main() {
  group('Task models', () {
    test('build Task with default values and mutable completion state', () {
      final task = Task(
        id: 'task-1',
        title: 'Write tests',
        createdAt: DateTime(2026, 5, 11),
      );

      expect(task.description, isEmpty);
      expect(task.location, isEmpty);
      expect(task.priority, Priority.medium);
      expect(task.size, Size.medium);
      expect(task.reminders, isEmpty);
      expect(task.category, isEmpty);
      expect(task.isCompleted, isFalse);
      expect(task.completedAt, isNull);
      expect(task.subtasks, isEmpty);

      task.isCompleted = true;
      task.completedAt = DateTime(2026, 5, 11, 9, 30);

      expect(task.isCompleted, isTrue);
      expect(task.completedAt, isNotNull);
    });

    test('Subtask defaults to incomplete and supports toggling', () {
      final subtask = Subtask(id: 'sub-1', title: 'Outline cases');

      expect(subtask.isCompleted, isFalse);

      subtask.isCompleted = true;

      expect(subtask.isCompleted, isTrue);
    });
  });
}

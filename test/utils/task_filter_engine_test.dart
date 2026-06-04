import 'package:flutter_test/flutter_test.dart';
import 'package:timety/data/task/task.dart';
import 'package:timety/utils/logic/task_filter_utils.dart';
import 'package:timety/data/task/task_sort_option.dart';

void main() {
  group('TaskFilterEngine', () {
    late List<Task> testTasks;

    setUp(() {
      testTasks = [
        Task(
          id: '1',
          title: 'Buy groceries',
          description: 'Milk, eggs, bread',
          category: 'Shopping',
          priority: Priority.high,
          size: Size.small,
          createdAt: DateTime(2026, 5),
        ),
        Task(
          id: '2',
          title: 'Finish report',
          description: 'Q2 sales report',
          category: 'Work',
          size: Size.large,
          dueDate: DateTime(2026, 5, 20),
          createdAt: DateTime(2026, 5, 2),
        ),
        Task(
          id: '3',
          title: 'Call mom',
          description: 'Weekly check-in',
          category: 'Personal',
          priority: Priority.low,
          size: Size.small,
          isCompleted: true,
          createdAt: DateTime(2026, 5, 3),
        ),
        Task(
          id: '4',
          title: 'Code review',
          description: 'Review PR #123',
          category: 'Work',
          priority: Priority.veryHigh,
          dueDate: DateTime(2026, 5, 16),
          createdAt: DateTime(2026, 5, 4),
        ),
        Task(
          id: '5',
          title: 'Read article',
          description: 'Flutter best practices',
          category: 'Learning',
          priority: Priority.low,
          size: Size.small,
          createdAt: DateTime(2026, 5, 5),
        ),
      ];
    });

    group('process', () {
      test('returns all tasks with default filters', () {
        const engine = TaskFilterEngine();
        final result = engine.process(testTasks);

        expect(result.length, equals(5));
      });

      test('filters by search query in title', () {
        const engine = TaskFilterEngine(searchQuery: 'report');
        final result = engine.process(testTasks);

        expect(result.length, equals(1));
        expect(result.first.title, equals('Finish report'));
      });

      test('filters by search query in description', () {
        const engine = TaskFilterEngine(searchQuery: 'sales');
        final result = engine.process(testTasks);

        expect(result.length, equals(1));
        expect(result.first.id, equals('2'));
      });

      test('search is case-insensitive', () {
        const engine = TaskFilterEngine(searchQuery: 'REVIEW');
        final result = engine.process(testTasks);

        expect(result.length, equals(1));
        expect(result.first.id, equals('4')); // 'Code review' title
      });

      test('returns empty list for non-matching search', () {
        const engine = TaskFilterEngine(searchQuery: 'nonexistent');
        final result = engine.process(testTasks);

        expect(result.isEmpty, isTrue);
      });

      test('filters by category', () {
        const engine = TaskFilterEngine(categoryFilter: 'Work');
        final result = engine.process(testTasks);

        expect(result.length, equals(2));
        expect(result.every((t) => t.category == 'Work'), isTrue);
      });

      test('filters by category and search combined', () {
        const engine = TaskFilterEngine(
          categoryFilter: 'Work',
          searchQuery: 'report',
        );
        final result = engine.process(testTasks);

        expect(result.length, equals(1));
        expect(result.first.title, equals('Finish report'));
      });

      test('ignores empty category filter', () {
        const engine = TaskFilterEngine(categoryFilter: '');
        final result = engine.process(testTasks);

        expect(result.length, equals(5));
      });
    });

    group('sorting', () {
      test('sorts by due date ascending', () {
        const engine = TaskFilterEngine();
        final result = engine.process(testTasks);

        // Tasks without due dates go to bottom
        expect(result[0].id, equals('4')); // 2026-05-16
        expect(result[1].id, equals('2')); // 2026-05-20
      });

      test('tasks without due dates sort to bottom', () {
        const engine = TaskFilterEngine();
        final result = engine.process(testTasks);

        final tasksWithoutDueDate = result.where((t) => t.dueDate == null);
        expect(
          result.sublist(result.length - tasksWithoutDueDate.length),
          equals(tasksWithoutDueDate.toList()),
        );
      });

      test('sorts by priority ascending', () {
        const engine = TaskFilterEngine(sortOption: TaskSortOption.priority);
        final result = engine.process(testTasks);

        // Priority.low (index 0) should come first
        expect(result.first.priority, equals(Priority.low));
        // Priority.veryHigh (index 3) should come last
        expect(result.last.priority, equals(Priority.veryHigh));
      });

      test('sorts by size ascending', () {
        const engine = TaskFilterEngine(sortOption: TaskSortOption.size);
        final result = engine.process(testTasks);

        expect(result.first.size, equals(Size.small));
      });

      test('sorts alphabetically', () {
        const engine = TaskFilterEngine(
          sortOption: TaskSortOption.alphabetical,
        );
        final result = engine.process(testTasks);

        expect(result.first.title, equals('Buy groceries'));
        expect(result.last.title, equals('Read article'));
      });

      test('sorts by category', () {
        const engine = TaskFilterEngine(sortOption: TaskSortOption.category);
        final result = engine.process(testTasks);

        expect(result[0].category, equals('Learning'));
        expect(result[result.length - 1].category, equals('Work'));
      });

      test('reverses sort order with isAscending=false', () {
        final ascending = const TaskFilterEngine(
          sortOption: TaskSortOption.alphabetical,
        ).process(testTasks);

        final descending = const TaskFilterEngine(
          sortOption: TaskSortOption.alphabetical,
          isAscending: false,
        ).process(testTasks);

        expect(ascending.first.title, equals(descending.last.title));
        expect(ascending.last.title, equals(descending.first.title));
      });
    });

    group('copyWith', () {
      test('creates a new instance with updated values', () {
        const engine1 = TaskFilterEngine(
          searchQuery: 'test',
          categoryFilter: 'Work',
          sortOption: TaskSortOption.priority,
        );

        final engine2 = engine1.copyWith(searchQuery: 'updated');

        expect(engine1.searchQuery, equals('test'));
        expect(engine2.searchQuery, equals('updated'));
        expect(engine2.categoryFilter, equals('Work'));
        expect(engine2.sortOption, equals(TaskSortOption.priority));
      });

      test('preserves values not specified in copyWith', () {
        const engine1 = TaskFilterEngine(isAscending: false);

        final engine2 = engine1.copyWith(sortOption: TaskSortOption.priority);

        expect(engine2.isAscending, equals(false));
      });
    });

    group('complex scenarios', () {
      test('filters and sorts together', () {
        const engine = TaskFilterEngine(
          categoryFilter: 'Work',
          sortOption: TaskSortOption.priority,
          isAscending: false,
        );
        final result = engine.process(testTasks);

        expect(result.length, equals(2));
        expect(result.first.priority, equals(Priority.veryHigh));
        expect(result.last.priority, equals(Priority.medium));
      });

      test('searches, filters, and sorts together', () {
        const engine = TaskFilterEngine(
          searchQuery: 'code',
          categoryFilter: 'Work',
          sortOption: TaskSortOption.alphabetical,
        );
        final result = engine.process(testTasks);

        expect(result.length, equals(1));
        expect(result.first.title, equals('Code review'));
      });

      test('handles empty task list', () {
        const engine = TaskFilterEngine(categoryFilter: 'NonExistent');
        final result = engine.process(testTasks);

        expect(result.isEmpty, isTrue);
      });
    });
  });
}

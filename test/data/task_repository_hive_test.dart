import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:hive/hive.dart';
import 'package:timety/data/task/task.dart';
import 'package:timety/data/task/task_repository_hive.dart';

import '../test_support/hive_test_utils.dart';

void main() {
  late Directory hiveDir;

  setUpAll(() async {
    hiveDir = await initializeHiveTestDir();
  });

  tearDownAll(() async {
    await disposeHiveTestDir(hiveDir);
  });

  test('fetches and saves tasks while removing stale entries', () async {
    final repository = HiveTaskRepository();
    final box = await Hive.openBox<Task>(HiveTaskRepository.boxName);

    await box.put(
      'old-task',
      Task(
        id: 'old-task',
        title: 'Stale task',
        createdAt: DateTime(2026),
      ),
    );

    await repository.saveTasks([
      Task(id: 'task-1', title: 'First task', createdAt: DateTime(2026, 1, 2)),
      Task(id: 'task-2', title: 'Second task', createdAt: DateTime(2026, 1, 3)),
    ]);

    final savedTasks = await repository.fetchTasks();

    expect(savedTasks, hasLength(2));
    expect(
      savedTasks.map((task) => task.id),
      containsAll(['task-1', 'task-2']),
    );
    expect(box.containsKey('old-task'), isFalse);

    await repository.saveTasks([
      Task(
        id: 'task-2',
        title: 'Updated second task',
        createdAt: DateTime(2026, 1, 3),
      ),
    ]);

    final updatedTasks = await repository.fetchTasks();

    expect(updatedTasks, hasLength(1));
    expect(updatedTasks.single.id, 'task-2');
    expect(updatedTasks.single.title, 'Updated second task');
  });
}

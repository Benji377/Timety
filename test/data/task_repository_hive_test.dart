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

  test('saves, updates, and deletes individual tasks', () async {
    final repository = HiveTaskRepository();
    final box = await Hive.openBox<Task>(HiveTaskRepository.boxName);
    await box.clear(); // Start clean

    // 1. Save individual tasks
    await repository.saveTask(
      Task(id: 'task-1', title: 'First task', createdAt: DateTime(2026, 1, 2)),
    );
    await repository.saveTask(
      Task(id: 'task-2', title: 'Second task', createdAt: DateTime(2026, 1, 3)),
    );

    var savedTasks = await repository.fetchTasks();
    expect(savedTasks, hasLength(2));
    expect(
      savedTasks.map((task) => task.id),
      containsAll(['task-1', 'task-2']),
    );

    // 2. Update an existing task
    await repository.saveTask(
      Task(
        id: 'task-2',
        title: 'Updated second task',
        createdAt: DateTime(2026, 1, 3),
      ),
    );

    savedTasks = await repository.fetchTasks();
    expect(savedTasks, hasLength(2)); // Still 2 tasks! No accidental deletions.

    final updatedTask = savedTasks.firstWhere((t) => t.id == 'task-2');
    expect(updatedTask.title, 'Updated second task');

    // 3. Delete a task explicitly
    await repository.deleteTask('task-1');

    savedTasks = await repository.fetchTasks();
    expect(savedTasks, hasLength(1));
    expect(savedTasks.single.id, 'task-2');
  });
}

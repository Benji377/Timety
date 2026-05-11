import 'package:flutter_test/flutter_test.dart';

import 'package:timety/providers/task_provider.dart';
import 'package:timety/providers/user_provider.dart';
import 'package:timety/utils/xp_calculator.dart';

import '../test_support/fakes.dart';
import '../test_support/notifications_mock.dart';

void main() {
  setUpAll(() {
    installLocalNotificationsMock();
  });

  tearDownAll(() {
    clearLocalNotificationsMock();
  });

  test(
    'loads and mutates task state while preserving repository writes',
    () async {
      final now = DateTime.now();
      final upcoming = now.add(const Duration(days: 1));
      final repository = FakeTaskRepository(
        initialTasks: [
          buildTask(
            id: 'task-1',
            title: 'First task',
            description: 'Keep moving',
            dueDate: upcoming,
            category: 'Work',
          ),
        ],
      );
      final provider = TaskProvider(repository: repository);
      final userRepository = FakeUserRepository();
      final userProvider = UserProvider(repository: userRepository);

      await drainEventQueue();
      await provider.loadTasks();

      expect(provider.tasks, hasLength(1));

      await provider.addTask(
        buildTask(id: 'task-2', title: 'Second task', dueDate: upcoming),
      );
      await drainEventQueue();
      expect(provider.tasks, hasLength(2));
      expect(repository.saveCalls, greaterThanOrEqualTo(1));

      await provider.toggleTask('task-1', userProvider: userProvider);
      await drainEventQueue();
      expect(
        provider.tasks.firstWhere((task) => task.id == 'task-1').isCompleted,
        isTrue,
      );
      expect(userProvider.totalXp, ExperienceEngine.xpPerTask);

      final updatedTask = buildTask(
        id: 'task-1',
        title: 'Updated task',
        dueDate: upcoming,
        isCompleted: true,
      );
      await provider.updateTask(updatedTask);
      await drainEventQueue();
      expect(
        provider.tasks.firstWhere((task) => task.id == 'task-1').title,
        'Updated task',
      );

      await provider.removeTask('task-2');
      await drainEventQueue();
      expect(provider.tasks, hasLength(1));
      expect(provider.tasks.single.id, 'task-1');

      await provider.loadTasks();
      expect(repository.fetchCalls, greaterThanOrEqualTo(1));
    },
  );
}

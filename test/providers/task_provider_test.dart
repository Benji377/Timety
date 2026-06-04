import 'package:flutter_test/flutter_test.dart';
import 'package:flutter/services.dart';

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

  test('phrases task reminders relative to the due date', () async {
    installLocalNotificationsMock();

    final scheduledNotifications = <Map<String, Object?>>[];
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(localNotificationsChannel, (
          MethodCall methodCall,
        ) async {
          switch (methodCall.method) {
            case 'initialize':
              return true;
            case 'zonedSchedule':
              scheduledNotifications.add(
                Map<String, Object?>.from(
                  methodCall.arguments as Map<dynamic, dynamic>,
                ),
              );
              return null;
            case 'pendingNotificationRequests':
            case 'getActiveNotifications':
              return <Map<String, Object?>>[];
            case 'getNotificationAppLaunchDetails':
              return null;
            default:
              return null;
          }
        });

    addTearDown(installLocalNotificationsMock);

    final now = DateTime.now();
    final dueDate = now.add(const Duration(days: 1, hours: 1));
    final reminderTime = dueDate.subtract(const Duration(hours: 1));

    final provider = TaskProvider(repository: FakeTaskRepository());

    await provider.addTask(
      buildTask(
        id: 'task-reminder-1',
        title: 'Write docs',
        dueDate: dueDate,
        reminders: [reminderTime],
      ),
    );
    await drainEventQueue();

    final taskReminder = scheduledNotifications.firstWhere(
      (notification) => notification['title'] == 'Reminder: Write docs',
    );

    expect(taskReminder['body'], contains('In 1 hour'));
    expect(taskReminder['body'], isNot(contains('Tomorrow')));
  });
}

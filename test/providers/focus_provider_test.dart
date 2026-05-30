import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:timety/data/focus/focus_models.dart';
import 'package:timety/providers/focus_provider.dart';
import 'package:timety/providers/habit_provider.dart';
import 'package:timety/providers/settings_provider.dart';
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

  test('initializes default modes and tag when repository is empty', () async {
    final repository = FakeFocusRepository();
    final provider = FocusProvider(repository: repository);

    await drainEventQueue();

    expect(provider.tags, isNotEmpty);
    expect(provider.selectedTag, isNotNull);
    expect(provider.modes, isNotEmpty);
    expect(provider.activeMode?.id, 'system_stopwatch');
    expect(repository.savedTagIds, contains('default_tag'));
    expect(
      repository.savedModeIds,
      containsAll(<String>[
        'system_stopwatch',
        'system_flexible',
        'system_pomodoro',
      ]),
    );
  });

  test(
    'auto-completes linked task and habit on completed session when enabled',
    () async {
      SharedPreferences.setMockInitialValues({});

      final focusRepository = FakeFocusRepository(
        initialModes: [
          FocusMode.stopwatch(),
          FocusMode.flexible(),
          FocusMode.classicPomodoro(),
        ],
        initialTags: [
          FocusTag(
            id: 'tag-1',
            name: 'Deep Work',
            colorValue: Colors.blue.toARGB32(),
          ),
        ],
      );
      final taskRepository = FakeTaskRepository(
        initialTasks: [buildTask(id: 'task-1', title: 'Write docs')],
      );
      final habitRepository = FakeHabitRepository(
        initialHabits: [buildDailyHabit(id: 'habit-1', name: 'Drink water')],
      );

      final focusProvider = FocusProvider(repository: focusRepository);
      final taskProvider = TaskProvider(repository: taskRepository);
      final habitProvider = HabitProvider(repository: habitRepository);
      final settingsProvider = SettingsProvider();
      final userProvider = UserProvider(repository: FakeUserRepository());

      await taskProvider.loadTasks();
      await habitProvider.loadHabits();
      await drainEventQueue();

      await drainEventQueue();

      focusProvider.attachTaskProvider(taskProvider);
      focusProvider.attachHabitProvider(habitProvider);
      focusProvider.attachSettingsProvider(settingsProvider);
      focusProvider.attachUserProvider(userProvider);

      await drainEventQueue();

      settingsProvider.setAutoCompleteFocusTargetOnFinish(true);

      await drainEventQueue();

      focusProvider.setSelectedTask(id: 'task-1', label: 'Write docs');
      focusProvider.startSession();
      focusProvider.stopSession(completed: true, userProvider: userProvider);

      await drainEventQueue();

      expect(taskProvider.tasks.first.isCompleted, isTrue);
      expect(focusProvider.selectedTarget?.type, FocusTargetType.tag);
      expect(focusProvider.selectedTag?.id, 'tag-1');

      focusProvider.setSelectedHabit(id: 'habit-1', label: 'Drink water');
      focusProvider.startSession();
      focusProvider.stopSession(completed: true, userProvider: userProvider);

      await drainEventQueue();

      expect(
        habitProvider.isCompletedOn(habitProvider.habits.first, DateTime.now()),
        isTrue,
      );
      expect(focusProvider.selectedTarget?.type, FocusTargetType.tag);
      expect(focusProvider.selectedTag?.id, 'tag-1');
    },
  );

  test(
    'does not start a session for a habit locked behind an incomplete stack item',
    () async {
      final focusRepository = FakeFocusRepository(
        initialModes: [
          FocusMode.stopwatch(),
          FocusMode.flexible(),
          FocusMode.classicPomodoro(),
        ],
        initialTags: [
          FocusTag(
            id: 'tag-1',
            name: 'Deep Work',
            colorValue: Colors.blue.toARGB32(),
          ),
        ],
      );
      final habitRepository = FakeHabitRepository(
        initialHabits: [
          buildDailyHabit(
            id: 'habit-1',
            name: 'First habit',
            stackName: 'Morning',
            stackOrder: 0,
          ),
          buildDailyHabit(
            id: 'habit-2',
            name: 'Second habit',
            stackName: 'Morning',
            stackOrder: 1,
          ),
        ],
      );

      final focusProvider = FocusProvider(repository: focusRepository);
      final habitProvider = HabitProvider(repository: habitRepository);

      await habitProvider.loadHabits();
      await drainEventQueue();

      focusProvider.attachHabitProvider(habitProvider);
      await drainEventQueue();

      focusProvider.setSelectedHabit(id: 'habit-2', label: 'Second habit');
      focusProvider.startSession();

      expect(focusProvider.isRunning, isFalse);
      expect(focusProvider.selectedTarget?.id, 'habit-2');
    },
  );

  test(
    'supports mode, tag, and history operations without running a timer',
    () async {
      final existingMode = FocusMode(
        id: 'custom_mode',
        name: 'Custom Mode',
        type: FocusModeType.custom,
        phases: [SessionPhase(type: PhaseType.focus, durationMinutes: 15)],
      );
      final repository = FakeFocusRepository(
        initialModes: [
          existingMode,
          FocusMode.stopwatch(),
          FocusMode.flexible(),
          FocusMode.classicPomodoro(),
        ],
        initialSessions: [
          FocusSession(
            id: 'session-1',
            modeId: 'system_stopwatch',
            startTime: DateTime.now().subtract(const Duration(days: 1)),
            totalSecondsFocused: 120,
            isCompleted: true,
          ),
        ],
        initialTags: [
          FocusTag(
            id: 'tag-1',
            name: 'Deep Work',
            colorValue: Colors.blue.toARGB32(),
          ),
        ],
      );
      final provider = FocusProvider(repository: repository);
      final userRepository = FakeUserRepository();
      final userProvider = UserProvider(repository: userRepository);

      await drainEventQueue();

      provider.setActiveMode(existingMode);
      expect(provider.activeMode?.id, 'custom_mode');

      await provider.saveCustomMode(
        FocusMode(
          id: 'custom_mode_2',
          name: 'Custom Two',
          type: FocusModeType.custom,
          phases: [SessionPhase(type: PhaseType.focus, durationMinutes: 20)],
        ),
      );
      await drainEventQueue();
      expect(provider.modes.any((mode) => mode.id == 'custom_mode_2'), isTrue);

      provider.setActiveMode(FocusMode.flexible());
      provider.setFlexibleDuration(45);
      expect(provider.secondsRemainingInPhase, 45 * 60);

      final now = DateTime.now();
      await provider.logPastSession(
        mode: existingMode,
        startTime: now.subtract(const Duration(minutes: 5)),
        endTime: now,
        tag: provider.selectedTag,
        userProvider: userProvider,
      );
      await drainEventQueue();
      expect(provider.history, hasLength(2));
      expect(userProvider.totalXp, ExperienceEngine.xpPerFocusMin * 5);
      expect(provider.getMinutesFocusedToday(), 5);

      await provider.createTag('Reading', Colors.red);
      await drainEventQueue();
      expect(provider.tags.any((tag) => tag.name == 'Reading'), isTrue);

      final createdTag = provider.tags.firstWhere(
        (tag) => tag.name == 'Reading',
      );
      provider.setSelectedTag(createdTag);
      expect(provider.selectedTag?.name, 'Reading');

      await provider.deleteTag(createdTag.id);
      await drainEventQueue();
      expect(provider.tags.any((tag) => tag.id == createdTag.id), isFalse);

      await provider.deleteMode('custom_mode_2');
      await drainEventQueue();
      expect(provider.modes.any((mode) => mode.id == 'custom_mode_2'), isFalse);

      await provider.deleteMode('system_stopwatch');
      await pumpEventQueue();
      expect(
        provider.modes.any((mode) => mode.id == 'system_stopwatch'),
        isTrue,
      );
    },
  );
}

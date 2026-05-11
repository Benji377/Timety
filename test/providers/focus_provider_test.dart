import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:timety/data/focus/focus_models.dart';
import 'package:timety/providers/focus_provider.dart';
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

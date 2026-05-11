import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hive/hive.dart';
import 'package:timety/data/focus/focus_models.dart';
import 'package:timety/data/focus/focus_repository_hive.dart';

import '../test_support/hive_test_utils.dart';

void main() {
  late Directory hiveDir;

  setUpAll(() async {
    hiveDir = await initializeHiveTestDir();
  });

  tearDownAll(() async {
    await disposeHiveTestDir(hiveDir);
  });

  test('saves, fetches, and deletes focus modes', () async {
    final repository = HiveFocusRepository();
    final mode = FocusMode(
      id: 'custom_mode',
      name: 'Custom',
      type: FocusModeType.custom,
      phases: [SessionPhase(type: PhaseType.focus, durationMinutes: 15)],
    );

    await repository.saveMode(mode);

    final modes = await repository.fetchModes();
    expect(modes, hasLength(1));
    expect(modes.single.id, 'custom_mode');

    await repository.deleteMode('custom_mode');
    expect(await repository.fetchModes(), isEmpty);
  });

  test('saves, fetches, and deletes focus sessions and tags', () async {
    final repository = HiveFocusRepository();
    final session = FocusSession(
      id: 'session-1',
      modeId: 'system_stopwatch',
      startTime: DateTime(2026, 5, 11, 9),
      totalSecondsFocused: 300,
      distractions: [Distraction(time: DateTime(2026, 5, 11, 9, 2))],
      isCompleted: true,
      tagId: 'tag-1',
    );
    final tag = FocusTag(
      id: 'tag-1',
      name: 'Deep Work',
      colorValue: Colors.red.toARGB32(),
    );

    await repository.saveSession(session);
    await repository.saveTag(tag);

    final sessions = await repository.fetchSessions();
    final tags = await repository.fetchTags();

    expect(sessions, hasLength(1));
    expect(sessions.single.id, 'session-1');
    expect(tags, hasLength(1));
    expect(tags.single.name, 'Deep Work');

    await repository.deleteTag('tag-1');
    expect(await repository.fetchTags(), isEmpty);

    final sessionBox = await Hive.openBox<FocusSession>(
      HiveFocusRepository.sessionBoxName,
    );
    expect(sessionBox.length, 1);
  });
}

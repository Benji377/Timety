import 'package:flutter_test/flutter_test.dart';
import 'package:timety/data/focus/focus_models.dart';

void main() {
  group('Focus models', () {
    test('creates the built-in focus modes with expected phases', () {
      final stopwatch = FocusMode.stopwatch();
      final flexible = FocusMode.flexible();
      final pomodoro = FocusMode.classicPomodoro();

      expect(stopwatch.id, 'system_stopwatch');
      expect(stopwatch.isSystem, isTrue);
      expect(stopwatch.type, FocusModeType.stopwatch);
      expect(stopwatch.phases, hasLength(1));
      expect(stopwatch.phases.single.type, PhaseType.focus);
      expect(stopwatch.phases.single.durationMinutes, 0);

      expect(flexible.id, 'system_flexible');
      expect(flexible.isSystem, isTrue);
      expect(flexible.type, FocusModeType.flexible);
      expect(flexible.phases.single.durationMinutes, -1);

      expect(pomodoro.id, 'system_pomodoro');
      expect(pomodoro.isSystem, isTrue);
      expect(pomodoro.type, FocusModeType.pomodoro);
      expect(pomodoro.phases, hasLength(8));
      expect(pomodoro.phases.first.durationMinutes, 25);
      expect(pomodoro.phases.last.durationMinutes, 15);

      expect(FocusTargetType.values, contains(FocusTargetType.task));
    });

    test('stores focus session, tag, target, and distraction data', () {
      final distraction = Distraction(
        time: DateTime(2026, 5, 11, 10, 15),
        note: 'Phone check',
      );
      final tag = FocusTag(
        id: 'tag-1',
        name: 'Deep Work',
        colorValue: 0xff112233,
      );
      final session = FocusSession(
        id: 'session-1',
        modeId: 'system_stopwatch',
        startTime: DateTime(2026, 5, 11, 9),
        endTime: DateTime(2026, 5, 11, 9, 25),
        totalSecondsFocused: 1500,
        distractions: [distraction],
        isCompleted: true,
        tagId: tag.id,
        targetType: FocusTargetType.task,
        targetId: 'task-1',
        targetLabel: 'Write docs',
      );

      expect(distraction.note, 'Phone check');
      expect(tag.name, 'Deep Work');
      expect(session.modeId, 'system_stopwatch');
      expect(session.totalSecondsFocused, 1500);
      expect(session.distractions, hasLength(1));
      expect(session.isCompleted, isTrue);
      expect(session.tagId, tag.id);
      expect(session.targetType, FocusTargetType.task);
      expect(session.targetId, 'task-1');
      expect(session.displayTargetLabel, 'Write docs');
    });
  });
}

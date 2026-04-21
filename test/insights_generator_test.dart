import 'package:flutter_test/flutter_test.dart';
import 'package:timety/data/focus_session.dart';
import 'package:timety/utils/insights_generator.dart';

FocusSession _sessionAtHour({required int hour, required int durationMinutes}) {
  final start = DateTime(2026, 1, 1, hour).millisecondsSinceEpoch;
  final durationMs = durationMinutes * 60000;
  return FocusSession(
    categoryId: 1,
    startTime: start,
    endTime: start + durationMs,
    duration: durationMs,
  );
}

void main() {
  group('InsightsGenerator.generateInsights', () {
    test('returns onboarding message when there are no sessions', () {
      final insights = InsightsGenerator.generateInsights([]);

      expect(insights, hasLength(1));
      expect(insights.first, contains('Start focusing'));
    });

    test('summarizes total time, best time, and average session', () {
      final sessions = [_sessionAtHour(hour: 13, durationMinutes: 30)];

      final insights = InsightsGenerator.generateInsights(sessions);

      expect(insights, hasLength(3));
      expect(insights[0], contains('30 minutes focusing'));
      expect(insights[1], contains('Afternoon'));
      expect(
        insights[2],
        contains('average focus session is medium (~30 min)'),
      );
    });

    test('caps to 3 insights and includes streak milestone', () {
      final sessions = [
        _sessionAtHour(hour: 18, durationMinutes: 20),
        _sessionAtHour(hour: 19, durationMinutes: 20),
        _sessionAtHour(hour: 20, durationMinutes: 20),
        _sessionAtHour(hour: 9, durationMinutes: 10),
        _sessionAtHour(hour: 10, durationMinutes: 10),
      ];

      final insights = InsightsGenerator.generateInsights(sessions);

      expect(insights, hasLength(3));
      expect(insights[0], contains('80 minutes focusing'));
      expect(insights[1], contains('Evening'));
      expect(insights[2], contains('focus spree'));
    });
  });
}

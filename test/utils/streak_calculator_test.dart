import 'package:flutter_test/flutter_test.dart';
import 'package:timety/utils/streak_calculator.dart';

void main() {
  group('StreakCalculator', () {
    test('calculateBestStreak collapses duplicates and finds longest run', () {
      final completions = <DateTime>[
        DateTime(2026, 5, 1, 9),
        DateTime(2026, 5, 1, 18),
        DateTime(2026, 5, 2, 10),
        DateTime(2026, 5, 3, 11),
        DateTime(2026, 5, 6, 14),
        DateTime(2026, 5, 7, 12),
      ];

      expect(StreakCalculator.calculateBestStreak(completions), 3);
    });

    test('calculateCurrentStreak counts from today or yesterday', () {
      final today = DateTime.now();
      final completions = <DateTime>[
        today,
        today.subtract(const Duration(days: 1)),
        today.subtract(const Duration(days: 2)),
      ];

      expect(StreakCalculator.calculateCurrentStreak(completions), 3);
    });

    test(
      'calculateCurrentStreak falls back to yesterday when today is missing',
      () {
        final today = DateTime.now();
        final completions = <DateTime>[
          today.subtract(const Duration(days: 1)),
          today.subtract(const Duration(days: 2)),
        ];

        expect(StreakCalculator.calculateCurrentStreak(completions), 2);
      },
    );

    test('calculateBoth returns current and highest streaks', () {
      final today = DateTime.now();
      final completions = <DateTime>[
        today,
        today.subtract(const Duration(days: 1)),
        DateTime(2026),
      ];

      final result = StreakCalculator.calculateBoth(completions);

      expect(result.current, 2);
      expect(result.highest, 2);
    });
  });
}
